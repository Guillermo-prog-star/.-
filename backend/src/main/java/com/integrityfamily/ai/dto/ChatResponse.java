package com.integrityfamily.ai.dto;

public record ChatResponse(
        String reply,
        String familyCode,
        String currentMilestone
) {}


