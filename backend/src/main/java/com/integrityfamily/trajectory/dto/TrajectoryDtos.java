package com.integrityfamily.trajectory.dto;

import com.integrityfamily.domain.RiskMacrodomain;
import com.integrityfamily.domain.TrajectoryStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class TrajectoryDtos {

    public record TrajectoryBankItem(
        Long id,
        String code,
        String name,
        RiskMacrodomain macrodomain,
        String description,
        String earlySignals,
        String potentialEvolution,
        String severityDefault
    ) {}

    public record FamilyTrajectoryDto(
        Long id,
        TrajectoryBankItem trajectory,
        TrajectoryStatus status,
        LocalDateTime detectedAt,
        LocalDateTime resolvedAt,
        String notes,
        String assignedBy
    ) {}

    public record TrajectoryTimelineDto(
        Long id,
        LocalDate eventDate,
        Integer ageAtEvent,
        String eventDescription,
        String riskLevel,
        String actionTaken,
        String result,
        LocalDateTime recordedAt
    ) {}

    public record TrajectoryImpactDto(
        Long id,
        String indicatorName,
        String indicatorKey,
        BigDecimal baselineValue,
        BigDecimal currentValue,
        String unit,
        Boolean higherIsBetter,
        Double improvementPct
    ) {}

    // ─── Request bodies ───────────────────────────────────────────────────────

    public record AssignTrajectoryRequest(String code, String notes) {}

    public record UpdateStatusRequest(String status, String notes) {}

    public record TimelineEventRequest(
        LocalDate eventDate,
        Integer ageAtEvent,
        String eventDescription,
        String riskLevel,
        String actionTaken,
        String result
    ) {}

    public record IndicatorRequest(
        String indicatorName,
        String indicatorKey,
        BigDecimal baselineValue,
        BigDecimal currentValue,
        String unit,
        Boolean higherIsBetter,
        String notes
    ) {}

    public record TrajectoryBankResponse(
        java.util.Map<String, List<TrajectoryBankItem>> byMacrodomain,
        int totalTrajectories
    ) {}
}
