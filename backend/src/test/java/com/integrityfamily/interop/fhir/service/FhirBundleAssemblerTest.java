package com.integrityfamily.interop.fhir.service;

import com.integrityfamily.interop.canonical.*;
import com.integrityfamily.interop.terminology.ConceptMapping;
import com.integrityfamily.interop.terminology.TerminologyService;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FhirBundleAssembler")
class FhirBundleAssemblerTest {

    private final TerminologyService terminologyService = new TerminologyService();
    private final FhirBundleAssembler assembler = new FhirBundleAssembler(terminologyService);

    @Test
    @DisplayName("ensambla Bundle COLLECTION con 1 Group + N Patient + M Observation")
    void shouldAssembleBundle() {
        Person p1 = Person.builder().canonicalId("member-1").identifiers(List.of()).fullName("Carlos").active(true).build();
        Person p2 = Person.builder().canonicalId("member-2").identifiers(List.of()).fullName("Ana").active(true).build();
        Household household = Household.builder().canonicalId("family-1").name("Familia López").members(List.of(p1, p2)).build();

        Observation obs1 = Observation.builder().canonicalId("obs-1").subjectId("family-1").code("ICF").display("ICF").status("FINAL").build();
        Observation obs2 = Observation.builder().canonicalId("obs-2").subjectId("family-1").code("RISK").display("Risk").status("FINAL").build();
        Assessment assessment = Assessment.builder().canonicalId("eval-1").subjectId("family-1").observations(List.of(obs1, obs2)).build();

        CanonicalFamilyRecord record = CanonicalFamilyRecord.builder()
                .canonicalId("family-1")
                .household(household)
                .assessments(List.of(assessment))
                .risks(List.of())
                .consents(List.of())
                .build();

        Bundle bundle = assembler.assemble(record);

        assertThat(bundle.getType()).isEqualTo(Bundle.BundleType.COLLECTION);
        assertThat(bundle.getId()).isEqualTo("family-1-bundle");
        assertThat(bundle.getEntry()).hasSize(5); // 1 Group + 2 Patient + 2 Observation

        long groupCount = bundle.getEntry().stream().filter(e -> e.getResource() instanceof Group).count();
        long patientCount = bundle.getEntry().stream().filter(e -> e.getResource() instanceof Patient).count();
        long obsCount = bundle.getEntry().stream().filter(e -> e.getResource() instanceof org.hl7.fhir.r4.model.Observation).count();

        assertThat(groupCount).isEqualTo(1);
        assertThat(patientCount).isEqualTo(2);
        assertThat(obsCount).isEqualTo(2);
    }

    @Test
    @DisplayName("familia sin miembros ni evaluaciones → Bundle con solo el Group")
    void shouldAssembleMinimalBundle() {
        Household household = Household.builder().canonicalId("family-2").name("Familia X").members(List.of()).build();
        CanonicalFamilyRecord record = CanonicalFamilyRecord.builder()
                .canonicalId("family-2").household(household)
                .assessments(List.of()).risks(List.of()).consents(List.of())
                .build();

        Bundle bundle = assembler.assemble(record);

        assertThat(bundle.getEntry()).hasSize(1);
        assertThat(bundle.getEntry().get(0).getResource()).isInstanceOf(Group.class);
    }

    @Test
    @DisplayName("con mapeo registrado en TerminologyService → la Observation FHIR trae la coding estándar")
    void shouldUseRegisteredTerminologyMapping() {
        terminologyService.register(new ConceptMapping("ICF", "http://example.org/test-system", "999999", "Ejemplo", "1.0"));

        Household household = Household.builder().canonicalId("family-3").name("Familia Y").members(List.of()).build();
        Observation obs = Observation.builder().canonicalId("obs-x").subjectId("family-3").code("ICF").display("ICF").status("FINAL").build();
        Assessment assessment = Assessment.builder().canonicalId("eval-1").subjectId("family-3").observations(List.of(obs)).build();
        CanonicalFamilyRecord record = CanonicalFamilyRecord.builder()
                .canonicalId("family-3").household(household)
                .assessments(List.of(assessment)).risks(List.of()).consents(List.of())
                .build();

        Bundle bundle = assembler.assemble(record);

        var fhirObs = (org.hl7.fhir.r4.model.Observation) bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof org.hl7.fhir.r4.model.Observation)
                .findFirst().orElseThrow().getResource();
        assertThat(fhirObs.getCode().getCoding()).hasSize(2);
        assertThat(fhirObs.getCode().getCoding().get(1).getCode()).isEqualTo("999999");
    }
}
