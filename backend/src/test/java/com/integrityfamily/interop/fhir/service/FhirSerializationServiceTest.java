package com.integrityfamily.interop.fhir.service;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FhirSerializationService")
class FhirSerializationServiceTest {

    private final FhirSerializationService service = new FhirSerializationService(FhirContext.forR4());

    @Test
    @DisplayName("serializa un recurso a JSON FHIR válido")
    void shouldSerializeToFhirJson() {
        Patient patient = new Patient();
        patient.setId("member-10");
        patient.setActive(true);

        String json = service.toJson(patient);

        assertThat(json).contains("\"resourceType\": \"Patient\"");
        assertThat(json).contains("\"id\": \"member-10\"");
    }
}
