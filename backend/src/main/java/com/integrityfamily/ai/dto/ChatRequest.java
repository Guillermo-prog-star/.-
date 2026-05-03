package com.integrityfamily.ai.dto;
import jakarta.validation.constraints.*;
public record ChatRequest(@NotNull Long familyId, @NotBlank String message) {}


