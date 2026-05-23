package com.integrityfamily.analytics.service;

import com.integrityfamily.ai.service.AiService;
import com.integrityfamily.analytics.dto.DashboardSummaryResponse;
import com.integrityfamily.analytics.dto.SuggestedActionDto;
import com.integrityfamily.common.event.SystemEvent;
import com.integrityfamily.domain.ChecklistItem;
import com.integrityfamily.domain.repository.ChecklistItemRepository;
import com.integrityfamily.common.config.RabbitConfig;
import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.RiskLevel;
import com.integrityfamily.domain.RiskSnapshot;
import com.integrityfamily.domain.LogbookStatus;
import com.integrityfamily.domain.FamilyLogbookEntry;
import com.integrityfamily.domain.repository.FamilyLogbookRepository;
import com.integrityfamily.domain.repository.PlanTaskRepository;
import com.integrityfamily.domain.repository.RiskSnapshotRepository;
import com.integrityfamily.domain.repository.ImprovementPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SDD: Implementacion del Servicio de Analitica Proyectiva.
 * Integra IA, Riesgo e Hitos con persistencia de snapshots y sincronizacion asincrona.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final FamilyRepository familyRepository;
    private final EvaluationRepository evaluationRepository;
    private final AiService aiService;
    private final RiskSnapshotRepository riskSnapshotRepository;
    private final ChecklistItemRepository checklistRepository;
    private final FamilyLogbookRepository logbookRepository;
    private final ImprovementPlanRepository planRepository;
    private final PlanTaskRepository planTaskRepository;
    private final RabbitTemplate rabbitTemplate;

    // ── Nivel de consciencia (misma escala que RiskService: 1=mín → 5=máx) ──
    private static int deriveConsciousnessLevel(double icf) {
        if (icf < 20) return 1;
        if (icf < 40) return 2;
        if (icf < 60) return 3;
        if (icf < 80) return 4;
        return 5;
    }

    private static String deriveConsciousnessLabel(int level) {
        return switch (level) {
            case 1  -> "Inconsciente";
            case 2  -> "Reactiva";
            case 3  -> "Consciente";
            case 4  -> "Madurando";
            default -> "Plena";
        };
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse calculateLatestResults(Long familyId) {
        log.info("📊 [ANALYTICS] Iniciando calculo integral para familia ID: {}", familyId);

        // 1. Recuperar Entidad Core
        Family family = familyRepository.findById(familyId)
                .orElseGet(() -> familyRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("No se encontro ninguna familia en el sistema.")));

        // 2. Recuperar Historial de Evaluaciones
        List<Evaluation> allEvals = evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(familyId);
        Evaluation firstEval = allEvals.isEmpty() ? null : allEvals.get(0);
        Evaluation lastEval = allEvals.isEmpty() ? null : allEvals.get(allEvals.size() - 1);

        // 3. Recuperar ultimo Riesgo
        RiskSnapshot lastRisk = riskSnapshotRepository.findFirstByFamilyIdOrderByCreatedAtDesc(familyId).orElse(null);

        // 4. Calculo de Crecimiento de Consciencia
        double growth = 0.0;
        if (firstEval != null && firstEval.getIcf() != null && lastEval != null && lastEval.getIcf() != null) {
            growth = lastEval.getIcf() - firstEval.getIcf();
        }

        // 5. Mapeo de Dimensiones para el Radar Chart
        Map<String, Double> dims = new HashMap<>();
        if (lastEval != null && lastEval.getDimensionScores() != null) {
            lastEval.getDimensionScores().stream()
                .filter(ds -> ds != null && ds.getDimensionName() != null && ds.getScore() != null)
                .forEach(ds -> dims.put(ds.getDimensionName(), (double) ds.getScore()));
        }

        // 6. Mapeo Seguro de Nivel de Riesgo
        RiskLevel mappedLevel = RiskLevel.LOW;
        if (lastRisk != null && lastRisk.getRiskLevel() != null) {
            try {
                mappedLevel = RiskLevel.valueOf(lastRisk.getRiskLevel().toUpperCase());
            } catch (Exception e) {
                mappedLevel = RiskLevel.LOW;
            }
        }

        // 7. Generacion de Insight mediante IA (Motor de Pensamiento Critico)
        String insight;
        try {
            insight = aiService.generateDashboardInsight(family, dims, mappedLevel.name());
        } catch (Exception e) {
            log.error("⚠️ [ANALYTICS] No se pudo generar insight de IA: {}", e.getMessage());
            insight = "Sincronizando reflexiones profundas... El motor de IA esta procesando el contexto familiar.";
        }

        // 8. Checklist — solo para «Suggested Actions» (no confundir con PlanTask)
        List<ChecklistItem> allChecklist = checklistRepository.findByFamilyIdOrderByCreatedAtDesc(familyId);
        long totalChecklistItems    = allChecklist.size();
        long completedChecklistItems = allChecklist.stream().filter(ChecklistItem::isCompleted).count();

        List<SuggestedActionDto> suggestedActions = allChecklist.stream()
                .limit(5)
                .map(item -> SuggestedActionDto.builder()
                        .id(item.getId())
                        .description(item.getDescription())
                        .dimension(item.getDimension())
                        .completed(item.isCompleted())
                        .build())
                .collect(Collectors.toList());

        // 8b. Tareas del Plan (PlanTask) — fuente correcta para el progreso de la ruta
        long totalPlanTasks     = planTaskRepository.countByFamilyId(familyId);
        long completedPlanTasks = planTaskRepository.countCompletedByFamilyId(familyId);

        // 9. Recuperacion de Bitacora
        long openLogbookItems = logbookRepository.findByFamilyIdAndStatusOrderByCreatedAtDesc(familyId, LogbookStatus.OPEN).size();
        String latestAgreement = logbookRepository.findByFamilyIdOrderByCreatedAtDesc(familyId).stream()
                .filter(e -> e.getFamilyAgreement() != null && !e.getFamilyAgreement().isBlank())
                .map(FamilyLogbookEntry::getFamilyAgreement)
                .findFirst()
                .orElse("No hay acuerdos recientes registrados.");

        // 9.5 Recuperación de Reporte de Plan IA
        List<com.integrityfamily.domain.ImprovementPlan> familyPlans = planRepository.findByFamilyId(familyId);
        String planAiReport = (familyPlans != null && !familyPlans.isEmpty()) ?
                familyPlans.get(familyPlans.size() - 1).getAiReport() :
                "No hay reporte de plan disponible.";

        // 10. Motor de Activacion Proactiva Sentinel (Capa de Contencion)
        boolean sentinelTriggered = Boolean.TRUE.equals(family.getSentinelActive())
                || (lastEval != null && lastEval.getIcf() != null && lastEval.getIcf() < 40.0)
                || (growth < -15.0)
                || (openLogbookItems > 3);

        // 11. Ajuste de recomendacion IA ante estado de crisis
        String finalInsight = sentinelTriggered ? "⚠️ [S.O.S NODO] Protocolo de Contencion Activado. " + insight : insight;

        if (openLogbookItems > 0) {
            finalInsight += " Hay " + openLogbookItems + " situaciones pendientes en la bitacora.";
        }

        // 12. Nivel de consciencia
        // RiskService.calculateAndCreate() ya persiste un RiskSnapshot correcto tras cada
        // evaluación.  Aquí solo leemos ese valor — no escribimos un snapshot nuevo en cada
        // GET del dashboard (evita write-on-read y el bucle de valores estancados).
        int consciousnessLevel;
        String consciousnessLabel;
        if (lastRisk != null && lastRisk.getConsciousnessLevel() != null) {
            consciousnessLevel = lastRisk.getConsciousnessLevel();
            consciousnessLabel = lastRisk.getConsciousnessLabel() != null
                    ? lastRisk.getConsciousnessLabel()
                    : deriveConsciousnessLabel(lastRisk.getConsciousnessLevel());
        } else {
            double icfForLevel = (lastEval != null && lastEval.getIcf() != null) ? lastEval.getIcf() : 0.0;
            consciousnessLevel = deriveConsciousnessLevel(icfForLevel);
            consciousnessLabel = deriveConsciousnessLabel(consciousnessLevel);
        }

        // 13. Construccion del DTO de Respuesta
        DashboardSummaryResponse response = DashboardSummaryResponse.builder()
                .familyId(familyId)
                .familyName(family.getName())
                .familyCode(family.getFamilyCode())
                .currentMilestone(family.getCurrentMilestone())
                .totalMembers((long) (family.getMembers() != null ? family.getMembers().size() : 0))
                .totalEvaluations((long) allEvals.size())
                .latestRiskLevel(mappedLevel)
                .latestGlobalScore(BigDecimal.valueOf(lastEval != null && lastEval.getIcf() != null ? lastEval.getIcf() : 0.0))
                .latestConsciousnessLevel(consciousnessLevel)
                .latestConsciousnessLabel(consciousnessLabel)
                .hasCrisis(sentinelTriggered)
                .isSentinelActive(sentinelTriggered)
                .totalChecklistItems(totalChecklistItems)
                .completedChecklistItems(completedChecklistItems)
                .totalPlanTasks(totalPlanTasks)
                .completedPlanTasks(completedPlanTasks)
                .pillarProgress(totalPlanTasks > 0
                        ? (double) completedPlanTasks * 100.0 / totalPlanTasks
                        : 0.0)
                .awarenessGrowth(growth)
                .dimensionScores(dims)
                .suggestedActions(suggestedActions)
                .aiRecommendation(finalInsight)
                .planAiReport(planAiReport)
                .openLogbookEntriesCount(openLogbookItems)
                .latestFamilyAgreement(latestAgreement)
                .build();

        // 14. Sincronizacion Asincrona via RabbitMQ
        log.info("📤 [ANALYTICS] Sincronizando resultados con RabbitMQ...");
        try {
            // A) DashboardSummaryResponse -> PlanConsumer genera tareas adaptativas
            rabbitTemplate.convertAndSend(
                    RabbitConfig.EXCHANGE_NAME,
                    "tasks.suggested",
                    response);

            // B) SystemEvent bien formado -> AnalyticsEventConsumer actualiza el Read Model
            //    FIX: antes se publicaba un objeto nulo que causaba el warning "campos nulos"
            SystemEvent analyticsEvent = SystemEvent.of(
                    "analytics.updated",
                    familyId,
                    response,
                    "SYSTEM");
            rabbitTemplate.convertAndSend(
                    RabbitConfig.EXCHANGE_NAME,
                    "analytics.updated",
                    analyticsEvent);

            log.info("✅ [ANALYTICS] Sincronizacion RabbitMQ completada.");
        } catch (Exception e) {
            log.error("❌ [ANALYTICS] Error al enviar a RabbitMQ (no critico): {}", e.getMessage());
        }

        return response;
    }

    @Override
    public void invalidateCacheAndRecalculate(Long familyId) {
        log.warn("🔄 [ANALYTICS] Invalidando cache y recalculando para familia: {}", familyId);
        calculateLatestResults(familyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getEvolutionRadarData(Long familyId) {
        log.info("🎯 [ANALYTICS] Generando radar de evolución dinámico para familia ID: {}", familyId);

        List<Evaluation> history = evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(familyId);
        List<String> labels = List.of("Emociones", "Comunicación", "Hábitos", "Tiempos");

        if (history == null || history.isEmpty()) {
            log.warn("⚠️ [ANALYTICS] No se encontró historial de evaluaciones para familia ID: {}. Retornando valores base cero.", familyId);
            return List.of(
                Map.of(
                    "labels", labels,
                    "name", "Actual",
                    "value", List.of(0.0, 0.0, 0.0, 0.0)
                ),
                Map.of(
                    "labels", labels,
                    "name", "Inicio",
                    "value", List.of(0.0, 0.0, 0.0, 0.0)
                )
            );
        }

        Evaluation firstEval = history.get(0);
        Evaluation lastEval = history.get(history.size() - 1);

        List<Double> actualValues = List.of(
            getScoreForDimension(lastEval, "emociones"),
            getScoreForDimension(lastEval, "comunicacion"),
            getScoreForDimension(lastEval, "habitos"),
            getScoreForDimension(lastEval, "tiempos")
        );

        List<Double> inicioValues = List.of(
            getScoreForDimension(firstEval, "emociones"),
            getScoreForDimension(firstEval, "comunicacion"),
            getScoreForDimension(firstEval, "habitos"),
            getScoreForDimension(firstEval, "tiempos")
        );

        return List.of(
            Map.of(
                "labels", labels,
                "name", "Actual",
                "value", actualValues
            ),
            Map.of(
                "labels", labels,
                "name", "Inicio",
                "value", inicioValues
            )
        );
    }

    private Double getScoreForDimension(Evaluation eval, String key) {
        if (eval == null || eval.getDimensionScores() == null) return 50.0;
        return eval.getDimensionScores().stream()
            .filter(ds -> ds != null && ds.getDimensionName() != null)
            .filter(ds -> {
                String name = ds.getDimensionName().toLowerCase();
                if (key.equals("comunicacion")) {
                    return name.contains("comunica");
                } else if (key.equals("emociones")) {
                    return name.contains("emocio") || name.contains("clima");
                } else if (key.equals("habitos")) {
                    return name.contains("habito") || name.contains("hábito") || name.contains("convive");
                } else if (key.equals("tiempos")) {
                    return name.contains("tiempo") || name.contains("conexi");
                }
                return false;
            })
            .map(ds -> ds.getScore())
            .findFirst()
            .orElse(50.0);
    }
}
