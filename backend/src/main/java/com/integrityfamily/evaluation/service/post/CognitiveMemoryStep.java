package com.integrityfamily.evaluation.service.post;

import com.integrityfamily.cognitive.service.FamilyMemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** [MEMORIA COGNITIVA] Captura el episodio y consolida el patrón semántico. */
@Slf4j
@Component
@Order(40)
@RequiredArgsConstructor
public class CognitiveMemoryStep implements PostFinalizationStep {

    private final FamilyMemoryService familyMemoryService;

    @Override
    public String name() { return "COGNITIVE_MEMORY"; }

    @Override
    public void execute(PostFinalizationContext ctx) {
        familyMemoryService.captureEvaluationMemory(ctx.evaluation());
        log.info("🧠 [EVALUATION] Memoria episódica capturada en el sistema cognitivo.");
    }
}
