package com.integrityfamily.twin.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DigitalTwinDto(
    Long   familyId,
    String familyName,

    // Huella conductual única
    String behavioralSignature,
    String communicationPattern,
    double resilienceIndex,
    String bondingRhythm,
    String dominantStrength,
    String dominantVulnerability,
    String dataRichness,

    // Métricas del ciclo familiar
    Integer avgDaysBetweenCrises,
    Integer avgRecoveryDays,
    String  peakActivityDay,

    // Patrones detectados
    List<DetectedPattern> detectedPatterns,

    // Correlaciones
    List<Correlation> correlations,

    // Predicciones activas
    List<PredictionDto> activePredictions,

    LocalDateTime computedAt
) {
    public record DetectedPattern(
        String pattern,
        int    frequency,
        int    confidence,
        String description
    ) {}

    public record Correlation(
        String trigger,
        String effect,
        int    lagDays,
        int    confidence
    ) {}

    public record PredictionDto(
        Long   id,
        String predictionType,
        String title,
        String description,
        int    confidence,
        String timeHorizon,
        String recommendedAction,
        String status
    ) {}
}
