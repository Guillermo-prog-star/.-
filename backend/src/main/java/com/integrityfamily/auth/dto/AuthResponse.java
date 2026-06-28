package com.integrityfamily.auth.dto;

import java.util.List;

/**
 * Record optimizado para la respuesta de autenticación.
 */
public record AuthResponse(
    Long id,
    String email,
    String token,
    String fullName,
    List<String> roles
) {}


