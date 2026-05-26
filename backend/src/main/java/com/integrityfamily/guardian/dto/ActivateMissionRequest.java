package com.integrityfamily.guardian.dto;

import com.integrityfamily.guardian.domain.MissionCategory;

public record ActivateMissionRequest(
    String title,
    String description,
    MissionCategory category,
    Integer durationMinutes,
    Long guardianMemberId
) {}
