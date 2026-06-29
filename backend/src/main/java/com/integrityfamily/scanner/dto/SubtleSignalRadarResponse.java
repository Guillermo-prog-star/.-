package com.integrityfamily.scanner.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta del Radar de Señales Sutiles.
 * Contiene tendencias por dimensión ICF, microseñales detectadas,
 * fortalezas invisibles y puntuación de confianza del análisis.
 */
public record SubtleSignalRadarResponse(

    Long familyId,

    /** Número de evaluaciones usadas para el análisis. Mínimo 2 para pendientes. */
    int evaluationsAnalyzed,

    /** Tendencias por cada dimensión ICF. */
    DimensionTrend emociones,
    DimensionTrend comunicacion,
    DimensionTrend habitos,
    DimensionTrend tiempos,

    /** ICF general: valor actual, delta vs 30d, delta vs 90d, dirección. */
    IcfTrend icfOverall,

    /** Microseñales detectadas (patrones sutiles de riesgo). Máx 8. */
    List<MicroSignal> microSignals,

    /** Fortalezas invisibles: patrones positivos que la familia no ve. */
    List<InvisibleStrength> strengths,

    /** Trayectorias de riesgo que coinciden con las señales detectadas. */
    List<TrajectoryMatch> trajectoryMatches,

    /** Confianza del análisis (0-100). Baja si pocas evaluaciones o datos incompletos. */
    int confidenceScore,

    /** Resumen narrativo del radar. */
    String narrativeSummary,

    LocalDateTime generatedAt

) {

    public record DimensionTrend(
        String dimension,
        Double currentScore,
        Double previousScore,
        Double delta,
        String direction,       // IMPROVING | STABLE | DECLINING | CRITICAL_DECLINE
        String signal,          // mensaje corto sobre qué está pasando
        java.util.List<Double> scoreHistory  // historial completo de puntuaciones (orden cronológico)
    ) {}

    public record IcfTrend(
        Double current,
        Double delta30d,
        Double delta90d,
        String direction,
        String evolutionPhase   // inconsciente | reactivo | consciente | pleno
    ) {}

    public record MicroSignal(
        String dimension,       // emociones | comunicacion | habitos | tiempos | general
        String signalCode,      // código interno del patrón
        String description,     // qué se detectó, en lenguaje comprensible
        String severity,        // LOW | MEDIUM | HIGH
        double weight           // 0.0-1.0, para ordenar por relevancia
    ) {}

    public record InvisibleStrength(
        String dimension,
        String description,     // qué fortaleza emergió
        String evidence         // evidencia observable que la sustenta
    ) {}

    public record TrajectoryMatch(
        String trajectoryCode,
        String trajectoryName,
        int confidenceScore,
        String reason
    ) {}
}
