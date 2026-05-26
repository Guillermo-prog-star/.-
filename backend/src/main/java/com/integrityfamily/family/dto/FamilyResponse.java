package com.integrityfamily.family.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record FamilyResponse(
    Long id,
    String name,
    String description,
    String familyCode,
    String currentMilestone,
    String municipio,
    String whatsapp,
    Boolean sentinelActive,
    List<FamilyMemberResponse> members,
    // Guardián Familiar
    Long guardianMemberId,
    String guardianFullName,
    LocalDateTime guardianSince,
    Integer participationScore
) {}
