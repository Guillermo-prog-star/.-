package com.integrityfamily.analytics.dto;

import lombok.Builder;
import java.time.LocalDate;
import java.util.List;

public class ConvivenceAnalyticsDto {

    @Builder
    public record OperativeDashboardResponse(
        Long familyId,
        Double convivenceIndex,
        String convivenceStatus, // Saludable, Estable, Vulnerable, Crítico
        String currentRiskLevel,
        String trendState, // Mejorando, Estable, Deterioro
        Double adherenceRate, // ej. 78.5%
        String adherenceStatus, // Alta, Media, Baja
        Double complianceRate, // ej. 50.0%
        Double emotionsScore,
        Double communicationScore,
        Integer rachaActivaDias,
        List<OperativeAlertDto> activeAlerts,
        List<MetricsTimelineDto> timeline,
        List<MemberParticipationDto> participation
    ) {}

    @Builder
    public record OperativeAlertDto(
        String alertCode, // ALERTA_CRITICAL_COMMUNICATION, ALERTA_INACTIVITY, ALERTA_LOW_ADHERENCE, ALERTA_REGRESSION
        String severity,
        String message,
        String timestamp
    ) {}

    @Builder
    public record MetricsTimelineDto(
        LocalDate date,
        Double convivenceIndex,
        Double riskScore,
        Double adherenceRate
    ) {}

    @Builder
    public record MemberParticipationDto(
        Long memberId,
        String memberName,
        String role,
        Integer activitiesParticipated,
        Double participationPercentage
    ) {}
}
