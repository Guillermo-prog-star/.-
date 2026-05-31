package com.integrityfamily.risk.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.domain.FamilyLongitudinalState;
import com.integrityfamily.risk.service.FamilyCausalEngine;
import com.integrityfamily.risk.service.FamilyCausalEngine.CausalInferenceResult;
import com.integrityfamily.risk.service.LongitudinalStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API del Estado Longitudinal Familiar y Motor Inferencial Causal.
 *
 * Resuelve GAP 2 y GAP 3: expone el estado longitudinal y el motor causal
 * para que el Dashboard, el Portal y el Consultor IA puedan consumirlos.
 *
 * Endpoints:
 *   GET  /api/families/{id}/longitudinal-state   → estado vivo de la familia
 *   GET  /api/families/{id}/causal-inference      → resultado del Motor Inferencial (R1-R7)
 *   POST /api/families/{id}/causal-inference      → fuerza re-inferencia causal
 */
@Slf4j
@RestController
@RequestMapping("/api/families")
@RequiredArgsConstructor
public class LongitudinalStateController {

    private final LongitudinalStateService longitudinalStateService;
    private final FamilyCausalEngine causalEngine;
    private final com.integrityfamily.domain.repository.RiskSnapshotRepository riskSnapshotRepository;
    private final com.integrityfamily.domain.repository.EvaluationRepository evaluationRepository;

    /**
     * Estado Longitudinal — la memoria estructural de la familia.
     *
     * El Dashboard consume este endpoint para mostrar:
     *   - Trayectoria ICF (hoy vs 30d atrás)
     *   - Tendencia: IMPROVING / STABLE / DETERIORATING
     *   - Fase evolutiva: RECONOCIMIENTO → AMOR → ENTREGA
     *   - Señales de crisis activa y deterioro sostenido
     */
    @GetMapping("/{familyId}/longitudinal-state")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<LongitudinalStateDto> getLongitudinalState(@PathVariable Long familyId) {
        log.info("📊 [LONGITUDINAL-API] Estado longitudinal solicitado para familia {}", familyId);
        FamilyLongitudinalState state = longitudinalStateService.getState(familyId);
        return ApiResponse.ok(LongitudinalStateDto.from(state));
    }

    /**
     * Inferencia Causal — resultado del Motor Inferencial con explicabilidad.
     *
     * El Dashboard y el Consultor IA consumen esto para mostrar:
     *   - Qué reglas causales están activas (R1-R7)
     *   - Por qué el riesgo es el que es (explicabilidad)
     *   - Si requiere intervención inmediata
     */
    @GetMapping("/{familyId}/causal-inference")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<CausalInferenceResult> getCausalInference(@PathVariable Long familyId) {
        log.info("⚙️ [CAUSAL-API] Evaluación causal solicitada para familia {}", familyId);
        CausalInferenceResult result = causalEngine.evaluate(familyId);
        return ApiResponse.ok(result);
    }

    /**
     * Fuerza re-inferencia causal completa y persiste el resultado.
     * Útil después de eventos importantes (diagnóstico, crisis resuelta).
     */
    @PostMapping("/{familyId}/causal-inference")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<CausalInferenceResult> triggerCausalInference(@PathVariable Long familyId) {
        log.info("⚙️ [CAUSAL-API] Re-inferencia causal forzada para familia {}", familyId);
        CausalInferenceResult result = causalEngine.infer(familyId);
        return ApiResponse.ok(result);
    }

    /**
     * Sincroniza el estado longitudinal desde las evaluaciones existentes.
     *
     * Útil para familias que ya tienen historial de evaluaciones pero cuyo
     * FamilyLongitudinalState fue creado recientemente con valores por defecto.
     * Llamar una vez para "arrancar" la memoria estructural con datos reales.
     *
     * Proceso:
     *   1. Lee el último RiskSnapshot de la familia
     *   2. Lee la última Evaluation finalizada y sus dimensiones
     *   3. Llama a LongitudinalStateService.syncFromSnapshot()
     *   4. Dispara re-inferencia causal completa
     */
    @PostMapping("/{familyId}/longitudinal-state/sync")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<String> syncLongitudinalStateFromHistory(@PathVariable Long familyId) {
        log.info("🔄 [LONGITUDINAL-SYNC] Sincronizando desde historial para familia {}", familyId);
        try {
            // 1. La fuente canónica de ICF es la Evaluation (RiskAlgoV1Engine con pesos clínicos)
            //    NO el RiskSnapshot (que puede venir de la fórmula simple)
            var lastEvalOpt = evaluationRepository.findFirstByFamilyIdAndStatusOrderByFinalizedAtDesc(
                    familyId, com.integrityfamily.domain.EvaluationStatus.FINALIZED);

            if (lastEvalOpt.isEmpty()) {
                return ApiResponse.ok("Sin evaluaciones finalizadas — estado longitudinal mantiene valores por defecto.");
            }
            var lastEval = lastEvalOpt.get();

            // 2. ICF del algoritmo clínico (fuente de verdad)
            double icf = lastEval.getIcf() != null ? lastEval.getIcf() : 50.0;

            // 3. Nivel de riesgo normalizado
            String riskLevel = lastEval.getRiskLevel() != null
                    ? normalizeRiskLevel(lastEval.getRiskLevel())
                    : "MODERADO";

            // 4. Dimensiones de la última evaluación
            double emociones    = 50.0;
            double comunicacion = 50.0;
            double habitos      = 50.0;
            double tiempos      = 50.0;

            if (lastEval.getDimensionScores() != null) {
                for (var ds : lastEval.getDimensionScores()) {
                    String name = ds.getDimensionName() != null ? ds.getDimensionName().toLowerCase() : "";
                    double score = ds.getScore() != null ? ds.getScore() : 50.0;
                    if (name.contains("emoci"))  emociones    = score;
                    if (name.contains("comuni")) comunicacion = score;
                    if (name.contains("habit"))  habitos      = score;
                    if (name.contains("tiempo")) tiempos      = score;
                }
            }

            // También usar snapshot para comparar ICF previo (delta longitudinal)
            var snapshots = riskSnapshotRepository.findByFamilyIdOrderByCreatedAtDesc(familyId);
            var latest = snapshots.isEmpty() ? null : snapshots.get(0);


            // 5. Sincronizar estado longitudinal con ICF del algoritmo clínico
            longitudinalStateService.syncFromSnapshot(
                    familyId, icf, riskLevel,
                    emociones, comunicacion, habitos, tiempos
            );

            // 6. Re-inferencia causal con datos reales
            var result = causalEngine.infer(familyId);
            log.info("[LONGITUDINAL-SYNC] Familia {} → ICF={} | {} | Fase={} | Reglas={}",
                    familyId, String.format("%.1f", icf), result.inferredRiskLevel(),
                    result.evolutionPhase(), result.activeRules());

            return ApiResponse.ok(String.format(
                    "Estado longitudinal sincronizado. ICF=%.1f (RiskAlgoV1) | %s | Fase=%s | dim=[E=%.0f,C=%.0f,H=%.0f,T=%.0f] | Reglas: %s",
                    icf, result.inferredRiskLevel(), result.evolutionPhase(),
                    emociones, comunicacion, habitos, tiempos,
                    result.activeRules().isEmpty() ? "ninguna (sistema estable)" : result.activeRules()));

        } catch (Exception e) {
            log.error("[LONGITUDINAL-SYNC] Error: {}", e.getMessage());
            return ApiResponse.ok("Error en sincronización: " + e.getMessage());
        }
    }

    /**
     * Resumen compacto para el Dashboard (campos mínimos necesarios).
     * Más liviano que el estado completo — para polling frecuente.
     */
    @GetMapping("/{familyId}/longitudinal-summary")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<Map<String, Object>> getLongitudinalSummary(@PathVariable Long familyId) {
        FamilyLongitudinalState state = longitudinalStateService.getState(familyId);
        Map<String, Object> summary = Map.of(
                "icfCurrent",              state.getIcfCurrent() != null ? state.getIcfCurrent() : 50.0,
                "icfDelta30d",             state.icfDelta30d(),
                "riskTrend",               state.getRiskTrend() != null ? state.getRiskTrend() : "STABLE",
                "currentRiskLevel",        state.getCurrentRiskLevel() != null ? state.getCurrentRiskLevel() : "MODERADO",
                "evolutionPhase",          state.getEvolutionPhase() != null ? state.getEvolutionPhase() : "inconsciente",
                "narrativeStage",          state.getNarrativeStage() != null ? state.getNarrativeStage() : "RECONOCIMIENTO",
                "crisisCount30d",          state.getCrisisCount30d() != null ? state.getCrisisCount30d() : 0,
                "consecutiveDeteriorations", state.getConsecutiveDeteriorations() != null ? state.getConsecutiveDeteriorations() : 0,
                "communicationCollapse",   Boolean.TRUE.equals(state.getCommunicationCollapseActive()),
                "inActiveCrisis",          state.isInActiveCrisis()
        );
        return ApiResponse.ok(summary);
    }

    // ── Helpers privados ─────────────────────────────────────────────────────

    /**
     * Normaliza los niveles de riesgo de distintas fuentes al estándar interno.
     * RiskAlgoV1: BAJO | MODERADO | ALTO | CRITICO
     * RiskService: BAJO | MEDIO | ALTO | CRITICO
     * Frontend:    LOW | MEDIUM | HIGH | CRITICAL
     */
    private String normalizeRiskLevel(String raw) {
        if (raw == null) return "MODERADO";
        return switch (raw.toUpperCase().trim()) {
            case "LOW",    "BAJO"     -> "BAJO";
            case "MEDIUM", "MEDIO",
                 "MODERADO"          -> "MODERADO";
            case "HIGH",   "ALTO"    -> "ALTO";
            case "CRITICAL","CRITICO" -> "CRITICO";
            default                   -> "MODERADO";
        };
    }

    // ── DTO de respuesta ──────────────────────────────────────────────────────

    public record LongitudinalStateDto(
            Long familyId,
            Double icfCurrent,
            Double icfDelta30d,
            String riskTrend,
            String currentRiskLevel,
            Double dimEmociones,
            Double dimComunicacion,
            Double dimHabitos,
            Double dimTiempos,
            String criticalDimension,
            Integer crisisCount30d,
            Integer crisisCountTotal,
            Integer consecutiveDeteriorations,
            Integer consecutiveImprovements,
            Boolean communicationCollapseActive,
            String evolutionPhase,
            String narrativeStage,
            Integer consciousnessLevel,
            String consciousnessLabel,
            Double planAdherencePercent,
            Integer inactivityDays,
            Boolean inActiveCrisis,
            Boolean hasEmotionalDeterioration,
            Boolean isImprovingTrend
    ) {
        public static LongitudinalStateDto from(FamilyLongitudinalState s) {
            return new LongitudinalStateDto(
                    s.getFamily() != null ? s.getFamily().getId() : null,
                    s.getIcfCurrent(),
                    s.icfDelta30d(),
                    s.getRiskTrend(),
                    s.getCurrentRiskLevel(),
                    s.getDimEmociones(),
                    s.getDimComunicacion(),
                    s.getDimHabitos(),
                    s.getDimTiempos(),
                    s.getCriticalDimension(),
                    s.getCrisisCount30d(),
                    s.getCrisisCountTotal(),
                    s.getConsecutiveDeteriorations(),
                    s.getConsecutiveImprovements(),
                    s.getCommunicationCollapseActive(),
                    s.getEvolutionPhase(),
                    s.getNarrativeStage(),
                    s.getConsciousnessLevel(),
                    s.getConsciousnessLabel(),
                    s.getPlanAdherencePercent(),
                    s.getInactivityDays(),
                    s.isInActiveCrisis(),
                    s.hasEmotionalDeterioration(),
                    s.isImprovingTrend()
            );
        }
    }
}
