package com.integrityfamily.admin.dto;

import java.time.LocalDateTime;

/**
 * SDD-SENTINEL-DASHBOARD-01: Administrative DTO for crisis monitoring.
 */
public record SentinelAlertDto(
    Long familyId,
    String familyName,
    String familyCode,
    Double currentIcf,
    String riskLevel,
    LocalDateTime activatedAt,
    String lastMilestone
) {}


