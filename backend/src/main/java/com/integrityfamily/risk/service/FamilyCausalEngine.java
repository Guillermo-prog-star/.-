package com.integrityfamily.risk.service;

import com.integrityfamily.domain.FamilyLongitudinalState;
import com.integrityfamily.domain.JournalEntry;
import com.integrityfamily.domain.RiskSnapshot;
import com.integrityfamily.domain.repository.FamilyLongitudinalStateRepository;
import com.integrityfamily.domain.repository.JournalEntryRepository;
import com.integrityfamily.domain.repository.RiskSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Motor Inferencial Causal — Cerebro del sistema vivo de IF.
 *
 * Resuelve el gap crítico: ningún módulo individual puede ver la imagen completa.
 * Este motor correlaciona TODOS los inputs para producir un estado familiar
 * coherente, causal y explicable.
 *
 * Inputs que correlaciona:
 *   ① Historial de RiskSnapshots (tendencia ICF)
 *   ② Entradas de Bitácora (señales emocionales recientes)
 *   ③ Estado de crisis (impacto inmediato y acumulado)
 *   ④ Adherencia al plan (inactividad → deterioro latente)
 *   ⑤ Dimensiones ICF (detecta colapso dimensional específico)
 *
 * Reglas causales que implementa:
 *   R1. 3+ deterioraciones en bitácora en 7 días → escalar riesgo un nivel
 *   R2. Crisis en últimas 48h → forzar CRITICO independientemente del ICF
 *   R3. Tendencia mejora 30+ días → promover fase de evolución
 *   R4. Inactividad ≥ 14 días → penalizar ICF latente
 *   R5. Cualquier dimensión < 25 → CRITICO (regla de seguridad)
 *   R6. Comunicación < 35 con 2+ crisis → COLAPSO_COMUNICACIONAL
 *   R7. ICF mejora > 10pts en 30 días → hito de evolución alcanzado
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FamilyCausalEngine {

    private static final int JOURNAL_WINDOW_DAYS     = 7;
    private static final int DETERIORATION_THRESHOLD = 3;
    private static final int CRISIS_ACTIVE_HOURS     = 48;
    private static final int IMPROVEMENT_WINDOW_DAYS = 30;
    private static final double DIM_CRITICAL_THRESHOLD = 25.0;
    private static final double DIM_COLLAPSE_THRESHOLD  = 35.0;
    private static final double EVOLUTION_DELTA_THRESHOLD = 10.0;
    private static final int INACTIVITY_RISK_DAYS    = 14;

    private final FamilyLongitudinalStateRepository longitudinalRepo;
    private final RiskSnapshotRepository riskSnapshotRepository;
    private final JournalEntryRepository journalEntryRepository;

    // ─── API Pública ──────────────────────────────────────────────────────────

    /**
     * Ejecuta inferencia causal completa para una familia.
     * Actualiza el FamilyLongitudinalState y retorna el resultado con explicabilidad.
     */
    @Transactional
    public CausalInferenceResult infer(Long familyId) {
        log.info("[CAUSAL-ENGINE] Iniciando inferencia causal para familia {}", familyId);

        FamilyLongitudinalState state = getOrCreateState(familyId);
        CausalInferenceResult result = correlateInputs(familyId, state);
        applyToLongitudinalState(state, result);
        longitudinalRepo.save(state);

        log.info("[CAUSAL-ENGINE] Resultado: {} | Tendencia: {} | Fase: {} | Reglas: {}",
                result.inferredRiskLevel(), result.trend(), result.evolutionPhase(),
                result.activeRules());

        return result;
    }

    /**
     * Versión ligera: solo evalúa sin persistir — para consultas del Dashboard.
     */
    @Transactional(readOnly = true)
    public CausalInferenceResult evaluate(Long familyId) {
        FamilyLongitudinalState state = getOrCreateStateReadOnly(familyId);
        return correlateInputs(familyId, state);
    }

    // ─── Correlación de inputs ────────────────────────────────────────────────

    private CausalInferenceResult correlateInputs(Long familyId, FamilyLongitudinalState state) {
        CausalContext ctx = buildContext(familyId, state);
        return applyRules(ctx, state);
    }

    private CausalContext buildContext(Long familyId, FamilyLongitudinalState state) {
        // ① Historial de snapshots para tendencia
        List<RiskSnapshot> recentSnapshots = riskSnapshotRepository
                .findByFamilyIdOrderByCreatedAtDesc(familyId)
                .stream().limit(10).toList();

        // ② Entradas de bitácora recientes
        LocalDateTime windowStart = LocalDateTime.now().minusDays(JOURNAL_WINDOW_DAYS);
        List<JournalEntry> recentJournals = journalEntryRepository
                .findByFamilyIdOrderByCreatedAtDesc(familyId)
                .stream()
                .filter(j -> j.getCreatedAt() != null && j.getCreatedAt().isAfter(windowStart))
                .toList();

        // ③ Métricas del estado longitudinal
        double icfCurrent   = state.getIcfCurrent()  != null ? state.getIcfCurrent()  : 50.0;
        double icf30d       = state.getIcf30dAgo()   != null ? state.getIcf30dAgo()   : 50.0;
        int crisisCount     = state.getCrisisCount30d()   != null ? state.getCrisisCount30d()   : 0;
        int inactivityDays  = state.getInactivityDays()   != null ? state.getInactivityDays()   : 0;
        boolean activeCrisis = state.isInActiveCrisis();

        // ④ Conteo de deterioraciones/mejoras en la ventana de 7 días
        long deteriorations = recentJournals.stream()
                .filter(j -> j.getMoodAfter() != null && j.getMoodAfter() <= 2).count();
        long improvements   = recentJournals.stream()
                .filter(j -> j.getMoodAfter() != null && j.getMoodAfter() >= 4).count();
        long commNegative   = recentJournals.stream()
                .filter(j -> j.getMoodAfter() != null && j.getMoodAfter() <= 2
                          && "comunicacion".equalsIgnoreCase(j.getRiskDimension())).count();

        // ⑤ Dimensiones actuales
        double dimEmociones    = state.getDimEmociones()    != null ? state.getDimEmociones()    : 50.0;
        double dimComunicacion = state.getDimComunicacion() != null ? state.getDimComunicacion() : 50.0;
        double dimHabitos      = state.getDimHabitos()      != null ? state.getDimHabitos()      : 50.0;
        double dimTiempos      = state.getDimTiempos()      != null ? state.getDimTiempos()      : 50.0;

        return new CausalContext(
                familyId, icfCurrent, icf30d, crisisCount, inactivityDays, activeCrisis,
                (int) deteriorations, (int) improvements, (int) commNegative,
                dimEmociones, dimComunicacion, dimHabitos, dimTiempos,
                recentSnapshots.size()
        );
    }

    private CausalInferenceResult applyRules(CausalContext ctx, FamilyLongitudinalState state) {
        java.util.List<String> activeRules = new java.util.ArrayList<>();
        java.util.List<String> explanations = new java.util.ArrayList<>();
        String inferredRisk = state.getCurrentRiskLevel() != null ? state.getCurrentRiskLevel() : "MODERADO";

        // ── R2: Crisis activa → CRITICO (prioridad máxima) ───────────────────
        if (ctx.activeCrisis()) {
            inferredRisk = "CRITICO";
            activeRules.add("R2:ACTIVE_CRISIS");
            explanations.add("Crisis registrada en las últimas 48h — riesgo forzado a CRITICO.");
        }

        // ── R5: Seguridad dimensional — cualquier dim < 25 → CRITICO ─────────
        if (ctx.dimEmociones() < DIM_CRITICAL_THRESHOLD ||
            ctx.dimComunicacion() < DIM_CRITICAL_THRESHOLD ||
            ctx.dimHabitos() < DIM_CRITICAL_THRESHOLD ||
            ctx.dimTiempos() < DIM_CRITICAL_THRESHOLD) {
            inferredRisk = "CRITICO";
            activeRules.add("R5:DIM_CRITICAL");
            explanations.add(String.format(
                "Dimensión en colapso crítico (< %.0f%%) — seguridad sistémica activada.", DIM_CRITICAL_THRESHOLD));
        }

        // ── R1: 3+ deterioraciones en 7 días → escalar un nivel ──────────────
        if (ctx.deteriorationsLast7d() >= DETERIORATION_THRESHOLD && !"CRITICO".equals(inferredRisk)) {
            inferredRisk = escalateRisk(inferredRisk);
            activeRules.add("R1:JOURNAL_DETERIORATION_PATTERN");
            explanations.add(String.format(
                "%d entradas de bitácora con deterioro emocional en 7 días — riesgo escalado a %s.",
                ctx.deteriorationsLast7d(), inferredRisk));
        }

        // ── R6: Colapso comunicacional ────────────────────────────────────────
        boolean commCollapse = ctx.dimComunicacion() < DIM_COLLAPSE_THRESHOLD && ctx.crisisCount30d() >= 2;
        if (commCollapse) {
            activeRules.add("R6:COMMUNICATION_COLLAPSE");
            explanations.add(String.format(
                "Comunicación al %.0f%% + %d crisis en 30 días — colapso comunicacional activo.",
                ctx.dimComunicacion(), ctx.crisisCount30d()));
        }

        // ── R4: Inactividad → penalización latente ────────────────────────────
        if (ctx.inactivityDays() >= INACTIVITY_RISK_DAYS && "BAJO".equals(inferredRisk)) {
            inferredRisk = "MODERADO";
            activeRules.add("R4:INACTIVITY_LATENT_RISK");
            explanations.add(String.format(
                "%d días sin actividad — riesgo latente; ICF puede no reflejar realidad actual.",
                ctx.inactivityDays()));
        }

        // ── Tendencia ICF ─────────────────────────────────────────────────────
        double delta = ctx.icfCurrent() - ctx.icf30dAgo();
        String trend;
        if (delta > 5.0)       trend = "IMPROVING";
        else if (delta < -5.0) trend = "DETERIORATING";
        else                   trend = "STABLE";

        // ── R3: Mejora sostenida → promover fase de evolución ─────────────────
        boolean evolutionMilestone = false;
        if (delta >= EVOLUTION_DELTA_THRESHOLD) {
            activeRules.add("R3:EVOLUTION_MILESTONE");
            explanations.add(String.format(
                "ICF mejoró %.1f puntos en 30 días — hito de evolución familiar alcanzado.", delta));
            evolutionMilestone = true;
        }

        // ── Fase de evolución ─────────────────────────────────────────────────
        String evolutionPhase = deriveEvolutionPhase(ctx.icfCurrent(), trend, ctx.crisisCount30d());

        // ── Narrativa ─────────────────────────────────────────────────────────
        String narrativeStage = deriveNarrativeStage(evolutionPhase);

        // ── Nivel de consciencia ──────────────────────────────────────────────
        int consciousnessLevel = deriveConsciousnessLevel(ctx.icfCurrent());
        String consciousnessLabel = deriveConsciousnessLabel(consciousnessLevel);

        // ── Dimensión más crítica ─────────────────────────────────────────────
        Map<String, Double> dims = Map.of(
                "emociones",    ctx.dimEmociones(),
                "comunicacion", ctx.dimComunicacion(),
                "habitos",      ctx.dimHabitos(),
                "tiempos",      ctx.dimTiempos()
        );
        String criticalDim = dims.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("emociones");

        return new CausalInferenceResult(
                ctx.familyId(),
                ctx.icfCurrent(),
                inferredRisk,
                trend,
                evolutionPhase,
                narrativeStage,
                consciousnessLevel,
                consciousnessLabel,
                criticalDim,
                commCollapse,
                evolutionMilestone,
                activeRules,
                explanations
        );
    }

    // ─── Helpers de clasificación ─────────────────────────────────────────────

    private String escalateRisk(String current) {
        return switch (current) {
            case "BAJO"     -> "MODERADO";
            case "MODERADO" -> "ALTO";
            case "ALTO"     -> "CRITICO";
            default          -> current;
        };
    }

    private String deriveEvolutionPhase(double icf, String trend, int crisis30d) {
        if (crisis30d >= 3 || icf < 30) return "inconsciente";
        if (icf < 50)                   return "reactivo";
        if (icf < 70)                   return "consciente";
        if ("IMPROVING".equals(trend))  return "pleno";
        return "consciente";
    }

    private String deriveNarrativeStage(String phase) {
        return switch (phase) {
            case "inconsciente" -> "RECONOCIMIENTO";
            case "reactivo"     -> "RECONOCIMIENTO";
            case "consciente"   -> "AMOR";
            case "pleno"        -> "ENTREGA";
            default              -> "RECONOCIMIENTO";
        };
    }

    private int deriveConsciousnessLevel(double icf) {
        if (icf >= 85) return 1; // Plena
        if (icf >= 70) return 2; // Madura
        if (icf >= 55) return 3; // Consciente
        if (icf >= 35) return 4; // Reactiva
        return 5;                 // Inconsciente
    }

    private String deriveConsciousnessLabel(int level) {
        return switch (level) {
            case 1 -> "Plena";
            case 2 -> "Madurando";
            case 3 -> "Consciente";
            case 4 -> "Reactiva";
            default -> "Inconsciente";
        };
    }

    // ─── Persistencia del estado longitudinal ────────────────────────────────

    private void applyToLongitudinalState(FamilyLongitudinalState state, CausalInferenceResult r) {
        state.setCurrentRiskLevel(r.inferredRiskLevel());
        state.setRiskTrend(r.trend());
        state.setEvolutionPhase(r.evolutionPhase());
        state.setNarrativeStage(r.narrativeStage());
        state.setConsciousnessLevel(r.consciousnessLevel());
        state.setConsciousnessLabel(r.consciousnessLabel());
        state.setCriticalDimension(r.criticalDimension());
        state.setCommunicationCollapseActive(r.communicationCollapseActive());
    }

    private FamilyLongitudinalState getOrCreateState(Long familyId) {
        return longitudinalRepo.findByFamilyId(familyId)
                .orElseGet(() -> {
                    log.info("[CAUSAL-ENGINE] Creando nuevo FamilyLongitudinalState para familia {}", familyId);
                    FamilyLongitudinalState newState = FamilyLongitudinalState.builder()
                            .icfCurrent(50.0)
                            .icf30dAgo(50.0)
                            .icf90dAgo(50.0)
                            .currentRiskLevel("MODERADO")
                            .riskTrend("STABLE")
                            .evolutionPhase("inconsciente")
                            .narrativeStage("RECONOCIMIENTO")
                            .consciousnessLevel(4)
                            .consciousnessLabel("Reactiva")
                            .crisisCount30d(0)
                            .crisisCountTotal(0)
                            .consecutiveDeteriorations(0)
                            .consecutiveImprovements(0)
                            .inactivityDays(0)
                            .communicationCollapseActive(false)
                            .build();
                    // La familia se debe inyectar externamente antes de guardar
                    // Este estado se guarda solo cuando infer() lo llama con Family ya seteada
                    return newState;
                });
    }

    private FamilyLongitudinalState getOrCreateStateReadOnly(Long familyId) {
        return longitudinalRepo.findByFamilyId(familyId)
                .orElse(FamilyLongitudinalState.builder()
                        .icfCurrent(50.0).icf30dAgo(50.0).currentRiskLevel("MODERADO")
                        .riskTrend("STABLE").evolutionPhase("inconsciente")
                        .crisisCount30d(0).consecutiveDeteriorations(0).inactivityDays(0)
                        .build());
    }

    // ─── Records de resultado ─────────────────────────────────────────────────

    /**
     * Contexto interno de correlación — todos los inputs reunidos.
     */
    public record CausalContext(
            Long familyId,
            double icfCurrent,
            double icf30dAgo,
            int crisisCount30d,
            int inactivityDays,
            boolean activeCrisis,
            int deteriorationsLast7d,
            int improvementsLast7d,
            int commNegativeLast7d,
            double dimEmociones,
            double dimComunicacion,
            double dimHabitos,
            double dimTiempos,
            int snapshotCount
    ) {}

    /**
     * Resultado inmutable de la inferencia causal.
     * Incluye explicabilidad completa: qué reglas se activaron y por qué.
     */
    public record CausalInferenceResult(
            Long familyId,
            double icf,
            String inferredRiskLevel,
            String trend,
            String evolutionPhase,
            String narrativeStage,
            int consciousnessLevel,
            String consciousnessLabel,
            String criticalDimension,
            boolean communicationCollapseActive,
            boolean evolutionMilestoneReached,
            java.util.List<String> activeRules,
            java.util.List<String> explanations
    ) {
        /** Resumen en una línea para logs y dashboard */
        public String summary() {
            return String.format("ICF=%.1f | %s | %s | fase=%s | reglas=%s",
                    icf, inferredRiskLevel, trend, evolutionPhase, activeRules);
        }

        /** true si la situación requiere intervención inmediata */
        public boolean requiresImmediateIntervention() {
            return "CRITICO".equals(inferredRiskLevel) || communicationCollapseActive;
        }
    }
}
