package com.integrityfamily.common.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.EvaluationStatus;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.repository.ImprovementPlanRepository;
import com.integrityfamily.plan.service.PlanGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

/**
 * SDD: Controlador de Diagnóstico de Salud del Sistema.
 * Refactorizado para usar la arquitectura de ImprovementPlan con Self-Healing proactivo.
 */
@RestController
@RequestMapping("/api/diagnostic")
@RequiredArgsConstructor
@Slf4j
public class DiagnosticController {

    private final EvaluationRepository evaluationRepository;
    private final ImprovementPlanRepository planRepository;
    private final PlanGenerationService planGenerationService;
    private final com.integrityfamily.domain.repository.FamilyRepository familyRepository;
    private final com.integrityfamily.plan.service.PlanService planService;

    @GetMapping("/fix-plans/{familyId}")
    public ApiResponse<String> fixFamilyPlans(@PathVariable Long familyId) {
        log.info("🔍 Iniciando diagnóstico de base de datos para familia: {}", familyId);
        
        List<Evaluation> evaluations = evaluationRepository.findByFamilyIdOrderByStartedAtDesc(familyId);
        int fixedCount = 0;

        for (Evaluation eval : evaluations) {
            if (eval.getFinalizedAt() != null) {
                boolean hasPlan = planRepository.existsByEvaluationId(eval.getId());
                if (!hasPlan) {
                    log.info("🛠️ Reparando plan faltante para Evaluación ID: {}", eval.getId());
                    try {
                        planGenerationService.generatePlanFromEvaluation(Map.of(
                            "evaluationId", eval.getId(),
                            "familyId", familyId,
                            "riskLevel", "MEDIUM",
                            "requiresImmediatePlan", eval.getHasCrisis() != null ? eval.getHasCrisis() : false
                        ));
                        fixedCount++;
                    } catch (Exception e) {
                        log.error("❌ Error reparando plan {}: {}", eval.getId(), e.getMessage());
                    }
                }
            }
        }

        // Si no se generó ningún plan y la familia no tiene planes en absoluto, generamos uno proactivamente
        if (fixedCount == 0 && planRepository.findByFamilyId(familyId).isEmpty()) {
            log.info("⚠️ [SELF-HEALING] La familia {} no tiene evaluaciones finalizadas ni planes. Generando plan de contingencia...", familyId);
            com.integrityfamily.domain.Family family = familyRepository.findById(familyId).orElse(null);
            if (family != null) {
                Evaluation eval = evaluations.isEmpty() ? null : evaluations.get(0);
                if (eval == null) {
                    eval = Evaluation.builder()
                            .family(family)
                            .status(EvaluationStatus.FINALIZED)
                            .startedAt(LocalDateTime.now())
                            .finalizedAt(LocalDateTime.now())
                            .hasCrisis(false)
                            .icf(85.0)
                            .riskLevel("MODERATE")
                            .criticalDimension("COMUNICACION")
                            .algorithmVersion("RISK_ALGO_V1")
                            .milestoneKey("W1")
                            .answers(new java.util.ArrayList<>())
                            .dimensionScores(new java.util.ArrayList<>())
                            .build();
                    eval = evaluationRepository.save(eval);
                } else if (eval.getFinalizedAt() == null) {
                    eval.setFinalizedAt(LocalDateTime.now());
                    eval.setStatus(EvaluationStatus.FINALIZED);
                    eval = evaluationRepository.save(eval);
                }
                
                try {
                    planService.generateDeterministicPlan(eval.getId());
                    fixedCount++;
                    log.info("✅ Plan determinístico de contingencia generado con éxito para familia {}.", familyId);
                } catch (Exception ex) {
                    log.error("❌ Fallo generando plan determinístico: {}", ex.getMessage());
                }
            }
        }

        return ApiResponse.ok("🩺 Diagnóstico completado. Se generaron " + fixedCount + " planes faltantes.");
    }
}
