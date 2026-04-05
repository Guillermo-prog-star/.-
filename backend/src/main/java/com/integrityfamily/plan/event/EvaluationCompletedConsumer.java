package com.integrityfamily.plan.event;

import com.integrityfamily.plan.service.PlanGenerationService;
import org.springframework.stereotype.Component;

@Component
public class EvaluationCompletedConsumer {

    private final PlanGenerationService planGenerationService;

    public EvaluationCompletedConsumer(PlanGenerationService planGenerationService) {
        this.planGenerationService = planGenerationService;
    }

    public void onEvaluationCompleted(Long evaluationId) {
        if (evaluationId == null) {
            return;
        }
        planGenerationService.generatePlanFromEvaluation(evaluationId);
    }
}