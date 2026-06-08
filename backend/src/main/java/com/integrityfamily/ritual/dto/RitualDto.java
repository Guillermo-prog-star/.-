package com.integrityfamily.ritual.dto;

import com.integrityfamily.ritual.domain.RitualStatus;
import com.integrityfamily.ritual.domain.RitualType;

import java.time.LocalDateTime;
import java.util.List;

public record RitualDto(
    Long id,
    Long familyId,
    RitualType ritualType,
    RitualStatus status,
    String title,
    String description,
    List<String> guidedSteps,
    String triggerContext,
    LocalDateTime triggeredAt,
    LocalDateTime completedAt
) {}
