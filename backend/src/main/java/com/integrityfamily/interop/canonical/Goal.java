package com.integrityfamily.interop.canonical;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Una meta dentro de una {@link Intervention}. Traduce eventualmente a FHIR Goal.
 */
@Builder
public record Goal(
        String canonicalId,
        String description,
        String status,
        LocalDateTime targetDate
) {}
