package com.integrityfamily.interop.canonical;

import java.time.LocalDateTime;
import lombok.Builder;

/**
 * Equivalente canónico de una trayectoria de riesgo activa (mapea desde el
 * Banco de Trayectorias de Riesgo, {@code trajectory} module, V75+). Traduce
 * eventualmente a FHIR RiskAssessment / Condition según corresponda.
 */
@Builder
public record Risk(
        String canonicalId,
        String subjectId,
        String code,
        String display,
        String macrodomain,
        String severity,
        boolean requiresSafetyProtocol,
        LocalDateTime detectedAt,
        LocalDateTime resolvedAt,
        String status
) {}
