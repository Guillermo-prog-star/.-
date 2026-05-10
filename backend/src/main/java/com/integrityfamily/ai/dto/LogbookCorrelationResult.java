package com.integrityfamily.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogbookCorrelationResult {
    private Long familyId;
    private String familyName;
    private int totalEntriesAnalyzed;
    private double averageEmotionalScore;  // -1.0 a 1.0
    private String generalLabel;            // CRISIS, NEGATIVO, CONSCIENTE, POSITIVO
    private List<DimensionCorrelation> dimensionCorrelations;
    private String adaptationRecommendation; // Instrucción recomendada de adaptación del plan de misiones
    private LocalDateTime calculatedAt;
}
