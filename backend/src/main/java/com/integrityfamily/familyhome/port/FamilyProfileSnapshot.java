package com.integrityfamily.familyhome.port;

import java.util.UUID;

public record FamilyProfileSnapshot(
    UUID id,
    String displayName,
    UUID featuredImageId,
    String featuredImageUrl
) {
}
