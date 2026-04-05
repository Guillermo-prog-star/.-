package com.integrityfamily.auth.dto;

import java.util.List;

public record LoginResponse(
        Long id,
        String fullName,
        String email,
        String token,
        List<String> roles
) {}
