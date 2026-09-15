package com.integrityfamily.interop.fhir.mapper;

import com.integrityfamily.interop.canonical.CanonicalIdentifier;
import com.integrityfamily.interop.canonical.Person;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PatientFhirMapper")
class PatientFhirMapperTest {

    @Test
    @DisplayName("mapea identificadores, nombre, nacimiento, contacto y rol como extension")
    void shouldMapFullPerson() {
        Person person = Person.builder()
                .canonicalId("member-10")
                .identifiers(List.of(CanonicalIdentifier.builder().system("CC").value("1020304050").build()))
                .fullName("Ana Martínez")
                .givenName("Ana")
                .birthDate(LocalDate.of(1985, 5, 20))
                .familyRole("MADRE")
                .contactEmail("ana@test.com")
                .contactPhone("3001234567")
                .active(true)
                .build();

        Patient patient = PatientFhirMapper.toFhir(person);

        assertThat(patient.getId()).isEqualTo("member-10");
        assertThat(patient.getActive()).isTrue();
        assertThat(patient.getIdentifier()).hasSize(1);
        assertThat(patient.getIdentifier().get(0).getValue()).isEqualTo("1020304050");
        assertThat(patient.getIdentifier().get(0).getSystem()).contains("cc");
        assertThat(patient.getNameFirstRep().getText()).isEqualTo("Ana Martínez");
        assertThat(patient.getBirthDate()).isNotNull();

        boolean hasEmail = patient.getTelecom().stream()
                .anyMatch(t -> t.getSystem() == ContactPoint.ContactPointSystem.EMAIL && t.getValue().equals("ana@test.com"));
        assertThat(hasEmail).isTrue();

        assertThat(patient.getExtension()).hasSize(1);
        assertThat(patient.getExtension().get(0).getValue().primitiveValue()).isEqualTo("MADRE");
    }

    @Test
    @DisplayName("sin documento, sin contacto → Patient válido sin identifiers ni telecom")
    void shouldMapMinimalPerson() {
        Person person = Person.builder()
                .canonicalId("member-11")
                .identifiers(List.of())
                .fullName("Luisa")
                .active(true)
                .build();

        Patient patient = PatientFhirMapper.toFhir(person);

        assertThat(patient.getIdentifier()).isEmpty();
        assertThat(patient.getTelecom()).isEmpty();
        assertThat(patient.getExtension()).isEmpty();
        assertThat(patient.getBirthDate()).isNull();
    }
}
