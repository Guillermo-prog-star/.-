package com.integrityfamily.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TerritorialEvolutionReportDto(
    String familyCode,
    Long familyId,
    String referenceName,
    
    // Territorio
    String countryCode,
    String departmentCode,
    String district,
    String municipality,
    String commune,
    String neighborhood,
    Double lat,
    Double lng,
    
    // Hitos
    List<MilestoneReportDto> milestones,
    
    // Resumen
    String overallTrend,
    String riskChange,
    List<String> top2RecurringCriticalDimensions,
    String notes
) {
    public record MilestoneReportDto(
        String hitoKey,
        Integer month,
        LocalDateTime evaluatedAt,
        Double icfPercent,
        String riskLevel,
        DimensionScoresDto scores,
        String criticalDimension,
        Double deltaIcfVsPrev,
        ChecklistSummaryDto checklist
    ) {}

    public record DimensionScoresDto(
        Double emotions,
        Double communication,
        Double habits,
        Double time
    ) {}

    public record ChecklistSummaryDto(
        Integer totalItems,
        Integer doneItems,
        Integer lateItems,
        Double completionPercent,
        String trendVsPrev
    ) {}
}
