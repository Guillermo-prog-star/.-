package com.integrityfamily.plan.service;

import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.ImprovementPlan;
import com.integrityfamily.domain.PlanTask;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.repository.ImprovementPlanRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * SDD SPEC 6.6: ContinuityEngine.
 * Motor inteligente y proactivo longitudinal.
 * Compara diagnósticos sucesivos, analiza el delta de ICF, el avance de tareas de planes anteriores
 * y las crisis reportadas para orientar al AiPlanGenerator con un tipo de plan específico:
 * Intervención (Empeoró), Profundización (Mejoró), Recalibración (Estancado), Alerta Sentinel (Crisis).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ContinuityEngine {

    private final EvaluationRepository evaluationRepository;
    private final ImprovementPlanRepository planRepository;

    @Builder
    public record ContinuityAnalysis(
            EvolutionStatus status,
            double priorIcf,
            double currentIcf,
            double icfDelta,
            int totalPriorTasks,
            int completedPriorTasks,
            double taskCompletionRate,
            boolean hasCrisis,
            String recommendedPlanType,
            String analysisSummary
    ) {}

    public enum EvolutionStatus {
        IMPROVED,      // Mejoró (ICF delta > +5.0)
        DETERIORATED,  // Empeoró (ICF delta < -5.0)
        STAGNATED,     // Estancado (ICF delta entre -5.0 y +5.0)
        CRISIS         // Alerta Sentinel (Crisis o ICF crítico < 30.0)
    }

    /**
     * Realiza un análisis longitudinal de la familia comparando la última evaluación con la anterior.
     */
    @Transactional(readOnly = true)
    public ContinuityAnalysis analyzeFamilyContinuity(Long familyId, Evaluation currentEvaluation) {
        log.info("🧬 [CONTINUITY-ENGINE] Iniciando análisis longitudinal para la familia ID: {}", familyId);

        // 1. Obtener todas las evaluaciones finalizadas de la familia
        List<Evaluation> history = evaluationRepository.findByFamilyId(familyId).stream()
                .filter(e -> e.getFinalizedAt() != null)
                .sorted(Comparator.comparing(Evaluation::getFinalizedAt).reversed())
                .toList();

        double currentIcf = currentEvaluation.getIcf() != null ? currentEvaluation.getIcf() : 0.0;
        boolean currentCrisis = currentEvaluation.getHasCrisis() != null && currentEvaluation.getHasCrisis();

        // Si es la primera evaluación, retornar análisis inicial básico
        if (history.size() <= 1) {
            log.info("🌱 [CONTINUITY-ENGINE] Primera evaluación completada. No existe histórico previo para comparar.");
            EvolutionStatus status = currentIcf < 30.0 || currentCrisis ? EvolutionStatus.CRISIS : EvolutionStatus.IMPROVED;
            String planType = status == EvolutionStatus.CRISIS ? "PLAN DE CONTENCIÓN URGENTE" : "PLAN DE INICIACIÓN Y RECONOCIMIENTO";

            return ContinuityAnalysis.builder()
                    .status(status)
                    .priorIcf(0.0)
                    .currentIcf(currentIcf)
                    .icfDelta(0.0)
                    .totalPriorTasks(0)
                    .completedPriorTasks(0)
                    .taskCompletionRate(0.0)
                    .hasCrisis(currentCrisis)
                    .recommendedPlanType(planType)
                    .analysisSummary("Primer diagnóstico completado. Se establece una línea base de ICF: " + currentIcf)
                    .build();
        }

        // Obtener la evaluación anterior más reciente (excluyendo la actual si ya está en la lista)
        Evaluation priorEvaluation = history.stream()
                .filter(e -> !e.getId().equals(currentEvaluation.getId()))
                .findFirst()
                .orElse(null);

        if (priorEvaluation == null) {
            log.warn("⚠️ [CONTINUITY-ENGINE] No se encontró una evaluación anterior válida distinta a la actual.");
            return createDefaultAnalysis(currentIcf, currentCrisis);
        }

        double priorIcf = priorEvaluation.getIcf() != null ? priorEvaluation.getIcf() : 0.0;
        double icfDelta = currentIcf - priorIcf;

        log.info("📊 [CONTINUITY-ENGINE] ICF actual: {} | ICF anterior: {} | Delta: {}", currentIcf, priorIcf, icfDelta);

        // 2. Analizar cumplimiento de tareas del plan anterior
        int totalPriorTasks = 0;
        int completedPriorTasks = 0;
        double taskCompletionRate = 0.0;

        Optional<ImprovementPlan> priorPlanOpt = planRepository.findByFamilyId(familyId).stream()
                .filter(p -> p.getEvaluation() != null && p.getEvaluation().getId().equals(priorEvaluation.getId()))
                .findFirst();

        if (priorPlanOpt.isPresent()) {
            List<PlanTask> tasks = priorPlanOpt.get().getTasks();
            if (tasks != null && !tasks.isEmpty()) {
                totalPriorTasks = tasks.size();
                completedPriorTasks = (int) tasks.stream().filter(PlanTask::isCompleted).count();
                taskCompletionRate = ((double) completedPriorTasks / totalPriorTasks) * 100.0;
                log.info("📋 [CONTINUITY-ENGINE] Tareas del plan anterior: {} completadas de {} ({}%)",
                        completedPriorTasks, totalPriorTasks, taskCompletionRate);
            }
        }

        // 3. Determinar el estado de evolución y tipo de plan recomendado
        EvolutionStatus status;
        String planType;
        String summary;

        if (currentCrisis || currentIcf < 30.0) {
            status = EvolutionStatus.CRISIS;
            planType = "PLAN DE INTERVENCIÓN EN CRISIS (SENTINEL)";
            summary = String.format("Se ha detectado una situación de CRISIS familiar o puntaje crítico de ICF (%.2f). Se requiere contención inmediata.", currentIcf);
        } else if (icfDelta > 5.0) {
            status = EvolutionStatus.IMPROVED;
            planType = "PLAN DE PROFUNDIZACIÓN Y DESARROLLO";
            summary = String.format("Evolución altamente POSITIVA. El ICF mejoró en +%.2f puntos (de %.2f a %.2f). Tasa de cumplimiento previa: %.1f%%.",
                    icfDelta, priorIcf, currentIcf, taskCompletionRate);
        } else if (icfDelta < -5.0) {
            status = EvolutionStatus.DETERIORATED;
            planType = "PLAN DE INTERVENCIÓN CORRECTIVO";
            summary = String.format("Evolución NEGATIVA detectada. El ICF disminuyó en %.2f puntos (de %.2f a %.2f). Se recomienda enfocar misiones en dimensiones con retroceso.",
                    Math.abs(icfDelta), priorIcf, currentIcf, taskCompletionRate);
        } else {
            status = EvolutionStatus.STAGNATED;
            planType = "PLAN DE RECALIBRACIÓN E INCENTIVOS";
            summary = String.format("Evolución ESTANCADA. El ICF varió levemente en %.2f puntos (de %.2f a %.2f). Se deben reprogramar microacciones con mayor seguimiento de evidencias.",
                    icfDelta, priorIcf, currentIcf, taskCompletionRate);
        }

        log.info("🧬 [CONTINUITY-ENGINE] Diagnóstico longitudinal finalizado. Estado: {} | Plan Recomendado: {}", status, planType);

        return ContinuityAnalysis.builder()
                .status(status)
                .priorIcf(priorIcf)
                .currentIcf(currentIcf)
                .icfDelta(icfDelta)
                .totalPriorTasks(totalPriorTasks)
                .completedPriorTasks(completedPriorTasks)
                .taskCompletionRate(taskCompletionRate)
                .hasCrisis(currentCrisis)
                .recommendedPlanType(planType)
                .analysisSummary(summary)
                .build();
    }

    private ContinuityAnalysis createDefaultAnalysis(double currentIcf, boolean hasCrisis) {
        return ContinuityAnalysis.builder()
                .status(hasCrisis ? EvolutionStatus.CRISIS : EvolutionStatus.STAGNATED)
                .priorIcf(0.0)
                .currentIcf(currentIcf)
                .icfDelta(0.0)
                .totalPriorTasks(0)
                .completedPriorTasks(0)
                .taskCompletionRate(0.0)
                .hasCrisis(hasCrisis)
                .recommendedPlanType("PLAN GENERAL RECALIBRADO")
                .analysisSummary("Análisis parcial por falta de datos históricos limpios.")
                .build();
    }
}
