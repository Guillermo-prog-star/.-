package com.integrityfamily.interop.mapper;

import com.integrityfamily.interop.canonical.Consent;

/**
 * {@code consent.domain.Consent} (Fase 2) → {@code interop.canonical.Consent}
 * (Fase 1). Dos clases distintas con el mismo nombre a propósito: una es la
 * entidad persistida real, la otra la forma canónica intermedia hacia FHIR.
 */
public final class ConsentMapper {

    private ConsentMapper() {}

    public static Consent toCanonical(com.integrityfamily.consent.domain.Consent consent) {
        return Consent.builder()
                .canonicalId("consent-" + consent.getId())
                .subjectId(consent.getMemberId() != null
                        ? "member-" + consent.getMemberId()
                        : "family-" + consent.getFamilyId())
                .grantedToId(consent.getGranteeReference())
                .purpose(consent.getPurpose() != null ? consent.getPurpose().name() : null)
                .scope(consent.getScope())
                .status(consent.getStatus() != null ? consent.getStatus().name() : null)
                .grantedAt(consent.getGrantedAt())
                .expiresAt(consent.getExpiresAt())
                .revokedAt(consent.getRevokedAt())
                .build();
    }
}
