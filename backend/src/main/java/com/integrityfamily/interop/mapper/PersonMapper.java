package com.integrityfamily.interop.mapper;

import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.interop.canonical.CanonicalIdentifier;
import com.integrityfamily.interop.canonical.Person;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code FamilyMember} → {@link Person}. El {@code canonicalId} usa un
 * esquema legible ({@code member-<id>}) — no es un identificador FHIR real,
 * eso se decide en la Fase 4 (el adapter FHIR puede necesitar UUIDs o IDs
 * que el propio servidor FHIR asigna).
 */
public final class PersonMapper {

    private PersonMapper() {}

    public static Person toCanonical(FamilyMember member) {
        List<CanonicalIdentifier> identifiers = new ArrayList<>();
        if (member.getDocumentNumber() != null && !member.getDocumentNumber().isBlank()) {
            String system = member.getDocumentType() != null ? member.getDocumentType() : "UNKNOWN";
            identifiers.add(CanonicalIdentifier.builder()
                    .system(system)
                    .value(member.getDocumentNumber())
                    .build());
        }

        return Person.builder()
                .canonicalId("member-" + member.getId())
                .identifiers(identifiers)
                .fullName(member.getFullName())
                .givenName(member.getFirstName())
                .birthDate(member.getBirthDate())
                .familyRole(member.getRole())
                .contactEmail(member.getEmail())
                .contactPhone(member.getPhone())
                .active(member.isActive())
                .build();
    }
}
