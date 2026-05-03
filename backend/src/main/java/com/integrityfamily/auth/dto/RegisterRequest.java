package com.integrityfamily.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 120, message = "El nombre completo no puede superar 120 caracteres")
        String fullName,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato vÃƒÂ¡lido")
        String email,

        @NotBlank(message = "La contraseÃƒÂ±a es obligatoria")
        @Size(min = 6, message = "La contraseÃƒÂ±a debe tener al menos 6 caracteres")
        String password,

        // No es obligatorio, pero si se provee, activa el Onboarding Alfa
        String voucher
) {}


