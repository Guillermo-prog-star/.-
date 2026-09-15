package com.integrityfamily.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.integrityfamily.domain.User;
import com.integrityfamily.familyhome.security.FamilyIdentifierBridge;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(Long id, String email, String fullName, String role, Long familyId, String familyName, UUID homeId) {
    /**
     * homeId es el identificador público (UUID) que espera el contrato IFRM-D Family
     * Home (ver AdaptiveHudController, FamilyHomeController) — distinto del familyId
     * (Long) interno. Sin él, el frontend no tiene forma de armar esas URLs tras el
     * login (solo lo devolvía FamilyResponse en endpoints como /families/mine).
     */
    public static UserResponse from(User u, FamilyIdentifierBridge idBridge) {
        return new UserResponse(
                u.getId(),
                u.getEmail(),
                u.getFullName(),
                u.getRole(),
                u.getFamily() != null ? u.getFamily().getId() : null,
                u.getFamily() != null ? u.getFamily().getName() : null,
                u.getFamily() != null ? idBridge.toFamilyUuid(u.getFamily().getId()) : null
        );
    }
}


