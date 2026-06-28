package com.integrityfamily.capital.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Snapshot completo de los 20 indicadores SMFF para una familia.
 *
 * @param familyId       familia evaluada
 * @param calculatedAt   timestamp del cálculo
 * @param smffScore      media ponderada de los 20 indicadores (0–100)
 * @param indicators     los 20 resultados individuales, en orden IND-01..IND-20
 * @param totalReal      cuántos indicadores tienen dato real (no estimado)
 * @param dataCompletePct porcentaje de completitud de datos 0–100
 */
public record IndicatorsSnapshot(
        Long             familyId,
        LocalDateTime    calculatedAt,
        double           smffScore,
        List<IndicatorResult> indicators,
        int              totalReal,
        double           dataCompletePct
) {}
