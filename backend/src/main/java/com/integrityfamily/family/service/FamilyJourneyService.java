package com.integrityfamily.family.service;

import com.integrityfamily.domain.repository.SprintDailyRepository;
import com.integrityfamily.domain.repository.SprintMissionRepository;
import com.integrityfamily.dna.repository.FamilyDnaRepository;
import com.integrityfamily.domain.EvaluationStatus;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.*;
import com.integrityfamily.family.dto.FamilyJourneyResponse;
import com.integrityfamily.family.dto.FamilyJourneyResponse.*;
import com.integrityfamily.legado.repository.FamilyLegacyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Capa rectora del ecosistema Integrity Family.
 *
 * Evalúa el estado real de cada uno de los 13 niveles del viaje familiar
 * consultando los módulos existentes (sin tocarlos). La respuesta guía a
 * la familia y al Guardián hacia el siguiente paso concreto.
 *
 * Regla: un nivel es COMPLETE cuando tiene al menos un registro real.
 *        Un nivel es IN_PROGRESS cuando está parcialmente construido.
 *        Un nivel es NEXT cuando todos los anteriores están COMPLETE.
 *        Un nivel es LOCKED cuando el anterior no está COMPLETE.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FamilyJourneyService {

    private final FamilyRepository              familyRepository;
    private final MemberRepository              memberRepository;
    private final FamilyIdentityProfileRepository identityRepository;
    private final FamilyDnaRepository           dnaRepository;
    private final EvaluationRepository          evaluationRepository;
    private final ImprovementPlanRepository     planRepository;
    private final PlanTaskRepository            planTaskRepository;
    private final FamilySprintRepository        sprintRepository;
    private final SprintDailyRepository         dailyRepository;
    private final TaskEvidenceRepository        evidenceRepository;
    private final AiInferenceRepository         aiInferenceRepository;
    private final FamilyLegacyRepository        legacyRepository;
    private final FamilyDocumentaryRepository   documentaryRepository;

    @Transactional(readOnly = true)
    public FamilyJourneyResponse evaluate(Long familyId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new IllegalArgumentException("Familia no encontrada: " + familyId));

        // ── Evaluar cada nivel ────────────────────────────────────────────────
        boolean[] completed = new boolean[14];

        completed[0]  = true; // Plataforma siempre activa
        completed[1]  = evalIdentidad(family);
        completed[2]  = evalMiembros(familyId);
        completed[3]  = evalGuardian(family);
        completed[4]  = evalAdn(familyId);
        completed[5]  = evalDiagnostico(familyId);
        completed[6]  = evalPlan(familyId);
        completed[7]  = evalMisiones(familyId);
        completed[8]  = evalSprint(familyId);
        completed[9]  = evalDaily(familyId);
        completed[10] = evalEvidencias(familyId);
        completed[11] = evalConsultorIa(familyId);
        completed[12] = evalConsejo(family);
        completed[13] = evalLegado(familyId);

        // ── Construir niveles con estados ─────────────────────────────────────
        List<JourneyLevel> levels = buildLevels(familyId, family, completed);

        // ── Calcular nivel actual y progreso ──────────────────────────────────
        int currentLevel = 0;
        for (int i = 0; i <= 13; i++) {
            if (completed[i]) currentLevel = i;
            else break;
        }

        long completedCount = 0;
        for (boolean c : completed) if (c) completedCount++;
        int progress = (int) Math.round((completedCount / 14.0) * 100);

        // ── Siguiente acción ──────────────────────────────────────────────────
        int nextLevel = Math.min(currentLevel + 1, 13);
        String nextAction = buildNextAction(nextLevel, levels.get(nextLevel).status());

        log.debug("[JOURNEY] Familia {} — nivel actual {}, progreso {}%", familyId, currentLevel, progress);

        return new FamilyJourneyResponse(familyId, family.getName(),
                currentLevel, progress, levels, nextAction, nextLevel);
    }

    // ─── Evaluadores por nivel ────────────────────────────────────────────────

    private boolean evalIdentidad(Family family) {
        boolean hasName = family.getName() != null && !family.getName().isBlank();
        boolean hasProfile = identityRepository.findByFamilyId(family.getId()).isPresent();
        return hasName && hasProfile;
    }

    private boolean evalMiembros(Long familyId) {
        return memberRepository.countByFamilyId(familyId) >= 2; // mínimo 2 miembros
    }

    private boolean evalGuardian(Family family) {
        return family.getGuardianMemberId() != null;
    }

    private boolean evalAdn(Long familyId) {
        return dnaRepository.findByFamilyId(familyId).isPresent();
    }

    private boolean evalDiagnostico(Long familyId) {
        return evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(familyId)
                .stream().anyMatch(e -> e.getStatus() == EvaluationStatus.FINALIZED);
    }

    private boolean evalPlan(Long familyId) {
        return !planRepository.findByFamilyId(familyId).isEmpty();
    }

    private boolean evalMisiones(Long familyId) {
        return planTaskRepository.countByFamilyId(familyId) > 0;
    }

    private boolean evalSprint(Long familyId) {
        return sprintRepository.countByFamilyId(familyId) >= 1;
    }

    private boolean evalDaily(Long familyId) {
        // Hay daily si algún sprint tiene check-ins
        return sprintRepository.findByFamilyIdOrderByCreatedAtDesc(familyId).stream()
                .anyMatch(sprint ->
                        !dailyRepository.findBySprintIdOrderByCheckinDateDesc(sprint.getId()).isEmpty()
                );
    }

    private boolean evalEvidencias(Long familyId) {
        return !evidenceRepository.findByFamilyId(familyId).isEmpty();
    }

    private boolean evalConsultorIa(Long familyId) {
        return !aiInferenceRepository.findByFamilyIdOrderByCreatedAtDesc(familyId).isEmpty();
    }

    private boolean evalConsejo(Family family) {
        // El Consejo se activa cuando el legado tiene al menos una lección registrada
        // (historyLessons es el primer campo que se llena en el flujo de consejo).
        return legacyRepository.findByFamilyId(family.getId())
                .map(l -> l.getHistoryLessons() != null && !l.getHistoryLessons().isBlank())
                .orElse(false);
    }

    private boolean evalLegado(Long familyId) {
        boolean hasLegacy = legacyRepository.findByFamilyId(familyId).isPresent();
        boolean hasDocumentary = !documentaryRepository.findByFamilyIdOrderByCreatedAtDesc(familyId).isEmpty();
        return hasLegacy || hasDocumentary;
    }

    // ─── Builder de niveles ───────────────────────────────────────────────────

    private List<JourneyLevel> buildLevels(Long familyId, Family family, boolean[] completed) {
        List<JourneyLevel> list = new ArrayList<>();

        // Métricas de apoyo
        long memberCount = memberRepository.countByFamilyId(familyId);
        long sprintCount = sprintRepository.countByFamilyId(familyId);
        long evidenceCount = evidenceRepository.findByFamilyId(familyId).size();
        long inferenceCount = aiInferenceRepository.findByFamilyIdOrderByCreatedAtDesc(familyId).size();
        long planCount = planRepository.findByFamilyId(familyId).size();
        double icf = evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(familyId)
                .stream().filter(e -> e.getStatus() == EvaluationStatus.FINALIZED)
                .mapToDouble(e -> e.getIcf() != null ? e.getIcf() : 0)
                .max().orElse(0);

        list.add(level(0, "Plataforma", "Sistema operativo de Integrity Family activo",
                "⚙️", "/admin/stats", null, completed, true));
        list.add(level(1, "Identidad Familiar", "Nombre, historia, misión, visión y valores",
                "🏠", "/portal", identityRepository.findByFamilyId(familyId).isPresent()
                        ? "Perfil creado" : "Sin perfil", completed, false));
        list.add(level(2, "Miembros", "Integrantes con perfil y rol",
                "👥", "/members", memberCount + " miembro" + (memberCount == 1 ? "" : "s"), completed, false));
        list.add(level(3, "Guardián Familiar", "Director de la experiencia familiar",
                "🛡️", "/guardian/" + familyId + "/election",
                family.getGuardianMemberId() != null ? "Guardián activo" : "Sin guardián", completed, false));
        list.add(level(4, "ADN Familiar", "Identidad profunda: propósito, cultura, tradiciones",
                "🧬", "/family-dna",
                dnaRepository.findByFamilyId(familyId).isPresent() ? "ADN construido" : "Sin ADN", completed, false));
        list.add(level(5, "Diagnóstico Vivo", "Lectura continua del ICF en sus 4 dimensiones",
                "📊", "/evaluations/history",
                icf > 0 ? String.format("ICF %.0f", icf) : "Sin diagnóstico", completed, false));
        list.add(level(6, "Plan Familiar", "Respuesta estratégica al diagnóstico",
                "🗺️", "/plans",
                planCount > 0 ? planCount + " plan" + (planCount == 1 ? "" : "es") : "Sin plan", completed, false));
        list.add(level(7, "Misiones", "Acción concreta del plan convertida en objetivos",
                "🎯", "/plans",
                planCount > 0 ? "Misiones disponibles" : "Sin misiones", completed, false));
        list.add(level(8, "Sprint", "Ciclo operativo semanal de transformación",
                "⚡", "/bitacora",
                sprintCount > 0 ? sprintCount + " sprint" + (sprintCount == 1 ? "" : "s") : "Sin sprints", completed, false));
        list.add(level(9, "Daily", "Retroalimentación diaria del proceso",
                "📅", "/bitacora",
                completed[9] ? "Dailies registrados" : "Sin dailies", completed, false));
        list.add(level(10, "Evidencias", "Memoria verificable: fotos, videos, documentos, cápsulas",
                "📸", "/evidence/capture",
                evidenceCount > 0 ? evidenceCount + " evidencia" + (evidenceCount == 1 ? "" : "s") : "Sin evidencias", completed, false));
        list.add(level(11, "Consultor IA", "Motor de inferencia y traducción adaptativa",
                "🤖", "/evaluations/inferences",
                inferenceCount > 0 ? inferenceCount + " inferencia" + (inferenceCount == 1 ? "" : "s") : "Sin análisis IA", completed, false));
        list.add(level(12, "Consejo Familiar", "La familia decide con toda la información disponible",
                "🏛️", "/portal",
                completed[12] ? "Consejo activo" : "Pendiente", completed, false));
        list.add(level(13, "Legado", "Historia, aprendizaje, tradición — para la generación siguiente",
                "📜", "/legado",
                completed[13] ? "Legado iniciado" : "Sin legado", completed, false));

        return list;
    }

    private JourneyLevel level(int n, String name, String desc, String icon, String route,
                                String metric, boolean[] completed, boolean forceComplete) {
        JourneyStatus status;
        if (forceComplete || completed[n]) {
            status = JourneyStatus.COMPLETE;
        } else if (n > 0 && completed[n - 1]) {
            status = JourneyStatus.NEXT;
        } else if (n > 1 && completed[n - 2]) {
            status = JourneyStatus.IN_PROGRESS;
        } else {
            status = JourneyStatus.LOCKED;
        }

        String label = switch (status) {
            case COMPLETE     -> "Completado";
            case NEXT         -> "Siguiente paso";
            case IN_PROGRESS  -> "En progreso";
            case LOCKED       -> "Bloqueado";
        };

        return new JourneyLevel(n, name, desc, status, label, icon, route, metric);
    }

    private String buildNextAction(int nextLevel, JourneyStatus status) {
        if (status == JourneyStatus.COMPLETE) {
            return "¡El viaje familiar está completo! Continúa cultivando el legado.";
        }
        return switch (nextLevel) {
            case 1  -> "Completa el perfil de identidad familiar: historia, misión, visión y valores.";
            case 2  -> "Invita al menos a dos miembros de la familia y construye sus perfiles.";
            case 3  -> "Designa un Guardián Familiar: el coordinador de la experiencia.";
            case 4  -> "Construye el ADN Familiar: propósito, cultura, tradiciones y fortalezas.";
            case 5  -> "Realiza el primer diagnóstico completo del ICF familiar.";
            case 6  -> "Genera el Plan Familiar a partir del diagnóstico con el Consultor IA.";
            case 7  -> "Activa las misiones del plan: convierte los objetivos en acción.";
            case 8  -> "Inicia el primer Sprint Familiar: organiza las misiones en ciclos semanales.";
            case 9  -> "Registra el primer Daily: ¿Qué pasó hoy? ¿Qué aprendimos?";
            case 10 -> "Sube la primera evidencia: una foto, un acuerdo, una cápsula familiar.";
            case 11 -> "Consulta al Consultor IA para obtener análisis e inferencias del proceso.";
            case 12 -> "Convoca el primer Consejo Familiar para decidir juntos el siguiente ciclo.";
            case 13 -> "Construye el Legado: la historia que esta familia deja a la siguiente generación.";
            default -> "Continúa cultivando el viaje familiar.";
        };
    }
}
