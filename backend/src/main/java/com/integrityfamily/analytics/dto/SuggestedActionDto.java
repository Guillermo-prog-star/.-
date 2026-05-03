package com.integrityfamily.analytics.dto;

import lombok.Builder;

@Builder
public record SuggestedActionDto(
    Long id,
    String description,
    String dimension,
    boolean completed
) {}


