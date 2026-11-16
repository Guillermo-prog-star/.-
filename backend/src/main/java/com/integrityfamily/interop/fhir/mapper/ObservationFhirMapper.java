package com.integrityfamily.interop.fhir.mapper;

import com.integrityfamily.interop.canonical.Observation;
import com.integrityfamily.interop.fhir.FhirReferences;
import com.integrityfamily.interop.terminology.ConceptMapping;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.StringType;

import java.time.ZoneId;
import java.util.Date;

/**
 * {@link Observation} (modelo canónico) → FHIR {@code Observation} (recurso
 * piloto de la Fase 4). Se nombra igual que la clase canónica a propósito —
 * mismo criterio que con Consent en la Fase 3, siempre se referencia
 * completamente calificada para evitar ambigüedad.
 *
 * El código usa un CodeSystem propio de Integrity (ej. "ICF",
 * "SOMATIC_AWARENESS") como coding principal siempre. Si el llamador
 * (Fase 5, {@code TerminologyService}) resuelve un mapeo verificado a
 * SNOMED/LOINC, se agrega como una segunda {@code Coding} — nunca reemplaza
 * la primera. El mapper sigue siendo una función pura: no consulta el
 * Terminology Service él mismo, solo recibe el resultado ya resuelto.
 */
public final class ObservationFhirMapper {

    private static final String CODE_SYSTEM = "https://integrityfamily.com/fhir/CodeSystem/observation-code";

    private ObservationFhirMapper() {}

    public static org.hl7.fhir.r4.model.Observation toFhir(Observation observation, ConceptMapping standardMapping) {
        org.hl7.fhir.r4.model.Observation fhirObservation = new org.hl7.fhir.r4.model.Observation();
        fhirObservation.setId(observation.canonicalId());
        fhirObservation.setStatus(mapStatus(observation.status()));
        fhirObservation.setSubject(FhirReferences.forSubject(observation.subjectId()));

        CodeableConcept code = new CodeableConcept()
                .addCoding(new Coding().setSystem(CODE_SYSTEM).setCode(observation.code()).setDisplay(observation.display()))
                .setText(observation.display());
        if (standardMapping != null) {
            code.addCoding(new Coding()
                    .setSystem(standardMapping.targetSystem())
                    .setCode(standardMapping.targetCode())
                    .setDisplay(standardMapping.targetDisplay())
                    .setVersion(standardMapping.targetVersion()));
        }
        fhirObservation.setCode(code);

        if (observation.valueNumeric() != null) {
            fhirObservation.setValue(new Quantity().setValue(observation.valueNumeric()));
        } else if (observation.valueText() != null) {
            fhirObservation.setValue(new StringType(observation.valueText()));
        }

        if (observation.effectiveAt() != null) {
            fhirObservation.setEffective(new DateTimeType(
                    Date.from(observation.effectiveAt().atZone(ZoneId.systemDefault()).toInstant())));
        }

        return fhirObservation;
    }

    private static org.hl7.fhir.r4.model.Observation.ObservationStatus mapStatus(String status) {
        if (status == null) return org.hl7.fhir.r4.model.Observation.ObservationStatus.UNKNOWN;
        return switch (status) {
            case "FINAL" -> org.hl7.fhir.r4.model.Observation.ObservationStatus.FINAL;
            case "PRELIMINARY" -> org.hl7.fhir.r4.model.Observation.ObservationStatus.PRELIMINARY;
            case "AMENDED" -> org.hl7.fhir.r4.model.Observation.ObservationStatus.AMENDED;
            default -> org.hl7.fhir.r4.model.Observation.ObservationStatus.UNKNOWN;
        };
    }
}
