package com.integrityfamily.bitacora.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class SprintDtos {

    public record CreateSprintRequest(
            String objective,
            String riskDimension,
            Integer durationDays,
            List<String> missions
    ) {}

    public record CreateDailyCheckinRequest(
            String yesterdayText,
            String todayText,
            String blockagesText,
            String resolutionText,
            String emotionalIndicator,
            String memberName
    ) {}

    public record CloseSprintRequest(
            String whatWentWell,
            String whatWasDifficult,
            String whatLearned,
            String whatToAdjust,
            Integer tensionLevel,
            Integer mindfulCompliance,
            Integer sharedTime,
            Integer positiveInteractions,
            Integer emotionalPersistence
    ) {}

    public record SprintMissionResponse(
            Long id,
            String description,
            String status,
            LocalDateTime completedAt
    ) {}

    public record SprintDailyResponse(
            Long id,
            String memberName,
            LocalDate checkinDate,
            String yesterdayText,
            String todayText,
            String blockagesText,
            String resolutionText,
            String emotionalIndicator,
            LocalDateTime createdAt
    ) {}

    public record SprintRetrospectiveResponse(
            Long id,
            String whatWentWell,
            String whatWasDifficult,
            String whatLearned,
            String whatToAdjust,
            Integer tensionLevel,
            Integer mindfulCompliance,
            Integer sharedTime,
            Integer positiveInteractions,
            Integer emotionalPersistence,
            Integer consistencyScore,
            String aiFeedback,
            LocalDateTime createdAt
    ) {}

    public record SprintResponse(
            Long id,
            Long familyId,
            String objective,
            String riskDimension,
            Integer durationDays,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            List<SprintMissionResponse> missions,
            List<SprintDailyResponse> dailies,
            SprintRetrospectiveResponse retrospective,
            LocalDateTime createdAt
    ) {}
}
