package com.integrityfamily.interop.canonical;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

/**
 * Equivalente canónico de un miembro de familia (mapea desde
 * {@code com.integrityfamily.domain.FamilyMember}). Traduce eventualmente a
 * FHIR Patient o RelatedPerson según el rol.
 */
@Builder
public record Person(
        String canonicalId,
        List<CanonicalIdentifier> identifiers,
        String fullName,
        String givenName,
        LocalDate birthDate,
        String familyRole,
        String contactEmail,
        String contactPhone,
        boolean active
) {}
