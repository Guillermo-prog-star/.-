package com.integrityfamily.lineage.dto;

public record GenerationInfoResponse(
        Long id,
        Integer generationLevel,
        String generationType,
        String title,
        String summary,
        String context,
        String keyChallenge,
        String keyAchievement,
        String periodStart,
        String periodEnd
) {}
