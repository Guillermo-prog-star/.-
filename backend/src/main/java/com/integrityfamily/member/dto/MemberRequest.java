package com.integrityfamily.member.dto;

import jakarta.validation.constraints.*;

/**
 * MemberRequest: DTO para crear un miembro desde el frontend.
 * El familyId va en el path URL, no en el body.
 * Los campos numéricos son opcionales para no fallar con valores vacíos.
 */
public record MemberRequest(
    @NotBlank @Size(max=120) String fullName,
    @NotBlank @Size(max=50)  String roleType,
    Integer age,
    Integer autonomyLevel,
    Integer responsibilityLevel
) {}
