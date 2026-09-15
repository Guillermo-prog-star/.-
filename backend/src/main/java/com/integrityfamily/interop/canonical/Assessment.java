package com.integrityfamily.interop.canonical;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Equivalente canónico de una evaluación familiar (mapea desde
 * {@code com.integrityfamily.domain.Evaluation}). Traduce eventualmente a
 * FHIR Questionnaire/QuestionnaireResponse.
 */
@Builder
public record Assessment(
        String canonicalId,
        String subjectId,
        String status,
        LocalDateTime startedAt,
        LocalDateTime finalizedAt,
        List<Observation> observations,
        String overallRiskLevel,
        String criticalDimension
) {}
