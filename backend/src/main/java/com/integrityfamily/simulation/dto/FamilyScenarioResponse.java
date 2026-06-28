package com.integrityfamily.simulation.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Proyección de tres escenarios familiares condicionales.
 *
 * Muestra futuros plausibles para que la familia comprenda que
 * el resultado depende de sus decisiones, no está predeterminado.
 */
public record FamilyScenarioResponse(

    Long familyId,

    /** ICF actual en el momento de la proyección. */
    double icfBaseline,

    /** Escenario A — sin intervención: la tendencia actual continúa. */
    Scenario scenarioA,

    /** Escenario B — cumpliendo misiones actuales: adherencia normal. */
    Scenario scenarioB,

    /** Escenario C — intervención intensiva: máximo compromiso. */
    Scenario scenarioC,

    /** Mensaje clave que resume la bifurcación de futuros posibles. */
    String pivotMessage,

    /** Ventana de oportunidad: tiempo estimado para cambiar la trayectoria. */
    String opportunityWindow,

    LocalDateTime generatedAt

) {

    public record Scenario(
        String label,               // "Sin intervención" | "Misiones actuales" | "Intervención intensiva"
        String code,                // A | B | C
        int probabilityPercent,     // 0-100
        String direction,           // DECLINE | STABLE | IMPROVE | STRONG_IMPROVE

        /** Proyección ICF a 4, 8 y 12 semanas. */
        ProjectionPoint week4,
        ProjectionPoint week8,
        ProjectionPoint week12,

        /** Proyección por dimensión al final del periodo (12 semanas). */
        DimensionProjection emociones,
        DimensionProjection comunicacion,
        DimensionProjection habitos,
        DimensionProjection tiempos,

        /** Riesgo estimado al final del periodo. */
        String estimatedRiskLevel,

        /** Narración del escenario en lenguaje comprensible. */
        String narrative,

        /** Qué debe hacer la familia para que este escenario ocurra (o se evite). */
        List<String> keyActions
    ) {}

    public record ProjectionPoint(
        int weekNumber,
        double icfProjected,
        double icfMin,              // límite inferior del rango de incertidumbre
        double icfMax,              // límite superior
        String riskLevel
    ) {}

    public record DimensionProjection(
        String dimension,
        double currentScore,
        double projectedScore,
        double delta,
        String direction
    ) {}
}
