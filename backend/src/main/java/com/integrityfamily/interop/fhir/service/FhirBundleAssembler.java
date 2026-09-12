package com.integrityfamily.interop.fhir.service;

import com.integrityfamily.interop.canonical.CanonicalFamilyRecord;
import com.integrityfamily.interop.fhir.mapper.GroupFhirMapper;
import com.integrityfamily.interop.fhir.mapper.ObservationFhirMapper;
import com.integrityfamily.interop.fhir.mapper.PatientFhirMapper;
import com.integrityfamily.interop.terminology.TerminologyService;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Service;

/**
 * {@link CanonicalFamilyRecord} → FHIR {@code Bundle} de tipo COLLECTION con
 * los tres recursos piloto de la Fase 4: Group, Patient y Observation.
 * Interventions/Outcomes/ProfessionalNotes/Evidences quedan fuera del bundle
 * porque en la Fase 3 el registro canónico ya los deja vacíos.
 */
@Service
@RequiredArgsConstructor
public class FhirBundleAssembler {

    private final TerminologyService terminologyService;

    public Bundle assemble(CanonicalFamilyRecord record) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        bundle.setId(record.canonicalId() + "-bundle");

        var group = GroupFhirMapper.toFhir(record.household());
        addEntry(bundle, "Group", group);

        for (var person : record.household().members()) {
            addEntry(bundle, "Patient", PatientFhirMapper.toFhir(person));
        }

        for (var assessment : record.assessments()) {
            for (var observation : assessment.observations()) {
                var standardMapping = terminologyService.lookup(observation.code()).orElse(null);
                addEntry(bundle, "Observation", ObservationFhirMapper.toFhir(observation, standardMapping));
            }
        }

        return bundle;
    }

    private void addEntry(Bundle bundle, String resourceType, Resource resource) {
        bundle.addEntry()
                .setFullUrl(resourceType + "/" + resource.getId())
                .setResource(resource);
    }
}
