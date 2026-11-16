package com.integrityfamily.evaluation.service.post;

import com.integrityfamily.cognitive.service.NarrativeEvolutionEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** [NARRATIVA] Evoluciona la historia familiar y detecta puntos de inflexión. */
@Slf4j
@Component
@Order(70)
@RequiredArgsConstructor
public class NarrativeStep implements PostFinalizationStep {

    private final NarrativeEvolutionEngine narrativeEvolutionEngine;

    @Override
    public String name() { return "NARRATIVE_EVOLUTION"; }

    @Override
    public void execute(PostFinalizationContext ctx) {
        NarrativeEvolutionEngine.NarrativeSnapshot narrative =
                narrativeEvolutionEngine.evolve(ctx.evaluation().getFamily().getId(), ctx.evaluation());
        log.info("📖 [EVALUATION] Narrativa evolucionada. Capítulo #{}: '{}' | Fase: {} | Turning point: {}",
                narrative.currentChapter() != null ? narrative.currentChapter().getChapterNumber() : 1,
                narrative.currentChapter() != null ? narrative.currentChapter().getTitle() : "—",
                narrative.currentPhase(),
                narrative.turningPointDetected());
    }
}
