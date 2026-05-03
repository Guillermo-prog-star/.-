package com.integrityfamily.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato vÃƒÂ¡lido")
    String email,

    @NotBlank(message = "La contraseÃƒÂ±a es obligatoria")
    String password
) {}


