package com.integrityfamily.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.integrityfamily.domain.User;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(Long id, String email, String fullName, String role, Long familyId, String familyName) {
    public static UserResponse from(User u) {
        return new UserResponse(
                u.getId(),
                u.getEmail(),
                u.getFullName(),
                u.getRole(),
                u.getFamily() != null ? u.getFamily().getId() : null,
                u.getFamily() != null ? u.getFamily().getName() : null
        );
    }
}


