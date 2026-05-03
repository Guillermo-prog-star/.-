package com.integrityfamily.lts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSessionRequest(
    @NotNull Long memberId,
    @NotBlank String topic,
    @NotBlank String objective
) {}
