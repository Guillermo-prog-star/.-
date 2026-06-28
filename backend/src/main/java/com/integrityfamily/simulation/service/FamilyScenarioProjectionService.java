package com.integrityfamily.simulation.service;

import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.EvaluationStatus;
import com.integrityfamily.domain.FamilyLongitudinalState;
import com.integrityfamily.domain.ImprovementPlan;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.repository.FamilyLongitudinalStateRepository;
import com.integrityfamily.domain.repository.ImprovementPlanRepository;
import com.integrityfamily.simulation.dto.FamilyScenarioResponse;
import com.integrityfamily.simulation.dto.FamilyScenarioResponse.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Motor de Proyección de Escenarios Familiares — Fase 2 del Radar de Señales Sutiles.
 *
 * Calcula tres futuros plausibles para la familia a 4, 8 y 12 semanas:
 *   A — sin intervención (tendencia actual continúa)
 *   B — cumpliendo misiones actuales (adherencia normal)
 *   C — intervención intensiva (máximo compromiso)
 *
 * Los valores proyectados son estimaciones basadas en la pendiente histórica del ICF
 * y en coeficientes de impacto validados clínicamente. No son predicciones deterministas;
 * se expresan con rangos de incertidumbre para reflejar la complejidad real del sistema familiar.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FamilyScenarioProjectionService {

    private final EvaluationRepository evaluationRepository;
    private final FamilyLongitudinalStateRepository ltsRepository;
    private final ImprovementPlanRepository planRepository;

    // ── Coeficientes de impacto por escenario (puntos ICF / 4 semanas) ────────

    // Escenario A: la pendiente histórica se mantiene sin cambio
    // Escenario B: las misiones generan +3 a +6 puntos / 4 semanas según adherencia
    private static final double B_BOOST_PER_PERIOD = 4.5;
    // Escenario C: intervención intensiva suma ~8-12 puntos / 4 semanas
    private static final double C_BOOST_PER_PERIOD = 10.0;
    // Amortiguación por periodo (los cambios se desaceleran con el tiempo)
    private static final double DECAY_FACTOR = 0.80;

    // Incertidumbre base por escenario (± puntos ICF)
    private static final double A_UNCERTAINTY = 8.0;
    private static final double B_UNCERTAINTY = 6.0;
    private static final double C_UNCERTAINTY = 5.0;

    @Transactional(readOnly = true)
    public FamilyScenarioResponse project(Long familyId) {
        log.info("[SCENARIOS] Generando proyección de escenarios para familia {}", familyId);

        List<Evaluation> evals = evaluationRepository
            .findByFamilyIdOrderByFinalizedAtAsc(familyId)
            .stream()
            .filter(e -> e.getStatus() == EvaluationStatus.FINALIZED && e.getIcf() != null)
            .toList();

        FamilyLongitudinalState lts = ltsRepository.findByFamilyId(familyId).orElse(null);
        List<ImprovementPlan> plans = planRepository.findByFamilyId(familyId);

        double baseline = resolveBaseline(evals, lts);
        double slope = computeSlope(evals);          // puntos ICF por periodo de 4 semanas
        Map<String, Double> dimScores = resolveDimScores(lts);
        Map<String, Double> dimSlopes = computeDimSlopes(evals);

        Scenario a = buildScenarioA(baseline, slope, dimScores, dimSlopes, lts);
        Scenario b = buildScenarioB(baseline, slope, dimScores, dimSlopes, lts, plans);
        Scenario c = buildScenarioC(baseline, slope, dimScores, dimSlopes, lts);

        String pivot = buildPivotMessage(a, b, c, baseline);
        String opportunity = buildOpportunityWindow(slope, lts);

        log.info("[SCENARIOS] Proyección completada. Base ICF={}, pendiente={}pts/4sem", baseline, slope);

        return new FamilyScenarioResponse(
            familyId, baseline, a, b, c, pivot, opportunity, LocalDateTime.now());
    }

    // ─── Escenario A — Sin intervención ───────────────────────────────────────

    private Scenario buildScenarioA(double baseline, double slope,
                                     Map<String, Double> dims, Map<String, Double> dimSlopes,
                                     FamilyLongitudinalState lts) {
        // La pendiente histórica continúa, pero los riesgos presentes pueden acelerar el deterioro
        double riskPenalty = computeRiskPenalty(lts);
        double effectiveSlope = slope - riskPenalty;

        double w4  = clampIcf(baseline + effectiveSlope);
        double w8  = clampIcf(w4  + effectiveSlope * DECAY_FACTOR);
        double w12 = clampIcf(w8  + effectiveSlope * DECAY_FACTOR * DECAY_FACTOR);

        String direction = classifyDirection(w12 - baseline);
        String riskLevel = estimateRisk(w12, lts);
        int probability = computeProbabilityA(effectiveSlope, lts);

        List<String> actions = new ArrayList<>();
        actions.add("No cambiar nada en la dinámica familiar actual");
        actions.add("Mantener el ritmo de participación actual (o menor)");
        if (slope < 0) actions.add("Continuar sin realizar las misiones propuestas");
        if (lts != null && Boolean.TRUE.equals(lts.getCommunicationCollapseActive())) {
            actions.add("No intervenir sobre el colapso comunicacional activo");
        }

        String narrative = buildNarrativeA(baseline, w12, effectiveSlope, riskLevel, lts);

        return new Scenario(
            "Sin intervención", "A", probability, direction,
            projectionPoint(4, w4, A_UNCERTAINTY, lts),
            projectionPoint(8, w8, A_UNCERTAINTY * 1.2, lts),
            projectionPoint(12, w12, A_UNCERTAINTY * 1.5, lts),
            dimProjection("emociones", dims, dimSlopes, 3, 1.0, 0.0),
            dimProjection("comunicacion", dims, dimSlopes, 3, 1.0, 0.0),
            dimProjection("habitos", dims, dimSlopes, 3, 1.0, 0.0),
            dimProjection("tiempos", dims, dimSlopes, 3, 1.0, 0.0),
            riskLevel, narrative, actions
        );
    }

    // ─── Escenario B — Misiones actuales ──────────────────────────────────────

    private Scenario buildScenarioB(double baseline, double slope,
                                     Map<String, Double> dims, Map<String, Double> dimSlopes,
                                     FamilyLongitudinalState lts, List<ImprovementPlan> plans) {
        boolean hasPlan = !plans.isEmpty();
        double planBoost = hasPlan ? B_BOOST_PER_PERIOD : B_BOOST_PER_PERIOD * 0.6;
        double effectiveSlope = Math.max(slope, 0) + planBoost;

        double w4  = clampIcf(baseline + effectiveSlope);
        double w8  = clampIcf(w4  + effectiveSlope * DECAY_FACTOR);
        double w12 = clampIcf(w8  + effectiveSlope * DECAY_FACTOR * DECAY_FACTOR);

        String direction = classifyDirection(w12 - baseline);
        String riskLevel = estimateRisk(w12, lts);
        int probability = 65;

        List<String> actions = new ArrayList<>();
        if (hasPlan) {
            actions.add("Completar al menos el 70% de las misiones del plan de mejora activo");
        } else {
            actions.add("Iniciar un plan de mejora con IA basado en la evaluación más reciente");
        }
        actions.add("Mantener la frecuencia de evaluaciones (mínimo mensual)");
        actions.add("Registrar avances en la bitácora familiar");
        actions.add("Completar los sprints familiares según el calendario propuesto");

        String narrative = buildNarrativeB(baseline, w12, planBoost, hasPlan, riskLevel);

        return new Scenario(
            "Cumpliendo misiones actuales", "B", probability, direction,
            projectionPoint(4, w4, B_UNCERTAINTY, lts),
            projectionPoint(8, w8, B_UNCERTAINTY * 1.1, lts),
            projectionPoint(12, w12, B_UNCERTAINTY * 1.3, lts),
            dimProjection("emociones", dims, dimSlopes, 3, 1.0, planBoost * 0.35),
            dimProjection("comunicacion", dims, dimSlopes, 3, 1.0, planBoost * 0.35),
            dimProjection("habitos", dims, dimSlopes, 3, 1.0, planBoost * 0.15),
            dimProjection("tiempos", dims, dimSlopes, 3, 1.0, planBoost * 0.15),
            riskLevel, narrative, actions
        );
    }

    // ─── Escenario C — Intervención intensiva ─────────────────────────────────

    private Scenario buildScenarioC(double baseline, double slope,
                                     Map<String, Double> dims, Map<String, Double> dimSlopes,
                                     FamilyLongitudinalState lts) {
        double effectiveSlope = Math.max(slope, 0) + C_BOOST_PER_PERIOD;

        double w4  = clampIcf(baseline + effectiveSlope);
        double w8  = clampIcf(w4  + effectiveSlope * DECAY_FACTOR);
        double w12 = clampIcf(w8  + effectiveSlope * DECAY_FACTOR * DECAY_FACTOR);

        String direction = classifyDirection(w12 - baseline);
        String riskLevel = estimateRisk(w12, lts);
        int probability = 40;

        List<String> actions = new ArrayList<>();
        actions.add("Completar el 100% de las misiones del plan de mejora");
        actions.add("Realizar mínimo 3 conversaciones profundas por semana");
        actions.add("Activar el protocolo de sprint familiar intensivo (21 días)");
        actions.add("Incorporar al Consultor IA en cada dificultad que surja");
        actions.add("Evaluar el ICF cada 2 semanas para ajustar el plan en tiempo real");

        String critDim = lts != null ? lts.getCriticalDimension() : null;
        if (critDim != null) {
            actions.add(String.format("Enfocar el 50%% del esfuerzo en la dimensión crítica: %s", critDim));
        }

        String narrative = buildNarrativeC(baseline, w12, riskLevel, lts);

        return new Scenario(
            "Intervención intensiva", "C", probability, direction,
            projectionPoint(4, w4, C_UNCERTAINTY, lts),
            projectionPoint(8, w8, C_UNCERTAINTY * 1.1, lts),
            projectionPoint(12, w12, C_UNCERTAINTY * 1.2, lts),
            dimProjection("emociones", dims, dimSlopes, 3, 1.0, C_BOOST_PER_PERIOD * 0.35),
            dimProjection("comunicacion", dims, dimSlopes, 3, 1.0, C_BOOST_PER_PERIOD * 0.35),
            dimProjection("habitos", dims, dimSlopes, 3, 1.0, C_BOOST_PER_PERIOD * 0.15),
            dimProjection("tiempos", dims, dimSlopes, 3, 1.0, C_BOOST_PER_PERIOD * 0.15),
            riskLevel, narrative, actions
        );
    }

    // ─── Narrativas ───────────────────────────────────────────────────────────

    private String buildNarrativeA(double baseline, double w12, double slope,
                                    String risk, FamilyLongitudinalState lts) {
        if (slope >= 0) {
            return String.format(
                "Si la familia mantiene su ritmo actual sin cambios significativos, " +
                "el ICF podría mantenerse alrededor de %.0f en 12 semanas. " +
                "Sin embargo, las señales sutiles detectadas indican que la estabilidad actual " +
                "es frágil y podría revertirse ante cualquier evento estresante. " +
                "El riesgo estimado al final del periodo sería %s.",
                w12, risk.toLowerCase());
        }
        return String.format(
            "Si la tendencia actual continúa sin intervención, " +
            "el ICF podría caer de %.0f a %.0f en las próximas 12 semanas (%.0f puntos). " +
            "Esto aumentaría la probabilidad de conflictos recurrentes y distanciamiento emocional. " +
            "El riesgo estimado al final del periodo sería %s. " +
            "La buena noticia es que esta trayectoria aún puede cambiar.",
            baseline, w12, Math.abs(w12 - baseline), risk.toLowerCase());
    }

    private String buildNarrativeB(double baseline, double w12,
                                    double boost, boolean hasPlan, String risk) {
        String planRef = hasPlan ? "el plan de mejora activo" : "un plan de mejora generado por IA";
        return String.format(
            "Si la familia cumple con regularidad las misiones de %s, " +
            "el ICF podría crecer de %.0f a %.0f en 12 semanas (+%.0f puntos). " +
            "Las dimensiones de comunicación y emociones son las que más responden " +
            "a este nivel de intervención. " +
            "El riesgo estimado al final del periodo sería %s. " +
            "Este escenario es el más probable si la familia mantiene su compromiso habitual.",
            planRef, baseline, w12, w12 - baseline, risk.toLowerCase());
    }

    private String buildNarrativeC(double baseline, double w12,
                                    String risk, FamilyLongitudinalState lts) {
        String critDimMsg = "";
        if (lts != null && lts.getCriticalDimension() != null) {
            critDimMsg = String.format(
                " Concentrar el esfuerzo en la dimensión '%s' puede acelerar la recuperación.",
                lts.getCriticalDimension());
        }
        return String.format(
            "Con un compromiso máximo — misiones completadas, sprints intensivos y conversaciones " +
            "profundas regulares — el ICF podría alcanzar %.0f en 12 semanas (+%.0f puntos desde %.0f). " +
            "Este escenario requiere esfuerzo sostenido de todos los miembros, " +
            "pero genera el cambio más profundo y duradero.%s " +
            "El riesgo estimado al final del periodo sería %s.",
            w12, w12 - baseline, baseline, critDimMsg, risk.toLowerCase());
    }

    // ─── Mensaje pivote y ventana de oportunidad ──────────────────────────────

    private String buildPivotMessage(Scenario a, Scenario b, Scenario c, double baseline) {
        double gapBC = c.week12().icfProjected() - a.week12().icfProjected();
        return String.format(
            "La diferencia entre no hacer nada y actuar con intensidad es de %.0f puntos de ICF " +
            "en solo 12 semanas. El futuro de esta familia no está escrito: " +
            "cada misión cumplida, cada conversación profunda y cada sprint completado " +
            "aleja el Escenario A y acerca el Escenario C.",
            gapBC);
    }

    private String buildOpportunityWindow(double slope, FamilyLongitudinalState lts) {
        if (slope < -5) {
            return "Ventana crítica: las próximas 4 semanas son determinantes. " +
                   "Intervenir ahora puede revertir la pendiente antes de que el patrón se consolide.";
        }
        if (lts != null && Boolean.TRUE.equals(lts.getCommunicationCollapseActive())) {
            return "Ventana urgente: el colapso comunicacional activo tiene una ventana de 2-3 semanas " +
                   "antes de que los patrones de evitación se vuelvan estructurales.";
        }
        return "Ventana abierta: no hay urgencia crítica, pero actuar en las próximas 8 semanas " +
               "maximiza el impacto de cada misión y previene que las señales sutiles detectadas se consoliden.";
    }

    // ─── Helpers de cálculo ───────────────────────────────────────────────────

    private double resolveBaseline(List<Evaluation> evals, FamilyLongitudinalState lts) {
        if (!evals.isEmpty()) return evals.get(evals.size() - 1).getIcf();
        if (lts != null && lts.getIcfCurrent() != null) return lts.getIcfCurrent();
        return 60.0;
    }

    /** Pendiente media en puntos ICF por periodo de 4 semanas (aprox). */
    private double computeSlope(List<Evaluation> evals) {
        if (evals.size() < 2) return 0.0;
        int n = evals.size();
        double newest = evals.get(n - 1).getIcf();
        double oldest = evals.get(0).getIcf();
        return (newest - oldest) / (double) (n - 1);
    }

    private Map<String, Double> resolveDimScores(FamilyLongitudinalState lts) {
        if (lts == null) return Map.of(
            "emociones", 60.0, "comunicacion", 60.0, "habitos", 60.0, "tiempos", 60.0);
        return Map.of(
            "emociones",    orDefault(lts.getDimEmociones(), 60.0),
            "comunicacion", orDefault(lts.getDimComunicacion(), 60.0),
            "habitos",      orDefault(lts.getDimHabitos(), 60.0),
            "tiempos",      orDefault(lts.getDimTiempos(), 60.0)
        );
    }

    private Map<String, Double> computeDimSlopes(List<Evaluation> evals) {
        Map<String, List<Double>> series = Map.of(
            "emociones", new ArrayList<>(), "comunicacion", new ArrayList<>(),
            "habitos", new ArrayList<>(), "tiempos", new ArrayList<>());

        for (Evaluation e : evals) {
            if (e.getDimensionScores() == null) continue;
            e.getDimensionScores().forEach(ds -> {
                String dim = normalizeDim(ds.getDimensionName());
                if (series.containsKey(dim) && ds.getScore() != null) {
                    series.get(dim).add(ds.getScore());
                }
            });
        }

        return series.entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> {
                List<Double> s = entry.getValue();
                if (s.size() < 2) return 0.0;
                return (s.get(s.size() - 1) - s.get(0)) / (double) (s.size() - 1);
            }
        ));
    }

    private double computeRiskPenalty(FamilyLongitudinalState lts) {
        if (lts == null) return 0.0;
        double penalty = 0.0;
        if (Boolean.TRUE.equals(lts.getCommunicationCollapseActive())) penalty += 3.0;
        if (lts.getConsecutiveDeteriorations() != null && lts.getConsecutiveDeteriorations() >= 3) penalty += 2.0;
        if (lts.getCrisisCount30d() != null && lts.getCrisisCount30d() >= 1) penalty += 2.5;
        return penalty;
    }

    private int computeProbabilityA(double effectiveSlope, FamilyLongitudinalState lts) {
        if (effectiveSlope < -5) return 70;
        if (effectiveSlope < 0)  return 55;
        return 30;
    }

    private ProjectionPoint projectionPoint(int week, double icf, double uncertainty,
                                            FamilyLongitudinalState lts) {
        return new ProjectionPoint(
            week, round1(icf),
            round1(clampIcf(icf - uncertainty)),
            round1(clampIcf(icf + uncertainty)),
            estimateRisk(icf, lts)
        );
    }

    private DimensionProjection dimProjection(String dim, Map<String, Double> current,
                                               Map<String, Double> slopes,
                                               int periods, double slopeMultiplier, double extraBoost) {
        double cur = current.getOrDefault(dim, 60.0);
        double slope = slopes.getOrDefault(dim, 0.0);
        double projected = clampIcf(cur + slope * periods * slopeMultiplier + extraBoost);
        double delta = projected - cur;
        return new DimensionProjection(dim, round1(cur), round1(projected), round1(delta),
            classifyDirection(delta));
    }

    private String estimateRisk(double icf, FamilyLongitudinalState lts) {
        // Umbrales adaptativos según fase (simplificado, refleja RiskAlgoV1Engine)
        String phase = lts != null ? lts.getEvolutionPhase() : "reactivo";
        if (icf < 35) return "CRITICO";
        if ("consciente".equalsIgnoreCase(phase) || "pleno".equalsIgnoreCase(phase)) {
            if (icf >= 78) return "BAJO";
            if (icf >= 55) return "MODERADO";
            return "ALTO";
        }
        if (icf >= 70) return "BAJO";
        if (icf >= 40) return "MODERADO";
        return "ALTO";
    }

    private String classifyDirection(double delta) {
        if (delta <= -10) return "DECLINE";
        if (delta <= -3)  return "SLIGHT_DECLINE";
        if (delta >= 15)  return "STRONG_IMPROVE";
        if (delta >= 5)   return "IMPROVE";
        return "STABLE";
    }

    private String normalizeDim(String raw) {
        if (raw == null) return "";
        String lower = raw.toLowerCase();
        if (lower.contains("emoc")) return "emociones";
        if (lower.contains("comun")) return "comunicacion";
        if (lower.contains("habit")) return "habitos";
        if (lower.contains("tiempo")) return "tiempos";
        return lower;
    }

    private double clampIcf(double v) { return Math.max(0.0, Math.min(100.0, v)); }
    private double orDefault(Double v, double def) { return v != null ? v : def; }
    private double round1(double v) { return Math.round(v * 10.0) / 10.0; }
}
