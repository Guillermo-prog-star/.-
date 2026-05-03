package com.integrityfamily.lts.dto;

import jakarta.validation.constraints.NotBlank;

public record AddAttemptRequest(
    @NotBlank String content
) {}
