package com.integrityfamily.dto;

import com.integrityfamily.domain.FamilyGratitudeEntry;
import java.time.LocalDateTime;

public record FamilyGratitudeResponse(
        Long id,
        Long familyId,
        String fromMember,
        String toMember,
        String description,
        LocalDateTime createdAt
) {
    public static FamilyGratitudeResponse from(FamilyGratitudeEntry entry) {
        return new FamilyGratitudeResponse(
                entry.getId(),
                entry.getFamily().getId(),
                entry.getFromMember(),
                entry.getToMember(),
                entry.getDescription(),
                entry.getCreatedAt()
        );
    }
}
