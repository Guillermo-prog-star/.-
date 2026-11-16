package com.integrityfamily.familyhome.application;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public record ProjectionRequestContext(
    Locale locale,
    ProjectionChannel channel,
    UUID correlationId,
    Instant requestedAt,
    String requestedContractVersion
) {
    public ProjectionRequestContext {
        if (locale == null) {
            throw new IllegalArgumentException("locale is required");
        }
        if (channel == null) {
            throw new IllegalArgumentException("channel is required");
        }
        if (correlationId == null) {
            throw new IllegalArgumentException("correlationId is required");
        }
        if (requestedAt == null) {
            throw new IllegalArgumentException("requestedAt is required");
        }
        if (requestedContractVersion == null) {
            throw new IllegalArgumentException("requestedContractVersion is required");
        }
    }
}
