package com.integrityfamily.dto.home;

import java.time.Instant;

public record DimensionBlock(
    DimensionStatus status,
    String labelKey,
    Instant updatedAt,
    boolean detailsAvailable
) {
    public DimensionBlock {
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (labelKey == null) {
            throw new IllegalArgumentException("labelKey is required");
        }
        if (updatedAt == null) {
            throw new IllegalArgumentException("updatedAt is required");
        }
    }
}
