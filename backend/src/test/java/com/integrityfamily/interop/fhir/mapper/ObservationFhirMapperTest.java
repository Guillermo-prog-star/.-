package com.integrityfamily.interop.fhir.mapper;

import com.integrityfamily.interop.canonical.Observation;
import com.integrityfamily.interop.terminology.ConceptMapping;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ObservationFhirMapper")
class ObservationFhirMapperTest {

    @Test
    @DisplayName("subjectId con prefijo family- → referencia a Group")
    void shouldReferenceGroup_forFamilySubject() {
        Observation obs = Observation.builder()
                .canonicalId("evaluation-100-icf")
                .subjectId("family-1")
                .code("ICF").display("Índice de Cohesión Familiar")
                .valueNumeric(72.5)
                .effectiveAt(LocalDateTime.of(2026, 7, 1, 10, 0))
                .status("FINAL")
                .build();

        var fhirObs = ObservationFhirMapper.toFhir(obs, null);

        assertThat(fhirObs.getId()).isEqualTo("evaluation-100-icf");
        assertThat(fhirObs.getSubject().getReference()).isEqualTo("Group/family-1");
        assertThat(fhirObs.getStatus()).isEqualTo(org.hl7.fhir.r4.model.Observation.ObservationStatus.FINAL);
        assertThat(fhirObs.getValueQuantity().getValue().doubleValue()).isEqualTo(72.5);
        assertThat(fhirObs.getCode().getText()).isEqualTo("Índice de Cohesión Familiar");
    }

    @Test
    @DisplayName("subjectId con prefijo member- → referencia a Patient")
    void shouldReferencePatient_forMemberSubject() {
        Observation obs = Observation.builder()
                .canonicalId("obs-1").subjectId("member-5")
                .code("X").display("X").status("PRELIMINARY").build();

        var fhirObs = ObservationFhirMapper.toFhir(obs, null);

        assertThat(fhirObs.getSubject().getReference()).isEqualTo("Patient/member-5");
        assertThat(fhirObs.getStatus()).isEqualTo(org.hl7.fhir.r4.model.Observation.ObservationStatus.PRELIMINARY);
    }

    @Test
    @DisplayName("valueText en vez de valueNumeric → se mapea como StringType")
    void shouldMapTextValue() {
        Observation obs = Observation.builder()
                .canonicalId("obs-2").subjectId("family-1")
                .code("NOTE").display("Nota").valueText("texto libre")
                .status("FINAL").build();

        var fhirObs = ObservationFhirMapper.toFhir(obs, null);

        assertThat(fhirObs.getValueStringType().getValue()).isEqualTo("texto libre");
    }

    @Test
    @DisplayName("status desconocido/null → UNKNOWN, no lanza excepción")
    void shouldDefaultToUnknownStatus() {
        Observation obs = Observation.builder()
                .canonicalId("obs-3").subjectId("family-1")
                .code("X").display("X").status(null).build();

        var fhirObs = ObservationFhirMapper.toFhir(obs, null);

        assertThat(fhirObs.getStatus()).isEqualTo(org.hl7.fhir.r4.model.Observation.ObservationStatus.UNKNOWN);
    }

    @Test
    @DisplayName("con ConceptMapping → agrega una segunda Coding sin reemplazar la de Integrity")
    void shouldAddSecondCoding_whenStandardMappingPresent() {
        Observation obs = Observation.builder()
                .canonicalId("obs-4").subjectId("family-1")
                .code("ICF").display("Índice de Cohesión Familiar").status("FINAL").build();
        ConceptMapping mapping = new ConceptMapping("ICF", "http://example.org/test-system", "999999", "Ejemplo de prueba", "1.0");

        var fhirObs = ObservationFhirMapper.toFhir(obs, mapping);

        assertThat(fhirObs.getCode().getCoding()).hasSize(2);
        assertThat(fhirObs.getCode().getCoding().get(0).getCode()).isEqualTo("ICF");
        assertThat(fhirObs.getCode().getCoding().get(1).getSystem()).isEqualTo("http://example.org/test-system");
        assertThat(fhirObs.getCode().getCoding().get(1).getCode()).isEqualTo("999999");
    }
}
