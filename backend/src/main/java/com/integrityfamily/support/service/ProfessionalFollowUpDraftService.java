package com.integrityfamily.support.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.domain.AuditEventType;
import com.integrityfamily.auth.service.AuditService;
import com.integrityfamily.support.domain.DraftGeneratorType;
import com.integrityfamily.support.domain.DraftStatus;
import com.integrityfamily.support.domain.ProfessionalFollowUpDraft;
import com.integrityfamily.support.dto.SupportNetworkDtos.FamilyDataView;
import com.integrityfamily.support.dto.SupportNetworkDtos.FollowUpDraftResponse;
import com.integrityfamily.support.repository.ProfessionalFollowUpDraftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Genera el borrador de nota de seguimiento profesional (ADR-006).
 *
 * Reemplaza generateAiSummaryText() del frontend (plantilla en Angular,
 * sin version ni auditoria) por una plantilla determinista versionada en
 * backend. No invoca ningun modelo de IA -- ver ADR-006, Fase 5 (no
 * decidida) para cuando eso cambie.
 *
 * Reutiliza SupportNetworkService.getDataView() para la autorizacion y
 * los datos: ese metodo ya valida que la asignacion pertenece a la
 * familia, esta activa y el email coincide con el profesional -- no se
 * duplica esa logica aqui.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfessionalFollowUpDraftService {

    public static final String TEMPLATE_VERSION = "professional-follow-up-v1.0";

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", new Locale("es", "CO"));

    private static final Map<String, String> SPECIALTY_LABELS = Map.of(
            "THERAPIST", "Terapeuta familiar",
            "ORIENTADOR", "Orientador familiar",
            "SOCIAL_WORKER", "Trabajador social",
            "DOCTOR", "Médico",
            "TEACHER", "Docente",
            "COMMUNITY_LEADER", "Líder comunitario",
            "COACH", "Coach familiar",
            "INSTITUTION", "Institución"
    );

    private final SupportNetworkService supportNetworkService;
    private final ProfessionalFollowUpDraftRepository draftRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Transactional
    public FollowUpDraftResponse generate(Long familyId, Long assignmentId, String professionalEmail) {
        FamilyDataView view = supportNetworkService.getDataView(familyId, assignmentId, professionalEmail);

        voidPreviousDrafts(assignmentId);

        String narrativeText = buildNarrative(view);
        String sourceSnapshot = toSnapshotJson(view);

        ProfessionalFollowUpDraft draft = ProfessionalFollowUpDraft.builder()
                .familyId(familyId)
                .assignmentId(assignmentId)
                .generatedByUserEmail(professionalEmail)
                .generatorType(DraftGeneratorType.RULE_BASED_TEMPLATE)
                .templateVersion(TEMPLATE_VERSION)
                .sourceSnapshot(sourceSnapshot)
                .narrativeText(narrativeText)
                .status(DraftStatus.GENERATED)
                .build();

        ProfessionalFollowUpDraft saved = draftRepository.save(draft);

        auditService.registerSystemEvent(professionalEmail, AuditEventType.PROFESSIONAL_DRAFT_GENERATED,
                "{\"draftId\":" + saved.getId() + ",\"familyId\":" + familyId + ",\"assignmentId\":" + assignmentId + "}");

        return FollowUpDraftResponse.builder()
                .draftId(saved.getId())
                .familyId(familyId)
                .assignmentId(assignmentId)
                .generatedAt(saved.getGeneratedAt())
                .generatorType(saved.getGeneratorType().name())
                .templateVersion(saved.getTemplateVersion())
                .narrativeText(narrativeText)
                .warnings(List.of("REQUIRES_PROFESSIONAL_REVIEW", "NOT_A_CLINICAL_DIAGNOSIS"))
                .build();
    }

    private void voidPreviousDrafts(Long assignmentId) {
        List<ProfessionalFollowUpDraft> active =
                draftRepository.findByAssignmentIdAndStatus(assignmentId, DraftStatus.GENERATED);
        active.forEach(d -> d.setStatus(DraftStatus.VOIDED));
        draftRepository.saveAll(active);
    }

    private String buildNarrative(FamilyDataView v) {
        String todayStr = LocalDateTime.now().format(DATE_FORMATTER);
        String specialtyLabel = v.getSpecialty() != null
                ? SPECIALTY_LABELS.getOrDefault(v.getSpecialty().name(), v.getSpecialty().name())
                : "Médico Cirujano";

        String icfText = "No registrado";
        if (v.getIcfScore() != null) {
            String dirText = switch (v.getIcfDirection() == null ? "" : v.getIcfDirection()) {
                case "IMPROVING" -> "en mejoría progresiva (↑)";
                case "DECLINING" -> "en retroceso (↓)";
                case "CRITICAL_DECLINE" -> "en retroceso crítico (↓↓)";
                default -> "estable (→)";
            };
            // Locale.US deliberado: toFixed(1) en JS siempre usa punto decimal,
            // independientemente del locale -- se replica ese comportamiento aquí
            // para que el borrador no diverja del texto que mostraba el frontend.
            icfText = String.format(Locale.US, "%.1f (%s), con tendencia %s",
                    v.getIcfScore(), v.getIcfLabel(), dirText);
        }

        String riskText = v.getRiskLevel() != null
                ? v.getRiskLevel() + (Boolean.TRUE.equals(v.getSentinelActive()) ? " (ALERTA: Centinela Activo)" : "")
                : "No determinado";
        String sprintText = Boolean.TRUE.equals(v.getHasActiveSprint())
                ? "Activo (Estado: " + v.getActiveSprintStatus() + ")"
                : "Sin sprint activo";
        String planText = Boolean.TRUE.equals(v.getPlanSummaryAvailable())
                ? "Autorizado y en ejecución" : "No autorizado / No disponible";
        String crisisText = Boolean.TRUE.equals(v.getCrisisHistoryAvailable())
                ? "Habilitado para seguimiento longitudinal" : "No disponible";

        StringBuilder recomendacionesPlan = new StringBuilder();
        if (Boolean.TRUE.equals(v.getSentinelActive()) || "ALTO".equals(v.getRiskLevel())) {
            recomendacionesPlan.append("• Activar de inmediato el protocolo de mitigación de crisis y revisar disparadores históricos.\n");
        } else if ("MODERADO".equals(v.getRiskLevel())) {
            recomendacionesPlan.append("• Monitorear indicadores de salud familiar y ajustar dosificación de tareas en el Plan de Mejora.\n");
        }
        if ("DECLINING".equals(v.getIcfDirection()) || "CRITICAL_DECLINE".equals(v.getIcfDirection())) {
            recomendacionesPlan.append("• Recomendar la realización de un Consejo Familiar orientado a fortalecer la comunicación asertiva y reevaluar la cohesión relacional.\n");
        } else {
            recomendacionesPlan.append("• Reforzar el cumplimiento de rituales diarios para consolidar la estabilidad relacional lograda.\n");
        }

        return """
                Nota de Seguimiento – Ecosistema Integrity Family

                Fecha: %s
                Profesional: %s – Ecosistema Integrity Family

                1. DATOS OBJETIVOS DEL SISTEMA
                • ICaF: %s
                • Nivel de Riesgo Global: %s
                • Adherencia / Sprint Familiar: %s
                • Plan de Mejora Familiar: %s
                • Historial de Crisis: %s

                2. OBSERVACIÓN CLÍNICA
                • [Espacio para que el profesional documente los hallazgos directamente constatados durante el teleacompañamiento o sesión presencial].

                3. INTERPRETACIÓN PROFESIONAL
                • Los indicadores obtenidos sugieren una cohesión familiar que requiere seguimiento estructurado. Estos resultados provienen de un instrumento de tamizaje y no constituyen por sí mismos un diagnóstico clínico. Con la información aportada por la plataforma, no existen elementos suficientes para concluir la existencia de violencia intrafamiliar, trastornos específicos o diagnósticos definitivos.

                4. PLAN DE INTERVENCIÓN
                • Realizar observación clínica mediante el Modo Observador para verificar el desarrollo de las misiones del Sprint Familiar.
                • Analizar el cumplimiento de las metas semanales e identificar barreras.
                %s• Ajustar el Plan de Mejora de acuerdo con la evidencia obtenida en el seguimiento.
                • Mantener vigilancia sobre eventos críticos en el Historial de Crisis y activar rutas de protección de ser necesario.

                5. PRÓXIMA REEVALUACIÓN
                • Se sugiere nueva reevaluación tras completar el Sprint Familiar activo o ante la aparición de alertas en el protocolo Sentinel."""
                .formatted(todayStr, specialtyLabel, icfText, riskText, sprintText, planText, crisisText, recomendacionesPlan);
    }

    private String toSnapshotJson(FamilyDataView v) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("icfScore", v.getIcfScore());
        snapshot.put("icfLabel", v.getIcfLabel());
        snapshot.put("icfDirection", v.getIcfDirection());
        snapshot.put("riskLevel", v.getRiskLevel());
        snapshot.put("sentinelActive", v.getSentinelActive());
        snapshot.put("hasActiveSprint", v.getHasActiveSprint());
        snapshot.put("activeSprintStatus", v.getActiveSprintStatus());
        snapshot.put("planSummaryAvailable", v.getPlanSummaryAvailable());
        snapshot.put("crisisHistoryAvailable", v.getCrisisHistoryAvailable());
        snapshot.put("specialty", v.getSpecialty() != null ? v.getSpecialty().name() : null);
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            log.warn("No se pudo serializar sourceSnapshot para assignment {}: {}", v.getAssignmentId(), e.getMessage());
            return "{}";
        }
    }
}
