package com.integrityfamily.guardian.dto;

import com.integrityfamily.guardian.domain.MissionCategory;
import com.integrityfamily.guardian.domain.MissionStatus;
import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record MissionDto(
    Long id,
    String title,
    String description,
    MissionCategory category,
    Integer durationMinutes,
    MissionStatus status,
    Long createdByMemberId,
    String createdByFullName,
    LocalDateTime activatedAt,
    LocalDateTime completedAt,
    LocalDateTime createdAt
) {}
