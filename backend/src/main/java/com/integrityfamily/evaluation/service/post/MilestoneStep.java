package com.integrityfamily.evaluation.service.post;

import com.integrityfamily.milestone.service.MilestoneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Avanza el hito de la familia. No bloqueante para la evaluación. */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class MilestoneStep implements PostFinalizationStep {

    private final MilestoneService milestoneService;

    @Override
    public String name() { return "MILESTONE_ADVANCE"; }

    @Override
    public void execute(PostFinalizationContext ctx) {
        Long familyId = ctx.evaluation().getFamily().getId();
        milestoneService.advanceMilestone(familyId);
        log.info("🚀 [EVALUATION] Hito de la familia ID {} avanzado correctamente.", familyId);
    }
}
