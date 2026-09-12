package com.integrityfamily.interop.fhir.mapper;

import com.integrityfamily.interop.canonical.CanonicalIdentifier;
import com.integrityfamily.interop.canonical.Person;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;

import java.time.ZoneId;
import java.util.Date;

/**
 * {@link Person} → FHIR {@code Patient} (recurso piloto de la Fase 4).
 *
 * El sistema de los {@code Identifier} usa un namespace propio de Integrity
 * Family a falta de un OID/URI oficial publicado por el Ministerio para cada
 * tipo de documento (CC, TI, RC...) — cuando exista, se reemplaza acá sin
 * tocar el resto del pipeline.
 */
public final class PatientFhirMapper {

    private static final String IDENTIFIER_SYSTEM_PREFIX = "https://integrityfamily.com/fhir/identifier/";
    private static final String FAMILY_ROLE_EXTENSION_URL = "https://integrityfamily.com/fhir/StructureDefinition/family-role";

    private PatientFhirMapper() {}

    public static Patient toFhir(Person person) {
        Patient patient = new Patient();
        patient.setId(person.canonicalId());
        patient.setActive(person.active());

        for (CanonicalIdentifier id : person.identifiers()) {
            patient.addIdentifier(new Identifier()
                    .setSystem(IDENTIFIER_SYSTEM_PREFIX + id.system().toLowerCase())
                    .setValue(id.value()));
        }

        HumanName name = new HumanName().setText(person.fullName());
        if (person.givenName() != null && !person.givenName().isBlank()) {
            name.addGiven(person.givenName());
        }
        patient.addName(name);

        if (person.birthDate() != null) {
            patient.setBirthDate(Date.from(person.birthDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }

        if (person.contactEmail() != null && !person.contactEmail().isBlank()) {
            patient.addTelecom(new ContactPoint()
                    .setSystem(ContactPoint.ContactPointSystem.EMAIL)
                    .setValue(person.contactEmail()));
        }
        if (person.contactPhone() != null && !person.contactPhone().isBlank()) {
            patient.addTelecom(new ContactPoint()
                    .setSystem(ContactPoint.ContactPointSystem.PHONE)
                    .setValue(person.contactPhone()));
        }

        if (person.familyRole() != null && !person.familyRole().isBlank()) {
            patient.addExtension(FAMILY_ROLE_EXTENSION_URL, new org.hl7.fhir.r4.model.StringType(person.familyRole()));
        }

        return patient;
    }
}
