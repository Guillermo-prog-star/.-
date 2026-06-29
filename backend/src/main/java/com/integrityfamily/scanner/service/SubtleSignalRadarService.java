package com.integrityfamily.scanner.service;

import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.EvaluationStatus;
import com.integrityfamily.domain.FamilyLongitudinalState;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.repository.FamilyLongitudinalStateRepository;
import com.integrityfamily.domain.repository.RiskSnapshotRepository;
import com.integrityfamily.scanner.dto.SubtleSignalRadarResponse;
import com.integrityfamily.scanner.dto.SubtleSignalRadarResponse.*;
import com.integrityfamily.trajectory.service.TrajectorySuggestionService;
import com.integrityfamily.trajectory.service.TrajectorySuggestionService.TrajectorySuggestion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Radar de Señales Sutiles — Fase 1.
 *
 * Analiza la trayectoria temporal de evaluaciones ICF para detectar
 * microseñales (patrones de riesgo emergentes) y fortalezas invisibles
 * antes de que sean visibles para la familia.
 *
 * No reemplaza el sistema de riesgo existente; lo complementa con
 * análisis longitudinal y narrativa de tendencias.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SubtleSignalRadarService {

    private final EvaluationRepository evaluationRepository;
    private final FamilyLongitudinalStateRepository ltsRepository;
    private final RiskSnapshotRepository riskSnapshotRepository;
    private final TrajectorySuggestionService suggestionService;

    private static final double CRITICAL_DECLINE_THRESHOLD = -15.0;
    private static final double DECLINE_THRESHOLD = -5.0;
    private static final double IMPROVE_THRESHOLD = 5.0;
    private static final double STRONG_IMPROVE_THRESHOLD = 15.0;
    private static final int MIN_EVALUATIONS_FOR_TREND = 2;

    @Transactional(readOnly = true)
    public SubtleSignalRadarResponse analyze(Long familyId) {
        log.info("[RADAR] Iniciando análisis de señales sutiles para familia {}", familyId);

        List<Evaluation> evaluations = evaluationRepository
            .findByFamilyIdOrderByFinalizedAtAsc(familyId)
            .stream()
            .filter(e -> e.getStatus() == EvaluationStatus.FINALIZED && e.getIcf() != null)
            .toList();

        FamilyLongitudinalState lts = ltsRepository.findByFamilyId(familyId).orElse(null);

        int confidence = computeConfidence(evaluations, lts);

        if (evaluations.isEmpty()) {
            return emptyRadar(familyId, confidence);
        }

        // Extraer series temporales por dimensión
        List<DimensionSeries> series = extractDimensionSeries(evaluations);

        // Calcular tendencias por dimensión
        DimensionTrend emocionesTrend = buildTrend("emociones", series);
        DimensionTrend comunicacionTrend = buildTrend("comunicacion", series);
        DimensionTrend habitosTrend = buildTrend("habitos", series);
        DimensionTrend tiemposTrend = buildTrend("tiempos", series);

        // Tendencia ICF global
        IcfTrend icfTrend = buildIcfTrend(evaluations, lts);

        // Detectar microseñales
        List<MicroSignal> microSignals = detectMicroSignals(
            emocionesTrend, comunicacionTrend, habitosTrend, tiemposTrend, lts, evaluations);

        // Detectar fortalezas invisibles
        List<InvisibleStrength> strengths = detectStrengths(
            emocionesTrend, comunicacionTrend, habitosTrend, tiemposTrend, lts, evaluations);

        // Cruzar con banco de trayectorias
        List<TrajectoryMatch> trajectoryMatches = buildTrajectoryMatches(
            suggestionService.suggest(familyId));

        // Narrativa
        String narrative = buildNarrative(microSignals, strengths, icfTrend, lts, evaluations.size());

        log.info("[RADAR] Análisis completado: {} microseñales, {} fortalezas, confianza {}%",
            microSignals.size(), strengths.size(), confidence);

        return new SubtleSignalRadarResponse(
            familyId,
            evaluations.size(),
            emocionesTrend,
            comunicacionTrend,
            habitosTrend,
            tiemposTrend,
            icfTrend,
            microSignals,
            strengths,
            trajectoryMatches,
            confidence,
            narrative,
            LocalDateTime.now()
        );
    }

    // ─── Series temporales ────────────────────────────────────────────────────

    private record DimensionSeries(String dimension, List<Double> scores) {}

    private List<DimensionSeries> extractDimensionSeries(List<Evaluation> evals) {
        Map<String, List<Double>> map = new LinkedHashMap<>();
        map.put("emociones", new ArrayList<>());
        map.put("comunicacion", new ArrayList<>());
        map.put("habitos", new ArrayList<>());
        map.put("tiempos", new ArrayList<>());

        for (Evaluation eval : evals) {
            if (eval.getDimensionScores() == null) continue;
            eval.getDimensionScores().forEach(ds -> {
                String dim = normalizeDimension(ds.getDimensionName());
                if (map.containsKey(dim) && ds.getScore() != null) {
                    map.get(dim).add(ds.getScore());
                }
            });
        }

        return map.entrySet().stream()
            .map(e -> new DimensionSeries(e.getKey(), e.getValue()))
            .toList();
    }

    private String normalizeDimension(String raw) {
        if (raw == null) return "";
        String lower = raw.toLowerCase().trim();
        if (lower.contains("emoc")) return "emociones";
        if (lower.contains("comun")) return "comunicacion";
        if (lower.contains("habit")) return "habitos";
        if (lower.contains("tiempo")) return "tiempos";
        return lower;
    }

    // ─── Tendencias ───────────────────────────────────────────────────────────

    private DimensionTrend buildTrend(String dimension, List<DimensionSeries> allSeries) {
        DimensionSeries series = allSeries.stream()
            .filter(s -> s.dimension().equals(dimension))
            .findFirst()
            .orElse(new DimensionSeries(dimension, List.of()));

        List<Double> scores = series.scores();
        if (scores.isEmpty()) {
            return new DimensionTrend(dimension, null, null, null, "NO_DATA",
                "Sin datos suficientes para esta dimensión", List.of());
        }

        double current = scores.get(scores.size() - 1);
        Double previous = scores.size() >= 2 ? scores.get(scores.size() - 2) : null;
        Double delta = previous != null ? current - previous : null;

        String direction = classifyDirection(delta);
        String signal = buildDimensionSignal(dimension, current, delta, direction, scores);

        return new DimensionTrend(dimension, current, previous, delta, direction, signal, scores);
    }

    private String classifyDirection(Double delta) {
        if (delta == null) return "STABLE";
        if (delta <= CRITICAL_DECLINE_THRESHOLD) return "CRITICAL_DECLINE";
        if (delta <= DECLINE_THRESHOLD) return "DECLINING";
        if (delta >= STRONG_IMPROVE_THRESHOLD) return "STRONG_IMPROVING";
        if (delta >= IMPROVE_THRESHOLD) return "IMPROVING";
        return "STABLE";
    }

    private String buildDimensionSignal(String dim, double current, Double delta,
                                        String direction, List<Double> scores) {
        String dimLabel = switch (dim) {
            case "emociones" -> "la regulación emocional";
            case "comunicacion" -> "la comunicación familiar";
            case "habitos" -> "los hábitos compartidos";
            case "tiempos" -> "los tiempos juntos";
            default -> dim;
        };

        boolean consecutiveDecline = isConsecutiveDecline(scores, 3);
        boolean consecutiveGrowth = isConsecutiveGrowth(scores, 3);

        if (consecutiveDecline) {
            return String.format("%.0f pts — %s lleva %d evaluaciones bajando de forma sostenida",
                current, dimLabel, Math.min(scores.size(), 3));
        }
        if (consecutiveGrowth) {
            return String.format("%.0f pts — %s muestra crecimiento sostenido (%d evaluaciones)",
                current, dimLabel, Math.min(scores.size(), 3));
        }
        if ("CRITICAL_DECLINE".equals(direction) && delta != null) {
            return String.format("%.0f pts — caída crítica de %.0f puntos desde la última evaluación",
                current, Math.abs(delta));
        }
        if ("DECLINING".equals(direction) && delta != null) {
            return String.format("%.0f pts — bajó %.0f puntos, tendencia a vigilar",
                current, Math.abs(delta));
        }
        if ("IMPROVING".equals(direction) && delta != null) {
            return String.format("%.0f pts — subió %.0f puntos respecto a la evaluación anterior",
                current, delta);
        }
        return String.format("%.0f pts — estable", current);
    }

    private IcfTrend buildIcfTrend(List<Evaluation> evals, FamilyLongitudinalState lts) {
        double current = evals.get(evals.size() - 1).getIcf();
        Double delta30d = lts != null && lts.getIcf30dAgo() != null
            ? current - lts.getIcf30dAgo() : null;
        Double delta90d = lts != null && lts.getIcf90dAgo() != null
            ? current - lts.getIcf90dAgo() : null;

        String direction = delta30d != null ? classifyDirection(delta30d) : "STABLE";
        String phase = lts != null ? lts.getEvolutionPhase() : null;

        return new IcfTrend(current, delta30d, delta90d, direction, phase);
    }

    // ─── Microseñales ─────────────────────────────────────────────────────────

    private List<MicroSignal> detectMicroSignals(
        DimensionTrend emoc, DimensionTrend comun, DimensionTrend habit,
        DimensionTrend tiemp, FamilyLongitudinalState lts, List<Evaluation> evals) {

        List<MicroSignal> signals = new ArrayList<>();

        // Señal 1: caída sostenida en comunicación
        if (isSignificantDecline(comun)) {
            signals.add(new MicroSignal("comunicacion", "COMM_SUSTAINED_DECLINE",
                "La frecuencia y calidad de la comunicación familiar ha disminuido de forma sostenida. " +
                "Las conversaciones importantes se están haciendo menos frecuentes.",
                severity(comun.delta(), CRITICAL_DECLINE_THRESHOLD),
                0.9));
        }

        // Señal 2: desconexión emocional progresiva
        if (isSignificantDecline(emoc)) {
            signals.add(new MicroSignal("emociones", "EMOC_PROGRESSIVE_DISCONNECT",
                "La dimensión emocional muestra un patrón descendente. " +
                "Los miembros pueden estar evitando expresar lo que sienten.",
                severity(emoc.delta(), CRITICAL_DECLINE_THRESHOLD),
                0.85));
        }

        // Señal 3: hábitos en deterioro mientras emociones también bajan
        if (isDecline(habit) && isDecline(emoc)) {
            signals.add(new MicroSignal("habitos", "HABIT_EMOC_DUAL_DECLINE",
                "Los hábitos y las emociones bajan simultáneamente. " +
                "Este patrón suele preceder una pérdida de rutinas estructuradoras del vínculo familiar.",
                "MEDIUM", 0.75));
        }

        // Señal 4: tiempos compartidos en caída cuando comunicación ya es baja
        if (isDecline(tiemp) && comun.currentScore() != null && comun.currentScore() < 50) {
            signals.add(new MicroSignal("tiempos", "TIME_COMUN_CONVERGENCE",
                "Los tiempos compartidos disminuyen mientras la comunicación ya está debilitada. " +
                "La familia dispone de menos oportunidades para resolver lo que no se está hablando.",
                "HIGH", 0.80));
        }

        // Señal 5: LTS — colapso comunicacional activo
        if (lts != null && Boolean.TRUE.equals(lts.getCommunicationCollapseActive())) {
            signals.add(new MicroSignal("comunicacion", "COMM_COLLAPSE_ACTIVE",
                "El sistema detecta un colapso comunicacional activo: tres o más entradas " +
                "negativas en comunicación en los últimos 7 días.",
                "HIGH", 0.95));
        }

        // Señal 6: deterioro sostenido en LTS (3+ registros negativos consecutivos)
        if (lts != null && lts.getConsecutiveDeteriorations() != null
            && lts.getConsecutiveDeteriorations() >= 3) {
            signals.add(new MicroSignal("general", "LTS_SUSTAINED_DETERIORATION",
                String.format("La familia acumula %d registros negativos consecutivos. " +
                    "La tendencia general de deterioro está activa.",
                    lts.getConsecutiveDeteriorations()),
                lts.getConsecutiveDeteriorations() >= 5 ? "HIGH" : "MEDIUM", 0.88));
        }

        // Señal 7: crisis reciente
        if (lts != null && lts.getCrisisCount30d() != null && lts.getCrisisCount30d() >= 1) {
            signals.add(new MicroSignal("general", "RECENT_CRISIS_DETECTED",
                String.format("Se registraron %d crisis en los últimos 30 días. " +
                    "El sistema está en estado de alerta activa.",
                    lts.getCrisisCount30d()),
                lts.getCrisisCount30d() >= 2 ? "HIGH" : "MEDIUM", 0.92));
        }

        // Señal 8: dimensión crítica por debajo de 40
        checkLowDimension(emoc, "emociones", signals);
        checkLowDimension(comun, "comunicacion", signals);
        checkLowDimension(habit, "habitos", signals);
        checkLowDimension(tiemp, "tiempos", signals);

        return signals.stream()
            .sorted(Comparator.comparingDouble(MicroSignal::weight).reversed())
            .limit(8)
            .toList();
    }

    private void checkLowDimension(DimensionTrend trend, String dim, List<MicroSignal> signals) {
        if (trend.currentScore() != null && trend.currentScore() < 40) {
            signals.add(new MicroSignal(dim, dim.toUpperCase() + "_CRITICAL_LOW",
                String.format("La dimensión '%s' está en %.0f/100, por debajo del umbral crítico de 40. " +
                    "Requiere intervención directa.", dim, trend.currentScore()),
                "HIGH", 0.93));
        }
    }

    // ─── Fortalezas invisibles ────────────────────────────────────────────────

    private List<InvisibleStrength> detectStrengths(
        DimensionTrend emoc, DimensionTrend comun, DimensionTrend habit,
        DimensionTrend tiemp, FamilyLongitudinalState lts, List<Evaluation> evals) {

        List<InvisibleStrength> strengths = new ArrayList<>();

        // Fortaleza 1: dimensión que mejora consistentemente
        if (isStrongImprovement(emoc)) {
            strengths.add(new InvisibleStrength("emociones",
                "La regulación emocional está creciendo de forma sostenida",
                String.format("Pasó de %.0f a %.0f en la última evaluación (+%.0f puntos)",
                    emoc.previousScore(), emoc.currentScore(), emoc.delta())));
        }
        if (isStrongImprovement(comun)) {
            strengths.add(new InvisibleStrength("comunicacion",
                "La comunicación familiar muestra un salto de calidad significativo",
                String.format("Subió %.0f puntos, señal de que los espacios de diálogo están funcionando",
                    comun.delta())));
        }
        if (isStrongImprovement(habit)) {
            strengths.add(new InvisibleStrength("habitos",
                "Los hábitos familiares están fortaleciéndose",
                String.format("Mejora de %.0f puntos — las rutinas del hogar se están afianzando",
                    habit.delta())));
        }
        if (isStrongImprovement(tiemp)) {
            strengths.add(new InvisibleStrength("tiempos",
                "Los tiempos compartidos están aumentando de forma notable",
                String.format("Crecimiento de %.0f puntos en tiempo de calidad juntos", tiemp.delta())));
        }

        // Fortaleza 2: resiliencia — recuperación después de crisis
        if (lts != null && lts.getCrisisCountTotal() != null && lts.getCrisisCountTotal() >= 1
            && lts.getConsecutiveImprovements() != null && lts.getConsecutiveImprovements() >= 2) {
            strengths.add(new InvisibleStrength("general",
                "La familia muestra resiliencia activa: se recupera después de las crisis",
                String.format("%d mejoras consecutivas registradas tras haber atravesado %d crisis",
                    lts.getConsecutiveImprovements(), lts.getCrisisCountTotal())));
        }

        // Fortaleza 3: mejora ICF a largo plazo (90 días)
        if (evals.size() >= 3) {
            double oldest = evals.get(0).getIcf();
            double newest = evals.get(evals.size() - 1).getIcf();
            if (newest - oldest >= 10) {
                strengths.add(new InvisibleStrength("general",
                    "El ICF familiar creció de forma sostenida en el tiempo",
                    String.format("De %.1f a %.1f a lo largo de %d evaluaciones (+%.1f puntos totales)",
                        oldest, newest, evals.size(), newest - oldest)));
            }
        }

        return strengths;
    }

    // ─── Trayectorias ─────────────────────────────────────────────────────────

    private List<TrajectoryMatch> buildTrajectoryMatches(List<TrajectorySuggestion> suggestions) {
        return suggestions.stream()
            .map(s -> new TrajectoryMatch(s.code(), s.name(), s.confidenceScore(), s.reason()))
            .toList();
    }

    // ─── Narrativa ────────────────────────────────────────────────────────────

    private String buildNarrative(List<MicroSignal> signals, List<InvisibleStrength> strengths,
                                   IcfTrend icf, FamilyLongitudinalState lts, int evalCount) {
        StringBuilder sb = new StringBuilder();

        String phase = lts != null ? lts.getEvolutionPhase() : null;
        String phaseLabel = phase != null ? switch (phase.toLowerCase()) {
            case "inconsciente" -> "reconocimiento";
            case "reactivo" -> "transición";
            case "consciente" -> "transformación activa";
            case "pleno" -> "consolidación";
            default -> "evolución";
        } : "evolución";

        sb.append(String.format(
            "Esta familia ha completado %d evaluaciones y se encuentra en la fase de %s. ",
            evalCount, phaseLabel));

        if (!signals.isEmpty()) {
            long highSignals = signals.stream().filter(s -> "HIGH".equals(s.severity())).count();
            if (highSignals > 0) {
                sb.append(String.format(
                    "El radar detecta %d señal%s de alta intensidad que merece%s atención inmediata. ",
                    highSignals, highSignals > 1 ? "es" : "", highSignals > 1 ? "n" : ""));
            } else {
                sb.append(String.format(
                    "Hay %d microseñal%s sutil%s que, aunque no representan crisis, indican una tendencia que conviene acompañar. ",
                    signals.size(), signals.size() > 1 ? "es" : "",
                    signals.size() > 1 ? "es" : ""));
            }
        } else {
            sb.append("No se detectan señales de riesgo emergentes en este momento. ");
        }

        if (!strengths.isEmpty()) {
            sb.append(String.format(
                "Al mismo tiempo, %d fortaleza%s invisible%s %s surgiendo: ",
                strengths.size(), strengths.size() > 1 ? "s" : "",
                strengths.size() > 1 ? "s" : "",
                strengths.size() > 1 ? "están" : "está"));
            sb.append(strengths.get(0).description()).append(". ");
        }

        if (icf.delta30d() != null) {
            if (icf.delta30d() > 5) {
                sb.append(String.format(
                    "El ICF general subió %.1f puntos en los últimos 30 días, señal de que las intervenciones están funcionando.",
                    icf.delta30d()));
            } else if (icf.delta30d() < -5) {
                sb.append(String.format(
                    "El ICF general bajó %.1f puntos respecto a hace 30 días. La trayectoria aún puede modificarse con las misiones propuestas.",
                    Math.abs(icf.delta30d())));
            } else {
                sb.append("El ICF se mantiene estable. Es un buen momento para consolidar los avances recientes.");
            }
        }

        return sb.toString().trim();
    }

    // ─── Confianza ────────────────────────────────────────────────────────────

    private int computeConfidence(List<Evaluation> evals, FamilyLongitudinalState lts) {
        int score = 0;
        if (evals.size() >= 5) score += 40;
        else if (evals.size() >= 3) score += 25;
        else if (evals.size() >= 2) score += 15;
        else score += 5;

        if (lts != null) {
            score += 25;
            if (lts.getIcf30dAgo() != null) score += 15;
            if (lts.getIcf90dAgo() != null) score += 10;
        }

        boolean hasDimensionScores = evals.stream()
            .anyMatch(e -> e.getDimensionScores() != null && !e.getDimensionScores().isEmpty());
        if (hasDimensionScores) score += 10;

        return Math.min(score, 100);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private boolean isDecline(DimensionTrend t) {
        return t.delta() != null && t.delta() <= DECLINE_THRESHOLD;
    }

    private boolean isSignificantDecline(DimensionTrend t) {
        return "DECLINING".equals(t.direction()) || "CRITICAL_DECLINE".equals(t.direction());
    }

    private boolean isStrongImprovement(DimensionTrend t) {
        return t.delta() != null && t.delta() >= STRONG_IMPROVE_THRESHOLD;
    }

    private String severity(Double delta, double criticalThreshold) {
        if (delta == null) return "LOW";
        return delta <= criticalThreshold ? "HIGH" : "MEDIUM";
    }

    private boolean isConsecutiveDecline(List<Double> scores, int n) {
        if (scores.size() < n) return false;
        for (int i = scores.size() - 1; i >= scores.size() - n + 1; i--) {
            if (scores.get(i) >= scores.get(i - 1)) return false;
        }
        return true;
    }

    private boolean isConsecutiveGrowth(List<Double> scores, int n) {
        if (scores.size() < n) return false;
        for (int i = scores.size() - 1; i >= scores.size() - n + 1; i--) {
            if (scores.get(i) <= scores.get(i - 1)) return false;
        }
        return true;
    }

    private SubtleSignalRadarResponse emptyRadar(Long familyId, int confidence) {
        return new SubtleSignalRadarResponse(
            familyId, 0, null, null, null, null, null,
            List.of(), List.of(), List.of(), confidence,
            "Esta familia aún no tiene evaluaciones finalizadas. El radar se activará después de completar la primera evaluación.",
            LocalDateTime.now()
        );
    }
}
