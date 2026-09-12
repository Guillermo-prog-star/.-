package com.integrityfamily.interop.canonical;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Consentimiento formal para compartir información (distinto del flag simple
 * {@code consentedByEmail}/{@code consentedAt} usado hoy en {@code ecosystem}
 * y {@code support} — esta es la versión ampliada, con propósito, alcance y
 * revocación, que exige la Fase 2 del programa de interoperabilidad). Traduce
 * eventualmente a FHIR Consent.
 *
 * @param status GRANTED, REVOKED, PENDING, NOT_REQUIRED — mismos estados que
 *               {@code dto.home.ConsentStatus}, para no introducir un segundo
 *               vocabulario paralelo
 */
@Builder
public record Consent(
        String canonicalId,
        String subjectId,
        String grantedToId,
        String purpose,
        String scope,
        String status,
        LocalDateTime grantedAt,
        LocalDateTime expiresAt,
        LocalDateTime revokedAt
) {}
