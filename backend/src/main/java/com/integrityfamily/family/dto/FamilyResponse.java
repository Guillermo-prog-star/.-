package com.integrityfamily.family.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
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
    Integer participationScore,
    /** Identificador público (UUID) de la familia para el contrato IFRM-D Family Home. */
    UUID homeId
) {}
