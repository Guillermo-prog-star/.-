package com.integrityfamily.lineage.dto;

import jakarta.validation.constraints.NotNull;

public record GenerationInfoRequest(
        @NotNull Integer generationLevel,
        String generationType,
        String title,
        String summary,
        String context,
        String keyChallenge,
        String keyAchievement,
        String periodStart,
        String periodEnd
) {}
