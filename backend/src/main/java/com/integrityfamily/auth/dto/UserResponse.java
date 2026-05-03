package com.integrityfamily.auth.dto;

import com.integrityfamily.domain.User;

public record UserResponse(Long id, String email, String fullName, String role, Long familyId) {
    public static UserResponse from(User u) {
        return new UserResponse(
                u.getId(),
                u.getEmail(),
                u.getFullName(),
                u.getRole(),
                u.getFamily() != null ? u.getFamily().getId() : null
        );
    }
}


