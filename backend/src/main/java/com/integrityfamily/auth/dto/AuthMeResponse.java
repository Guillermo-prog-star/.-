package com.integrityfamily.auth.dto;

public record AuthMeResponse(
        Long id,
        String email,
        String fullName,
        String role
) {}


