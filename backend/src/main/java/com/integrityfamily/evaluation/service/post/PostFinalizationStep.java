package com.integrityfamily.evaluation.service.post;

/**
 * Un efecto posterior a la finalización de un diagnóstico (ADR-020, corte B).
 *
 * <p>Los pasos se ejecutan en el orden de {@code @Order} y dentro de la transacción
 * de {@code EvaluationService.finalize()}. Un paso que lance una excepción no
 * interrumpe a los demás: {@link PostFinalizationPipeline} la registra y continúa.
 *
 * <p>Agregar un efecto = agregar una clase {@code @Component @Order(n)} que
 * implemente esta interfaz, sin tocar {@code EvaluationService}.
 */
public interface PostFinalizationStep {

    /** Nombre estable para logs (no se muestra al usuario). */
    String name();

    void execute(PostFinalizationContext ctx) throws Exception;
}
