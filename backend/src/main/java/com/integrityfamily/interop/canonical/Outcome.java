package com.integrityfamily.interop.canonical;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Un resultado observado tras una {@link Intervention}. No tiene un recurso
 * FHIR dedicado propio — normalmente se traduce como una {@code Observation}
 * enlazada al CarePlan/Goal correspondiente.
 */
@Builder
public record Outcome(
        String canonicalId,
        String subjectId,
        String description,
        Double resultValue,
        LocalDateTime recordedAt
) {}
