package com.integrityfamily.lineage.dto;

public record LineageEventRequest(
        String eventYear,
        String title,
        String description,
        String eventType,
        Boolean isApproximate,
        Integer sortOrder
) {}
