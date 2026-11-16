package com.integrityfamily.interop.fhir;

import org.hl7.fhir.r4.model.Reference;

/**
 * Traduce un {@code subjectId} del modelo canónico ("family-1", "member-10")
 * a una referencia FHIR ("Group/family-1", "Patient/member-10"). Centralizado
 * acá porque tanto Observation como cualquier recurso futuro necesitan la
 * misma regla, y el prefijo del canonicalId es el único lugar donde vive esa
 * información.
 */
public final class FhirReferences {

    private FhirReferences() {}

    public static Reference forSubject(String canonicalSubjectId) {
        if (canonicalSubjectId == null) return null;
        if (canonicalSubjectId.startsWith("family-")) {
            return new Reference("Group/" + canonicalSubjectId);
        }
        if (canonicalSubjectId.startsWith("member-")) {
            return new Reference("Patient/" + canonicalSubjectId);
        }
        throw new IllegalArgumentException("subjectId con prefijo desconocido: " + canonicalSubjectId);
    }
}
