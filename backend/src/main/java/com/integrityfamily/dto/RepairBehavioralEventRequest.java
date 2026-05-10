package com.integrityfamily.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record RepairBehavioralEventRequest(
        @NotBlank
        @Size(max = 1000)
        String repairDescription,

        @NotNull
        LocalDateTime repairedAt
) {}
