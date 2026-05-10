package com.integrityfamily.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateFamilyGratitudeRequest(
        @NotNull
        Long familyId,

        @NotBlank
        @Size(max = 150)
        String fromMember,

        @NotBlank
        @Size(max = 150)
        String toMember,

        @NotBlank
        @Size(max = 1000)
        String description
) {}
