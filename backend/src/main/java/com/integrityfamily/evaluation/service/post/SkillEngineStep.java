package com.integrityfamily.evaluation.service.post;

import com.integrityfamily.cognitive.service.FamilySkillEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** [SKILL ENGINE] Detecta patrones, aplica skills y extrae nuevas habilidades. */
@Slf4j
@Component
@Order(50)
@RequiredArgsConstructor
public class SkillEngineStep implements PostFinalizationStep {

    private final FamilySkillEngine familySkillEngine;

    @Override
    public String name() { return "SKILL_ENGINE"; }

    @Override
    public void execute(PostFinalizationContext ctx) {
        FamilySkillEngine.SkillEngineResult result =
                familySkillEngine.analyze(ctx.evaluation().getFamily().getId(), ctx.evaluation());
        if (result.hasNewSkill()) {
            log.info("🌱 [EVALUATION] Nueva habilidad cognitiva extraída: '{}'",
                    result.newSkillExtracted().getSkillName());
        }
        if (result.hasAppliedSkills()) {
            log.info("⚙️ [EVALUATION] {} skills activadas para esta familia.",
                    result.appliedSkills().size());
        }
    }
}
