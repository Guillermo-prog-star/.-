package com.integrityfamily.lineage.dto;

import jakarta.validation.constraints.NotBlank;

public record LineageEventRequest(
        String eventYear,
        @NotBlank String title,
        String description,
        String eventType,
        Boolean isApproximate,
        Integer sortOrder
) {}
