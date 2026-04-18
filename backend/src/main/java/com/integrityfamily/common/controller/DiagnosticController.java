package com.integrityfamily.common.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.evaluation.domain.Evaluation;
import com.integrityfamily.evaluation.repository.EvaluationRepository;
import com.integrityfamily.plan.repository.PlanRepository;
import com.integrityfamily.plan.service.PlanGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/diagnostic")
@RequiredArgsConstructor
@Slf4j
public class DiagnosticController {

    private final EvaluationRepository evaluationRepository;
    private final PlanRepository planRepository;
    private final PlanGenerationService planGenerationService;

    @GetMapping("/fix-plans/{familyId}")
    public ApiResponse<String> fixFamilyPlans(@PathVariable Long familyId) {
        log.info("🔍 Iniciando diagnóstico de base de datos para familia: {}", familyId);
        
        // Buscamos evaluaciones completadas para esta familia
        List<Evaluation> evaluations = evaluationRepository.findByFamilyIdOrderByStartedAtDesc(familyId);
        int fixedCount = 0;

        for (Evaluation eval : evaluations) {
            // Si la evaluación está completada pero no tiene plan, lo generamos
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

        return ApiResponse.ok("🩺 Diagnóstico completado. Se generaron " + fixedCount + " planes faltantes.");
    }
}
