package com.integrityfamily.evaluation.service.post;

import com.integrityfamily.cognitive.service.FamilyIdentityGraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** [GRAFO DE IDENTIDAD] Actualiza las dinámicas relacionales entre miembros. */
@Slf4j
@Component
@Order(60)
@RequiredArgsConstructor
public class IdentityGraphStep implements PostFinalizationStep {

    private final FamilyIdentityGraphService familyIdentityGraphService;

    @Override
    public String name() { return "IDENTITY_GRAPH"; }

    @Override
    public void execute(PostFinalizationContext ctx) {
        FamilyIdentityGraphService.GraphSnapshot graph =
                familyIdentityGraphService.updateGraph(ctx.evaluation().getFamily().getId(), ctx.evaluation());
        log.info("🕸️ [EVALUATION] Grafo de identidad actualizado. Díadas: {} | Cohesión: {} | Conflictos: {}",
                graph.totalDyads(), String.format("%.1f", graph.cohesionDensity()), graph.conflictiveEdges());
    }
}
