package com.integrityfamily.FamilyMember.dto;
public record MemberResponse(
    Long id, Long familyId, String familyName, String fullName,
    String roleType, Integer age, Integer autonomyLevel,
    Integer responsibilityLevel, Boolean active) {}


