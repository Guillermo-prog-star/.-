package com.integrityfamily.auth.dto;

import java.util.List;

/**
 * Record optimizado para la respuesta de autenticaciÃƒÂ³n.
 */
public record AuthResponse(
    Long id,
    String email,
    String token,
    String fullName,
    List<String> roles
) {}


