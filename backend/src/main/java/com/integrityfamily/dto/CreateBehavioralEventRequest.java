package com.integrityfamily.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record CreateBehavioralEventRequest(
        @NotNull
        Long familyId,

        @NotBlank
        @Size(max = 1000)
        String description,

        @NotNull
        @Min(1)
        @Max(5)
        Integer severity,

        @NotNull
        LocalDateTime occurredAt
) {}
