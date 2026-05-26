package com.integrityfamily.scanner.service;

import com.integrityfamily.risk.service.RiskAlgoV1Engine;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * IF-REE: Resolvedor de señales emocionales.
 *
 * Mapea nombres de señal (strings almacenados en emotional_rule_signals)
 * a condiciones booleanas evaluadas contra AlgoResult en tiempo real.
 *
 * Vocabulario de señales soportado:
 *
 *   simulation_suspected    → AlgoResult.simulationSuspected == true
 *   relapse_detected        → AlgoResult.relapseDetected == true
 *   dimension_collapse      → alguna dimensión < 2.0 (nivel crítico)
 *   voice_tension           → relapseFlags o mirrorFlags contienen entradas de emociones
 *   interruptions           → ≥2 preguntas MIRROR flagueadas (sobreperfección sistemática)
 *   avoidance               → relapseFlags con dimensión emocional o comunicacional
 *   reduced_participation   → comunicacion < 2.5
 *   chronic_fatigue_signals → habitos < 2.0 AND emociones < 2.5
 *   high_risk               → riskLevel ALTO o CRITICO
 *   low_icf                 → healthyIndex < 40.0
 *
 * Cualquier señal desconocida resuelve a false (no bloquea, no activa).
 */
@Component
public class SignalResolver {

    public boolean isPresent(String signal, RiskAlgoV1Engine.AlgoResult algo) {
        if (signal == null) return false;

        return switch (signal.toLowerCase().trim()) {

            case "simulation_suspected"    -> algo.simulationSuspected();

            case "relapse_detected"        -> algo.relapseDetected();

            case "dimension_collapse"      -> anyDimensionBelow(algo.dimensionScores(), 2.0);

            // voice_tension: respuestas con señales de tensión vocal o conflicto emocional
            // Se detecta cuando hay relapseFlags en la dimensión emocional
            case "voice_tension"           -> flagsContainDimension(algo.relapseFlags(), "emociones")
                                              || algo.mirrorFlags().size() >= 1;

            // interruptions: patrón de sobreperfección en ≥2 preguntas MIRROR
            case "interruptions"           -> algo.mirrorFlags().size() >= 2;

            // avoidance: señales de evasión en dimensiones relacionales
            case "avoidance"               -> flagsContainDimension(algo.relapseFlags(), "comunicacion")
                                              || flagsContainDimension(algo.relapseFlags(), "emociones");

            // reduced_participation: baja puntuación en comunicación
            case "reduced_participation"   -> getDimension(algo.dimensionScores(), "comunicacion") < 2.5;

            // chronic_fatigue_signals: combinación de bajo hábito y bajo emocional
            case "chronic_fatigue_signals" -> getDimension(algo.dimensionScores(), "habitos")    < 2.0
                                           && getDimension(algo.dimensionScores(), "emociones") < 2.5;

            case "high_risk"               -> "ALTO".equals(algo.riskLevel())
                                              || "CRITICO".equals(algo.riskLevel());

            case "low_icf"                 -> algo.healthyIndex() < 40.0;

            default -> {
                // Señal desconocida: no activa la regla pero tampoco falla
                yield false;
            }
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean anyDimensionBelow(Map<String, Double> scores, double threshold) {
        if (scores == null || scores.isEmpty()) return false;
        return scores.values().stream().anyMatch(v -> v < threshold);
    }

    private boolean flagsContainDimension(List<String> flags, String dim) {
        if (flags == null || flags.isEmpty()) return false;
        return flags.stream().anyMatch(f -> f != null && f.toLowerCase().contains("dim=" + dim));
    }

    private double getDimension(Map<String, Double> scores, String dim) {
        if (scores == null) return 3.0;
        return scores.getOrDefault(dim, 3.0);
    }
}
