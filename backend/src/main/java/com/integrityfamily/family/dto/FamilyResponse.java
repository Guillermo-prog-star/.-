package com.integrityfamily.family.dto;
public record FamilyResponse(
    Long id, String name, String description, String familyCode,
    String currentMilestone, String municipio, String whatsapp,
    Long createdByUserId, String createdByName) {}
