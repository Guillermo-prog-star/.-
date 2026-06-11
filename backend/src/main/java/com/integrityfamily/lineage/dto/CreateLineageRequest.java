package com.integrityfamily.lineage.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateLineageRequest(
        @NotBlank String title,
        String description,
        Integer anchorGeneration,
        Integer maxPastGen,
        Integer maxFutureGen,
        String visionStatement,
        String foundingYear
) {}
