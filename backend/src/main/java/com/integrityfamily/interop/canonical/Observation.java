package com.integrityfamily.interop.canonical;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Una medición o hallazgo puntual (ej. un puntaje de dimensión ICF, un
 * marcador fenomenológico). Traduce eventualmente a FHIR Observation.
 *
 * @param code   código propio de Integrity (ej. "ICF", "SOMATIC_AWARENESS") —
 *               el mapeo a una terminología estándar (SNOMED/LOINC) es
 *               responsabilidad del Terminology Service, no de este modelo
 * @param status FINAL, PRELIMINARY, AMENDED (semántica FHIR ObservationStatus)
 */
@Builder
public record Observation(
        String canonicalId,
        String subjectId,
        String code,
        String display,
        Double valueNumeric,
        String valueText,
        LocalDateTime effectiveAt,
        String status
) {}
