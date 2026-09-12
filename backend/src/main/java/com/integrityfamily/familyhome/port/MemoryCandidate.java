package com.integrityfamily.familyhome.port;

import java.time.Instant;
import java.util.UUID;

public record MemoryCandidate(
    UUID memoryId,
    String title,
    Instant date,
    String assetUrl
) {
}
