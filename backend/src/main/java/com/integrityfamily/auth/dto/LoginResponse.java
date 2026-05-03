package com.integrityfamily.auth.dto;


public record LoginResponse(String token, long expiresInMs, UserResponse user) {}


