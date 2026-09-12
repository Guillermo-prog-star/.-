package com.integrityfamily.dto.home;

import java.time.Instant;
import java.util.UUID;

public record MediaAsset(
    UUID assetId,
    String url,
    String altText,
    UUID ownerId,
    MediaProcessingStatus processedStatus,
    ConsentStatus consentStatus,
    MediaVisibility visibility,
    Instant expiresAt
) {
    public MediaAsset {
        if (assetId == null) {
            throw new IllegalArgumentException("assetId is required");
        }
        if (processedStatus == null) {
            throw new IllegalArgumentException("processedStatus is required");
        }
        if (consentStatus == null) {
            throw new IllegalArgumentException("consentStatus is required");
        }
        if (visibility == null) {
            throw new IllegalArgumentException("visibility is required");
        }
        
        if (consentStatus == ConsentStatus.REVOKED || consentStatus == ConsentStatus.PENDING) {
            if (url != null) {
                throw new IllegalArgumentException("url must be null when consent is revoked or pending");
            }
        } else {
            if (url == null) {
                throw new IllegalArgumentException("url is required when consent is granted");
            }
        }
    }
}
