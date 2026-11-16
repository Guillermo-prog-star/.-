package com.integrityfamily.evaluation.service.post;

import com.integrityfamily.plan.service.PlanTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * [DIAGNÓSTICO CONSCIENTE] Genera misiones automáticas según el rol del miembro.
 *
 * <p>La generación del plan NO ocurre aquí: se dispara solo por el evento
 * "evaluation.completed" (PLAN_QUEUE). La llamada síncrona que existió antes
 * provocaba doble generación.
 */
@Slf4j
@Component
@Order(30)
@RequiredArgsConstructor
public class DiagnosisTasksStep implements PostFinalizationStep {

    private final PlanTaskService planTaskService;

    @Override
    public String name() { return "DIAGNOSIS_TASKS"; }

    @Override
    public void execute(PostFinalizationContext ctx) {
        planTaskService.generateTasksFromDiagnosis(ctx.evaluation());
        log.info("🎯 [EVALUATION] Misiones automáticas de diagnóstico generadas.");
    }
}
