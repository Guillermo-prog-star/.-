package com.integrityfamily.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DimensionCorrelation {
    private String dimensionName;
    private String dimensionFriendlyName;
    private double diagnosticScore;       // 0.0 a 100.0 (última evaluación)
    private double logbookSentimentScore;  // -1.0 a 1.0 (promedio de bitácora)
    private double correlationDelta;       // Grado de discrepancia/consonancia
    private boolean requiresPriorityShift; // Si la bitácora exige forzar esta dimensión como prioritaria
}
