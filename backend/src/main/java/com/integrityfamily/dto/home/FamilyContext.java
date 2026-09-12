package com.integrityfamily.dto.home;

import java.util.UUID;

public record FamilyContext(
    UUID id,
    String displayName,
    MediaAsset featuredImage
) {
    public FamilyContext {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        if (displayName == null) {
            throw new IllegalArgumentException("displayName is required");
        }
    }
}
