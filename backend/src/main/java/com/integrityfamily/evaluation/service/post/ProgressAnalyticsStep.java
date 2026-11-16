package com.integrityfamily.evaluation.service.post;

import com.integrityfamily.analytics.service.FamilyProgressAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** [ANALYTICS] Análisis de progreso longitudinal (Rediseño 6.6). */
@Slf4j
@Component
@Order(90)
@RequiredArgsConstructor
public class ProgressAnalyticsStep implements PostFinalizationStep {

    private final FamilyProgressAnalyticsService familyProgressAnalyticsService;

    @Override
    public String name() { return "PROGRESS_ANALYTICS"; }

    @Override
    public void execute(PostFinalizationContext ctx) {
        familyProgressAnalyticsService.analyzeProgress(ctx.evaluation().getId());
        log.info("📊 [EVALUATION] Análisis de progreso completado y snapshot guardado.");
    }
}
