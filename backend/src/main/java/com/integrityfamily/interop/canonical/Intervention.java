package com.integrityfamily.interop.canonical;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Equivalente canónico de un plan de mejora / sprint familiar (mapea desde
 * {@code plan}/{@code bitacora} modules). Traduce eventualmente a FHIR CarePlan.
 */
@Builder
public record Intervention(
        String canonicalId,
        String subjectId,
        String title,
        String description,
        String status,
        LocalDateTime startedAt,
        LocalDateTime targetAt,
        List<Goal> goals
) {}
