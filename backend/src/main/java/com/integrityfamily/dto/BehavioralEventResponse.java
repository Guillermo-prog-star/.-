package com.integrityfamily.dto;

import com.integrityfamily.domain.FamilyBehavioralEvent;
import java.time.LocalDateTime;

public record BehavioralEventResponse(
        Long id,
        Long familyId,
        String description,
        Integer severity,
        LocalDateTime occurredAt,
        LocalDateTime repairedAt,
        String repairDescription,
        LocalDateTime createdAt
) {
    public static BehavioralEventResponse from(FamilyBehavioralEvent event) {
        return new BehavioralEventResponse(
                event.getId(),
                event.getFamily().getId(),
                event.getDescription(),
                event.getSeverity(),
                event.getOccurredAt(),
                event.getRepairedAt(),
                event.getRepairDescription(),
                event.getCreatedAt()
        );
    }
}
