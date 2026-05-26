package com.integrityfamily.scanner.dto;

import java.time.Instant;

/**
 * DTO de lectura para InferenceRecord.
 * Expone solo los campos relevantes para el frontend y la auditoría científica.
 */
public record InferenceRecordDto(
        Long id,
        Long evaluationId,
        String inferenceKey,
        /** OBSERVED / CORRELATED / INFERRED / STABILIZED / REVISED / ARCHIVED */
        String epistemicState,
        /** IF-TOS: EMERGING / STABLE / ESCALATING / CRITICAL / RECOVERING / RESOLVED */
        String operationalState,
        Double icfValue,
        String riskLevel,
        String criticalDimension,
        /** IF-SUM: total de incertidumbre estructural (0.0–1.0) */
        Double uncertaintyTotal,
        Boolean simulationSuspected,
        /** IF-CIS: hash SHA-256 determinístico de la evidencia — misma entrada → mismo hash */
        String evidenceHash,
        Instant createdAt
) {}
