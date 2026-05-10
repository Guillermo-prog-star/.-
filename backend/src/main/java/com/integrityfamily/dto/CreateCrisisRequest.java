package com.integrityfamily.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCrisisRequest(
        @NotNull
        Long familyId,

        @NotBlank
        String category,

        String emotion,

        @NotBlank
        String description
) {}
