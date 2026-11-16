package com.integrityfamily.evaluation.service.post;

import com.integrityfamily.cognitive.service.FamilyReflectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** [REFLEXIÓN AUTÓNOMA] Autoevaluación del sistema sobre la efectividad de las intervenciones. */
@Slf4j
@Component
@Order(80)
@RequiredArgsConstructor
public class ReflectionStep implements PostFinalizationStep {

    private final FamilyReflectionService familyReflectionService;

    @Override
    public String name() { return "AUTONOMOUS_REFLECTION"; }

    @Override
    public void execute(PostFinalizationContext ctx) {
        FamilyReflectionService.ReflectionReport report =
                familyReflectionService.reflect(ctx.evaluation().getFamily().getId());
        log.info("🪞 [EVALUATION] Reflexión autónoma completada. Efectividad: {} | Abandono: {}",
                report.effectiveness().level(), report.abandonmentRisk().level());
    }
}
