package com.integrityfamily.member.dto;

import jakarta.validation.constraints.*;

/**
 * SDD: Record Universal de Miembros.
 */
public record MemberRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 120) String fullName,
        @NotBlank(message = "El rol es obligatorio") @Size(max = 50) String roleType,
        Integer age,
        Integer autonomyLevel,
        Integer responsibilityLevel,
        String email,
        String phone,
        Long familyId) {
}
