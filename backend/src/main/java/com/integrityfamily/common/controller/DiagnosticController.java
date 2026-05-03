package com.integrityfamily.common.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.repository.PlanRepository;
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
        log.info("Ã°Å¸â€Â Iniciando diagnÃƒÂ³stico de base de datos para familia: {}", familyId);
        
        // Buscamos evaluaciones completadas para esta familia
        List<Evaluation> evaluations = evaluationRepository.findByFamilyIdOrderByStartedAtDesc(familyId);
        int fixedCount = 0;

        for (Evaluation eval : evaluations) {
            // Si la evaluaciÃƒÂ³n estÃƒÂ¡ completada pero no tiene plan, lo generamos
            if (eval.getFinalizedAt() != null) {
                boolean hasPlan = planRepository.existsByEvaluationId(eval.getId());
                if (!hasPlan) {
                    log.info("Ã°Å¸â€ºÂ Ã¯Â¸Â Reparando plan faltante para EvaluaciÃƒÂ³n ID: {}", eval.getId());
                    try {
                        planGenerationService.generatePlanFromEvaluation(Map.of(
                            "evaluationId", eval.getId(),
                            "familyId", familyId,
                            "riskLevel", "MEDIUM",
                            "requiresImmediatePlan", eval.getHasCrisis() != null ? eval.getHasCrisis() : false
                        ));
                        fixedCount++;
                    } catch (Exception e) {
                        log.error("Ã¢ÂÅ’ Error reparando plan {}: {}", eval.getId(), e.getMessage());
                    }
                }
            }
        }

        return ApiResponse.ok("Ã°Å¸Â©Âº DiagnÃƒÂ³stico completado. Se generaron " + fixedCount + " planes faltantes.");
    }
}


