package com.integrityfamily.evaluation.service.post;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ejecuta los {@link PostFinalizationStep} en orden, aislando el fallo de cada
 * uno (ADR-020, corte B). Spring inyecta la lista ya ordenada por {@code @Order}.
 *
 * <p>Los pasos corren en la transacción de quien llama; este pipeline no abre
 * ninguna propia. Ver ADR-020, "Alcance excluido": el aislamiento transaccional
 * real queda para el corte C.
 */
@Slf4j
@Component
public class PostFinalizationPipeline {

    private final List<PostFinalizationStep> steps;

    public PostFinalizationPipeline(List<PostFinalizationStep> steps) {
        this.steps = steps;
    }

    public void run(PostFinalizationContext ctx) {
        for (PostFinalizationStep step : steps) {
            try {
                step.execute(ctx);
            } catch (Exception e) {
                log.error("⚠️ [EVALUATION][{}] Paso omitido (no bloqueante para la evaluación): {}",
                        step.name(), e.getMessage());
            }
        }
    }
}
