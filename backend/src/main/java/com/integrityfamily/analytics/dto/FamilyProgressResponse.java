package com.integrityfamily.analytics.dto;

import lombok.Builder;
import java.util.Map;

@Builder
public record FamilyProgressResponse(
        Long familyId,
        Long currentEvaluationId,
        Long previousEvaluationId,
        String milestoneCode,
        Double previousIcf,
        Double currentIcf,
        Double deltaIcf,
        String classification,
        String interpretation,
        Map<String, Double> dimensionEvolution,
        String recommendedAction
) {}
