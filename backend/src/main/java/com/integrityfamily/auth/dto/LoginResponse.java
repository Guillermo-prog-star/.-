package com.integrityfamily.auth.dto;

public record LoginResponse(String token, String refreshToken, long expiresInMs, UserResponse user) {}
