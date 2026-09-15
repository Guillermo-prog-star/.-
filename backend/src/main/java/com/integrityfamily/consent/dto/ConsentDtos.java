package com.integrityfamily.consent.dto;

import com.integrityfamily.consent.domain.ConsentPurpose;
import com.integrityfamily.consent.domain.ConsentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class ConsentDtos {

    private ConsentDtos() {}

    public record GrantRequest(
            Long memberId,
            @NotNull(message = "El propósito es obligatorio") ConsentPurpose purpose,
            @NotBlank(message = "El alcance (qué se comparte) es obligatorio") String scope,
            @NotBlank(message = "El receptor del consentimiento es obligatorio") String granteeReference,
            LocalDateTime expiresAt
    ) {}

    public record RevokeRequest(String reason) {}

    public record ConsentResponse(
            Long id,
            Long familyId,
            Long memberId,
            ConsentPurpose purpose,
            String scope,
            String granteeReference,
            ConsentStatus status,
            String grantedByEmail,
            LocalDateTime grantedAt,
            LocalDateTime expiresAt,
            String revokedByEmail,
            LocalDateTime revokedAt,
            String revocationReason,
            boolean active
    ) {}
}
