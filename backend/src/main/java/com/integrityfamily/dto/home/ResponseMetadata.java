package com.integrityfamily.dto.home;

import java.time.Instant;

public record ResponseMetadata(
    Instant generatedAt,
    Instant expiresAt,
    String projectionVersion,
    DataStatus dataStatus,
    String correlationId
) {
    public ResponseMetadata {
        if (generatedAt == null) {
            throw new IllegalArgumentException("generatedAt is required");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt is required");
        }
        if (projectionVersion == null) {
            throw new IllegalArgumentException("projectionVersion is required");
        }
        if (dataStatus == null) {
            throw new IllegalArgumentException("dataStatus is required");
        }
    }
}
