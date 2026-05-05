package com.integrityfamily.family.dto;

import lombok.Builder;

@Builder
public record FamilyMemberResponse(
    Long id,
    String fullName,
    String email,
    String role,
    Integer age,
    boolean active,
    Integer autonomyLevel,
    Integer responsibilityLevel
) {}
