package com.integrityfamily.dto;

import com.integrityfamily.domain.LogbookStatus;
import java.time.LocalDateTime;

public record FamilyLogbookEntryResponse(
        Long id,
        Long familyId,
        String situation,
        String difficultyDetected,
        String emotionIdentified,
        String understanding,
        String correctionAction,
        String familyAgreement,
        String progressEvidence,
        LogbookStatus status,
        String createdBy,
        String resolvedBy,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt
) {}
