package com.integrityfamily.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateFamilyLogbookEntryRequest(

        @NotNull
        Long familyId,

        @NotBlank
        @Size(max = 1000)
        String situation,

        @NotBlank
        @Size(max = 1000)
        String difficultyDetected,

        @NotBlank
        @Size(max = 255)
        String emotionIdentified,

        @NotBlank
        @Size(max = 1000)
        String understanding,

        @NotBlank
        @Size(max = 1000)
        String correctionAction,

        @NotBlank
        @Size(max = 1000)
        String familyAgreement,

        @Size(max = 120)
        String createdBy
) {}
