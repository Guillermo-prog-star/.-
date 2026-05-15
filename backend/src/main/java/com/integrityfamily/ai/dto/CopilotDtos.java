package com.integrityfamily.ai.dto;

import lombok.Builder;
import java.util.List;

public class CopilotDtos {

    @Builder
    public record CompactFamilyContext(
        Long familyId,
        String riskLevel,
        String criticalDimension,
        String trend,
        Double adherence,
        Integer inactiveDays,
        List<String> recentLearnings,
        List<String> alerts
    ) {}

    @Builder
    public record StructuredAiInferenceResponse(
        String summary,
        String priority, // HIGH, MEDIUM, LOW
        List<String> recommendedActions,
        String containmentSuggestion,
        Integer followUpDays
    ) {}

    public record CopilotInferRequest(
        Long familyId,
        String triggerEvent // NEW_ALERT, REGRESSION, INACTIVITY
    ) {}
}
