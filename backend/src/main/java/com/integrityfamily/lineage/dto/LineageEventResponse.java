package com.integrityfamily.lineage.dto;

public record LineageEventResponse(
        Long id,
        String eventYear,
        String title,
        String description,
        String eventType,
        Boolean isApproximate,
        Integer sortOrder
) {}
