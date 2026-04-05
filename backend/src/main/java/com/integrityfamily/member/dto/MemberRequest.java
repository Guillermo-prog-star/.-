package com.integrityfamily.member.dto;
import jakarta.validation.constraints.*;
public record MemberRequest(
    @NotNull Long familyId,
    @NotBlank @Size(max=120) String fullName,
    @NotBlank @Size(max=50) String roleType,
    @Min(0) @Max(120) Integer age,
    @Min(0) @Max(100) Integer autonomyLevel,
    @Min(0) @Max(100) Integer responsibilityLevel) {}
