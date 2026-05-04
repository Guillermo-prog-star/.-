package com.integrityfamily.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolveFamilyLogbookEntryRequest(

        @NotBlank
        @Size(max = 1000)
        String progressEvidence,

        @Size(max = 120)
        String resolvedBy
) {}
