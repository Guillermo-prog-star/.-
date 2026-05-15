package com.integrityfamily.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
    @NotBlank(message = "El token de refresco es obligatorio")
    String refreshToken
) {}
