package com.integrityfamily.risk.service;

import com.integrityfamily.domain.Question;
import com.integrityfamily.domain.repository.QuestionRepository;
import com.integrityfamily.dto.EvaluationDtos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * RISK_ALGO_V1 — Motor Oficial de Riesgo Familiar (Taxonomía v2).
 *
 * Mejoras sobre el cálculo básico anterior:
 *   1. severityWeight    — promedio ponderado por peso clínico de cada reactivo (1.0 – 3.3)
 *   2. MIRROR detection  — preguntas type=MIRROR detectan simulación (excluidas del ICF);
 *                          preguntas reverseQuestion=true invierten la escala y SÍ contribuyen al ICF
 *   3. detectsRelapse    — reactivos centinela disparan alerta de recaída cuando score ≤ 2
 *   4. Milestone context — boost 1.5× a reactivos del hito activo de la familia
 *   5. missionGenerator  — sugiere misión automática según dimensión más crítica
 *
 * Fórmula ICF (índice de cohesión familiar):
 *   ICF = emociones×0.30 + comunicacion×0.30 + habitos×0.20 + tiempos×0.20
 *
 * Umbrales de riesgo adaptativos según fase:
 *   inconsciente (W1-M1): BAJO ≥ 65 | MODERADO ≥ 40
 *   reactivo    (M3-M6):  BAJO ≥ 72 | MODERADO ≥ 48
 *   consciente  (M9-M24): BAJO ≥ 78 | MODERADO ≥ 55
 *   pleno       (M30-M36):BAJO ≥ 85 | MODERADO ≥ 65
 *
 * Regla de seguridad crítica (siempre aplica):
 *   Si cualquier dimensión < 25 → CRITICO
 *   Si cualquier dimensión < 40 y base era BAJO/MODERADO → ALTO
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RiskAlgoV1Engine {

    private final QuestionRepository questionRepository;

    // ─── Constantes ────────────────────────────────────────────────────────────

    private static final Map<String, Double> DIM_WEIGHTS = Map.of(
            "emociones",    0.30,
            "comunicacion", 0.30,
            "habitos",      0.20,
            "tiempos",      0.20
    );

    private static final List<String> DIMENSIONS = List.of("emociones", "comunicacion", "habitos", "tiempos");

    /** Fase de consciencia por hito */
    private static final Map<String, String> MILESTONE_PHASE = Map.of(
            "W1",  "inconsciente", "M1",  "inconsciente",
            "M3",  "reactivo",     "M6",  "reactivo",
            "M9",  "consciente",   "M12", "consciente",
            "M18", "consciente",   "M24", "consciente",
            "M30", "pleno",        "M36", "pleno"
    );

    /** Umbral BAJO (≥ valor → riesgo BAJO) por fase */
    private static final Map<String, double[]> PHASE_THRESHOLDS = Map.of(
            //                         BAJO    MODERADO
            "inconsciente", new double[]{65.0,   40.0},
            "reactivo",     new double[]{72.0,   48.0},
            "consciente",   new double[]{78.0,   55.0},
            "pleno",        new double[]{85.0,   65.0}
    );

    /** Generador de misión por dimensión más crítica */
    private static final Map<String, String> MISSION_BY_DIM = Map.of(
            "emociones",    "ESTABILIZACION_EMOCIONAL",
            "comunicacion", "COMUNICACION_CONSCIENTE",
            "habitos",      "CONSOLIDACION_HABITOS",
            "tiempos",      "PRESENCIA_CONSCIENTE"
    );

    private static final List<String> CONSCIOUSNESS_LABELS =
            List.of("Plena", "Madura", "Consciente", "Reactiva", "Inconsciente");

    /** Score máximo en escala 1-5 que se considera "perfección sospechosa" en MIRROR */
    private static final int MIRROR_SUSPICION_THRESHOLD = 5;

    // ─── API Pública ───────────────────────────────────────────────────────────

    /**
     * Ejecuta el algoritmo completo y retorna el resultado estructurado.
     *
     * @param answers       lista de respuestas del usuario (questionId + value 1-5)
     * @param milestoneCode hito actual de la familia (W1, M1, M3 … M36)
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public AlgoResult compute(List<EvaluationDtos.AnswerDto> answers, String milestoneCode) {
        log.info("[RISK_ALGO_V1] Iniciando cálculo. Hito: {} | Respuestas: {}",
                milestoneCode, answers == null ? 0 : answers.size());

        String phase = MILESTONE_PHASE.getOrDefault(milestoneCode, "inconsciente");

        // Acumuladores ponderados por dimensión
        Map<String, Double> weightedSum   = new HashMap<>(Map.of("emociones", 0.0, "comunicacion", 0.0, "habitos", 0.0, "tiempos", 0.0));
        Map<String, Double> weightTotal   = new HashMap<>(Map.of("emociones", 0.0, "comunicacion", 0.0, "habitos", 0.0, "tiempos", 0.0));

        List<String> mirrorFlags   = new ArrayList<>();
        List<String> relapseFlags  = new ArrayList<>();
        int mirrorPerfectCount     = 0;
        int mirrorAnsweredCount    = 0;

        if (answers == null || answers.isEmpty()) {
            return buildEmptyResult(phase);
        }

        // ── Carga en lote — 1 sola query para todas las preguntas ────────────
        List<Long> questionIds = answers.stream()
                .map(EvaluationDtos.AnswerDto::questionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Question> questionMap = new HashMap<>();
        questionRepository.findAllById(questionIds)
                .forEach(q -> questionMap.put(q.getId(), q));

        for (EvaluationDtos.AnswerDto a : answers) {
                Question q = questionMap.get(a.questionId());
                if (q == null) continue;

                int rawValue       = a.getEffectiveValue();
                String dim         = normalizeDimension(q.getDimension());
                boolean isMirrorType = "MIRROR".equalsIgnoreCase(q.getType());

                // ── Preguntas de control MIRROR (type="MIRROR") ──────────────
                // Solo detectan simulación de respuestas; NO contribuyen al ICF.
                // Una respuesta perfecta (valor 5) en ≥60% de estas preguntas
                // activa la bandera simulationSuspected.
                if (isMirrorType) {
                    mirrorAnsweredCount++;
                    if (rawValue >= MIRROR_SUSPICION_THRESHOLD) {
                        mirrorPerfectCount++;
                        mirrorFlags.add(q.getQuestionKey() != null ? q.getQuestionKey() : "Q-" + q.getId());
                        log.debug("[RISK_ALGO_V1] MIRROR sospechoso: {} (valor={})", q.getQuestionKey(), rawValue);
                    }
                    continue; // no contribuye al ICF
                }

                // ── Normalización 0-100 ──────────────────────────────────────
                // reverseQuestion=true → escala invertida: valor alto = peor salud.
                //   Ej.: "¿Gritas cuando te frustras?" — un 5 es malo, no bueno.
                // direction=NEGATIVE → mismo efecto (campo legado).
                // Ambos contribuyen al ICF; solo cambia la dirección de la escala.
                double normScore;
                if (q.isReverseQuestion() || "NEGATIVE".equalsIgnoreCase(q.getDirection())) {
                    normScore = ((5.0 - rawValue) / 4.0) * 100.0;
                } else {
                    normScore = ((rawValue - 1.0) / 4.0) * 100.0;
                }

                // ── Peso clínico = severityWeight × boost de hito ───────────
                double sw        = q.getSeverityWeight() != null ? q.getSeverityWeight() : 1.0;
                double hiteBoost = milestoneCode != null && milestoneCode.equals(q.getMilestoneCode()) ? 1.5 : 1.0;
                double effectiveWeight = sw * hiteBoost;

                weightedSum.merge(dim, normScore * effectiveWeight, Double::sum);
                weightTotal.merge(dim, effectiveWeight, Double::sum);

                // ── Detección de recaída ─────────────────────────────────────
                if (q.isDetectsRelapse() && rawValue <= 2) {
                    String key = q.getQuestionKey() != null ? q.getQuestionKey() : "Q-" + q.getId();
                    relapseFlags.add(key + " [dim=" + dim + ", val=" + rawValue + "]");
                    log.warn("[RISK_ALGO_V1] Señal de recaída detectada: {}", key);
                }
        }

        // ── Puntuaciones finales por dimensión ───────────────────────────────
        Map<String, Double> dimensionScores = new LinkedHashMap<>();
        for (String dim : DIMENSIONS) {
            double total = weightTotal.getOrDefault(dim, 0.0);
            double score = total > 0.0
                    ? weightedSum.getOrDefault(dim, 0.0) / total
                    : 100.0; // Sin datos → sin riesgo declarado
            dimensionScores.put(dim, Math.round(score * 100.0) / 100.0);
        }

        // ── ICF ponderado ────────────────────────────────────────────────────
        double icf = DIMENSIONS.stream()
                .mapToDouble(dim -> dimensionScores.get(dim) * DIM_WEIGHTS.get(dim))
                .sum();
        icf = Math.round(icf * 100.0) / 100.0;

        // ── Clasificación de riesgo con umbrales adaptativos por fase ────────
        String riskLevel = classifyRisk(icf, dimensionScores, phase);

        // ── Dimensión más crítica ────────────────────────────────────────────
        String criticalDim = DIMENSIONS.stream()
                .min(Comparator.comparingDouble(dimensionScores::get))
                .orElse("emociones");

        // ── Simulación sospechada: > 60% de las MIRROR con puntuación perfecta
        boolean simulationSuspected = mirrorAnsweredCount > 0
                && ((double) mirrorPerfectCount / mirrorAnsweredCount) > 0.6;

        // ── Nivel de consciencia ─────────────────────────────────────────────
        int consciousnessInt   = computeConsciousnessLevel(icf);
        String consciousnessLbl = CONSCIOUSNESS_LABELS.get(consciousnessInt - 1);

        // ── Misión sugerida ───────────────────────────────────────────────────
        String missionGen = MISSION_BY_DIM.getOrDefault(criticalDim, "ESTABILIZACION_EMOCIONAL");

        // ── IF-SUM: Vector de incertidumbre estructural ───────────────────────
        int nonMirrorProcessed = answers.size() - mirrorAnsweredCount;

        // U_o: cuestionario incompleto (esperado: 18 preguntas no-MIRROR)
        double uObservational = nonMirrorProcessed >= 18 ? 0.05
                : Math.max(0.0, 1.0 - (nonMirrorProcessed / 18.0));

        // U_s: sospecha de simulación (escala: ratio perfecto → 0.0-0.80)
        double uSemantic = mirrorAnsweredCount > 0
                ? Math.min(0.80, (double) mirrorPerfectCount / mirrorAnsweredCount * 0.80)
                : 0.05;

        // U_c: ambigüedad de contexto (decrece conforme la familia avanza)
        double uContextual = switch (phase) {
            case "inconsciente" -> 0.30;
            case "reactivo"     -> 0.20;
            case "consciente"   -> 0.10;
            case "pleno"        -> 0.05;
            default             -> 0.25;
        };

        // U_i: contradicción interna — ICF aparentemente saludable pero señales de recaída
        double uInferential = (!relapseFlags.isEmpty() && icf >= 65.0)
                ? Math.min(0.60, relapseFlags.size() * 0.12)
                : 0.05;

        // U_t: variabilidad temporal base (enriquecible post-hoc con días desde última eval)
        double uTemporal = 0.10;

        double uTotal = Math.min(1.0, Math.round(
                (uSemantic * 0.25 + uContextual * 0.15 + uTemporal * 0.15
                 + uObservational * 0.25 + uInferential * 0.20) * 100.0) / 100.0);

        UncertaintyVector uncertainty = new UncertaintyVector(
                Math.round(uSemantic      * 100.0) / 100.0,
                Math.round(uContextual    * 100.0) / 100.0,
                uTemporal,
                Math.round(uObservational * 100.0) / 100.0,
                Math.round(uInferential   * 100.0) / 100.0,
                uTotal
        );

        AlgoResult result = new AlgoResult(
                dimensionScores,
                icf,
                riskLevel,
                criticalDim,
                simulationSuspected,
                !relapseFlags.isEmpty(),
                missionGen,
                consciousnessLbl,
                consciousnessInt,
                relapseFlags,
                mirrorFlags,
                uncertainty
        );

        log.info("[RISK_ALGO_V1] Resultado: ICF={} | Riesgo={} | CritDim={} | Fase={} | " +
                "Simulacion={} | Recaida={} | Mision={} | Incertidumbre={}({})",
                icf, riskLevel, criticalDim, phase,
                simulationSuspected, !relapseFlags.isEmpty(), missionGen,
                uncertainty.total(), uncertainty.level());

        return result;
    }

    // ─── Helpers privados ──────────────────────────────────────────────────────

    private String classifyRisk(double icf, Map<String, Double> scores, String phase) {
        double[] th = PHASE_THRESHOLDS.getOrDefault(phase, PHASE_THRESHOLDS.get("inconsciente"));
        double thLow = th[0], thMod = th[1];

        String base;
        if (icf >= thLow)      base = "BAJO";
        else if (icf >= thMod) base = "MODERADO";
        else if (icf >= 25.0)  base = "ALTO";
        else                   base = "CRITICO";

        // Regla de seguridad crítica — cualquier dimensión en colapso sube el nivel
        boolean anyUnder25 = scores.values().stream().anyMatch(s -> s < 25.0);
        boolean anyUnder40 = scores.values().stream().anyMatch(s -> s < 40.0);

        if (anyUnder25) return "CRITICO";
        if (anyUnder40 && ("BAJO".equals(base) || "MODERADO".equals(base))) return "ALTO";

        return base;
    }

    private int computeConsciousnessLevel(double icf) {
        if (icf >= 85.0) return 1; // Plena
        if (icf >= 70.0) return 2; // Madura
        if (icf >= 55.0) return 3; // Consciente
        if (icf >= 35.0) return 4; // Reactiva
        return 5;                   // Inconsciente
    }

    /** Resultado neutral cuando no hay respuestas — evita ICF inflado artificialmente */
    private AlgoResult buildEmptyResult(String phase) {
        Map<String, Double> empty = new LinkedHashMap<>();
        DIMENSIONS.forEach(d -> empty.put(d, 50.0)); // Score neutro, no perfecto
        // Alta incertidumbre observacional: cuestionario vacío
        UncertaintyVector neutralU = new UncertaintyVector(0.05, 0.25, 0.10, 1.00, 0.05, 0.40);
        return new AlgoResult(empty, 50.0, "MODERADO", "emociones",
                false, false, "ESTABILIZACION_EMOCIONAL", "Reactiva", 4,
                List.of(), List.of(), neutralU);
    }

    private String normalizeDimension(String raw) {
        if (raw == null) return "emociones";
        String d = raw.toLowerCase().trim();
        return DIMENSIONS.contains(d) ? d : "emociones";
    }

    // ─── Resultado ─────────────────────────────────────────────────────────────

    /**
     * IF-SUM: Vector de incertidumbre estructural (0.0 – 1.0 por componente).
     *
     * La incertidumbre es una propiedad ontológica del dominio emocional, no ruido.
     * total > 0.40 → alta; total > 0.50 → reduce la proyección de riesgo.
     *
     * Componentes:
     *   semantic      — múltiples interpretaciones posibles (simulación detectada)
     *   contextual    — ambigüedad de fase/hito (inconsciente tiene más)
     *   temporal      — variabilidad emocional (base 0.10; enriquecible post-hoc)
     *   observational — señales incompletas (cuestionario parcialmente respondido)
     *   inferential   — contradicción interna (ICF alto con señales de recaída)
     */
    public record UncertaintyVector(
            double semantic,
            double contextual,
            double temporal,
            double observational,
            double inferential,
            double total
    ) {
        /** true si la incertidumbre total es tan alta que reduce la proyección de riesgo. */
        public boolean reducesRisk() { return total > 0.50; }
        /** true si la incertidumbre merece advertencia narrativa al usuario. */
        public boolean isHigh()      { return total > 0.40; }
        public String level() {
            if (total < 0.15) return "LOW";
            if (total < 0.35) return "MEDIUM";
            return "HIGH";
        }
    }

    /**
     * Resultado inmutable del algoritmo RISK_ALGO_V1.
     *
     * @param dimensionScores           score 0-100 por dimensión (ponderado por severityWeight)
     * @param healthyIndex              ICF global 0-100
     * @param riskLevel                 BAJO | MODERADO | ALTO | CRITICO
     * @param criticalDimension         dimensión con menor puntuación
     * @param simulationSuspected       >60% de preguntas MIRROR con valor 5 (perfección irreal)
     * @param relapseDetected           al menos una pregunta detectsRelapse con valor ≤ 2
     * @param suggestedMissionGenerator misión automática recomendada para el plan
     * @param consciousnessLabel        etiqueta del nivel de consciencia familiar
     * @param consciousnessLevel        nivel 1 (Plena) – 5 (Inconsciente)
     * @param relapseFlags              claves de preguntas que dispararon alerta de recaída
     * @param mirrorFlags               claves de preguntas MIRROR con respuesta perfecta sospechosa
     * @param uncertainty               IF-SUM: vector estructural de incertidumbre del diagnóstico
     */
    public record AlgoResult(
            Map<String, Double> dimensionScores,
            double healthyIndex,
            String riskLevel,
            String criticalDimension,
            boolean simulationSuspected,
            boolean relapseDetected,
            String suggestedMissionGenerator,
            String consciousnessLabel,
            int consciousnessLevel,
            List<String> relapseFlags,
            List<String> mirrorFlags,
            UncertaintyVector uncertainty
    ) {
        public boolean hasCrisis() {
            return "CRITICO".equals(riskLevel) || "ALTO".equals(riskLevel);
        }

        public String summary() {
            return String.format("ICF=%.1f | %s | crit=%s | sim=%s | relapse=%s",
                    healthyIndex, riskLevel, criticalDimension,
                    simulationSuspected, relapseDetected);
        }
    }
}
