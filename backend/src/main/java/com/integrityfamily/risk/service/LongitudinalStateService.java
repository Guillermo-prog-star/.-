package com.integrityfamily.risk.service;

import com.integrityfamily.common.event.*;
import com.integrityfamily.capital.service.IcafScoringEngine;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyLongitudinalState;
import com.integrityfamily.domain.repository.FamilyLongitudinalStateRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Servicio de Estado Longitudinal Familiar.
 *
 * Mantiene actualizado el FamilyLongitudinalState reaccionando
 * a todos los eventos del bus familiar de forma asíncrona.
 *
 * Es el componente que implementa la memoria estructural del sistema:
 *   sin él, el Consultor IA responde sin historial (FALLA 2).
 *   con él, cada inferencia IA tiene contexto longitudinal real.
 *
 * Principio: este servicio SOLO actualiza el estado longitudinal.
 * Las decisiones de riesgo las toma FamilyCausalEngine.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LongitudinalStateService {

    private final FamilyLongitudinalStateRepository longitudinalRepo;
    private final FamilyRepository familyRepository;
    private final FamilyCausalEngine causalEngine;

    // ── Reacciones a eventos del bus ──────────────────────────────────────────

    /**
     * Crisis registrada → actualiza contadores y marca crisis activa.
     * Luego dispara re-inferencia causal completa.
     */
    @Async
    @EventListener
    @Transactional
    public void onCrisisTriggered(FamilyCrisisEvent event) {
        log.warn("[LONGITUDINAL] Procesando {} para familia {}",
                EventTopics.CRISIS_TRIGGERED, event.familyId());

        FamilyLongitudinalState state = getOrCreate(event.familyId());

        // Incrementar contadores de crisis
        state.setCrisisCount30d(  (state.getCrisisCount30d()    != null ? state.getCrisisCount30d()    : 0) + 1);
        state.setCrisisCountTotal((state.getCrisisCountTotal()   != null ? state.getCrisisCountTotal()  : 0) + 1);
        state.setLastCrisisAt(LocalDateTime.now());

        // Reset mejoras consecutivas — la crisis rompe el streak positivo
        state.setConsecutiveImprovements(0);

        longitudinalRepo.save(state);

        // Re-inferencia causal post-crisis
        try {
            FamilyCausalEngine.CausalInferenceResult result = causalEngine.infer(event.familyId());
            log.warn("[LONGITUDINAL] Post-crisis → {}", result.summary());
        } catch (Exception e) {
            log.error("[LONGITUDINAL] Error en re-inferencia post-crisis: {}", e.getMessage());
        }
    }

    /**
     * ICF recalculado → sincroniza valores en estado longitudinal.
     * Actualiza icfCurrent, dimensiones y riesgo.
     */
    @Async
    @EventListener
    @Transactional
    public void onIcfRecalculated(FamilyIcfRecalculatedEvent event) {
        log.info("[LONGITUDINAL] Procesando {} para familia {} | ICF: {} → {}",
                EventTopics.ICF_RECALCULATED, event.familyId(),
                String.format("%.1f", event.previousIcf()),
                String.format("%.1f", event.newIcf()));

        FamilyLongitudinalState state = getOrCreate(event.familyId());

        // Rotar histórico: current → 30d si hay diferencia temporal
        if (state.getIcfCurrent() != null && !state.getIcfCurrent().equals(event.newIcf())) {
            state.setIcf30dAgo(state.getIcfCurrent());
        }

        state.setIcfCurrent(event.newIcf());
        state.setCurrentRiskLevel(event.newRiskLevel());

        // Sincronizar dimensiones si vienen en el evento
        if (event.emociones() > 0)    state.setDimEmociones(event.emociones());
        if (event.comunicacion() > 0) state.setDimComunicacion(event.comunicacion());
        if (event.habitos() > 0)      state.setDimHabitos(event.habitos());
        if (event.tiempos() > 0)      state.setDimTiempos(event.tiempos());

        longitudinalRepo.save(state);
    }

    /**
     * Entrada de bitácora → actualiza señales emocionales longitudinales.
     * Incrementa/resetea contadores de deterioración/mejora consecutiva.
     */
    @Async
    @EventListener
    @Transactional
    public void onJournalEntryAdded(FamilyJournalEntryEvent event) {
        FamilyLongitudinalState state = getOrCreate(event.familyId());

        state.setLastJournalAt(LocalDateTime.now());
        state.setInactivityDays(0); // Hay actividad — reset inactividad

        if (event.indicatesDeterioration()) {
            // Acumular deterioraciones consecutivas
            int det = (state.getConsecutiveDeteriorations() != null ? state.getConsecutiveDeteriorations() : 0) + 1;
            state.setConsecutiveDeteriorations(det);
            state.setConsecutiveImprovements(0);

            log.warn("[LONGITUDINAL] Deterioración #{} acumulada. Familia: {} | Dimensión: {} | Mood: {}/5",
                    det, event.familyId(), event.riskDimension(), event.moodAfter());

            // Si hay patrón de deterioro sostenido → disparar re-inferencia
            if (det >= 3) {
                log.warn("[LONGITUDINAL] PATRÓN DE DETERIORO SOSTENIDO (≥3) → re-inferencia causal. Familia: {}",
                        event.familyId());
                tryReInference(event.familyId());
            }

        } else if (event.indicatesImprovement()) {
            // Acumular mejoras consecutivas
            int imp = (state.getConsecutiveImprovements() != null ? state.getConsecutiveImprovements() : 0) + 1;
            state.setConsecutiveImprovements(imp);
            state.setConsecutiveDeteriorations(0);

            log.info("[LONGITUDINAL] Mejora #{} acumulada. Familia: {} | Dimensión: {} | Mood: {}/5",
                    imp, event.familyId(), event.riskDimension(), event.moodAfter());

        } else {
            // Entrada neutral — reset parcial de racha negativa
            if (state.getConsecutiveDeteriorations() != null && state.getConsecutiveDeteriorations() > 0) {
                state.setConsecutiveDeteriorations(Math.max(0, state.getConsecutiveDeteriorations() - 1));
            }
        }

        longitudinalRepo.save(state);
    }

    /**
     * ICaF recalculado → actualiza trayectoria longitudinal ICaF.
     * Rota histórico (6m, 12m, 36m) y actualiza madurez y tendencia.
     */
    @Async
    @EventListener
    @Transactional
    public void onIcafRecalculated(FamilyIcafRecalculatedEvent event) {
        log.info("[LONGITUDINAL] ICaF {} → {} | Madurez {} → {} | Familia {} | trigger={}",
                String.format("%.1f", event.previousIcaf()),
                String.format("%.1f", event.newIcaf()),
                event.previousMadurez(), event.newMadurez(),
                event.familyId(), event.trigger());

        FamilyLongitudinalState state = getOrCreate(event.familyId());

        // Rotar histórico longitudinal
        rotateIcafHistory(state, event.newIcaf());

        state.setIcafMadurez(event.newMadurez());
        state.setIcafTrend(event.trend());
        state.setIcafLastCalculated(java.time.LocalDateTime.now());

        longitudinalRepo.save(state);

        if (event.madurezImproved()) {
            log.info("[LONGITUDINAL] Familia {} alcanzó Nivel {} ({}) de Capital Familiar",
                    event.familyId(), event.newMadurez(),
                    IcafScoringEngine.madurezLabel(event.newMadurez()));
        }
    }

    /**
     * Rota el historial ICaF: current → 6m → 12m → 36m.
     * Solo rota si hay un valor previo distinto (evita duplicar en recálculos rápidos).
     */
    private void rotateIcafHistory(FamilyLongitudinalState state, double newIcaf) {
        Double current = state.getIcafCurrent();
        if (current == null) {
            state.setIcafCurrent(newIcaf);
            return;
        }
        if (Double.compare(current, newIcaf) == 0) return;

        // Rotar: current → 6m si 6m está vacío o es más antiguo
        if (state.getIcaf6mAgo() == null) {
            state.setIcaf6mAgo(current);
        } else if (state.getIcaf12mAgo() == null) {
            state.setIcaf12mAgo(state.getIcaf6mAgo());
            state.setIcaf6mAgo(current);
        } else {
            state.setIcaf36mAgo(state.getIcaf12mAgo());
            state.setIcaf12mAgo(state.getIcaf6mAgo());
            state.setIcaf6mAgo(current);
        }

        state.setIcafCurrent(newIcaf);
    }

    // ── API de consulta ───────────────────────────────────────────────────────

    /**
     * Retorna el estado longitudinal actual de una familia.
     * Si no existe, lo crea con valores por defecto.
     */
    @Transactional
    public FamilyLongitudinalState getState(Long familyId) {
        return getOrCreate(familyId);
    }

    /**
     * Fuerza actualización del ICF y las dimensiones desde un snapshot externo.
     * Llamado por RiskService después de calcular un nuevo snapshot.
     */
    @Transactional
    public void syncFromSnapshot(Long familyId, double icf, String riskLevel,
                                  double emociones, double comunicacion,
                                  double habitos, double tiempos) {
        FamilyLongitudinalState state = getOrCreate(familyId);

        // Rotar histórico
        if (state.getIcfCurrent() != null) {
            state.setIcf30dAgo(state.getIcfCurrent());
        }

        state.setIcfCurrent(icf);
        state.setCurrentRiskLevel(riskLevel);
        state.setDimEmociones(emociones);
        state.setDimComunicacion(comunicacion);
        state.setDimHabitos(habitos);
        state.setDimTiempos(tiempos);
        state.setLastAssessmentAt(LocalDateTime.now());

        longitudinalRepo.save(state);
        log.info("[LONGITUDINAL] Estado sincronizado desde snapshot. Familia: {} | ICF: {} | {}",
                familyId, String.format("%.1f", icf), riskLevel);
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    private FamilyLongitudinalState getOrCreate(Long familyId) {
        Optional<FamilyLongitudinalState> existing = longitudinalRepo.findByFamilyId(familyId);
        if (existing.isPresent()) return existing.get();

        // Crear estado inicial
        Family family = familyRepository.findById(familyId).orElse(null);
        if (family == null) {
            log.warn("[LONGITUDINAL] Familia {} no encontrada — estado longitudinal no creado", familyId);
            return FamilyLongitudinalState.builder()
                    .icfCurrent(50.0).icf30dAgo(50.0).currentRiskLevel("MODERADO")
                    .riskTrend("STABLE").evolutionPhase("inconsciente").narrativeStage("RECONOCIMIENTO")
                    .consciousnessLevel(4).consciousnessLabel("Reactiva")
                    .crisisCount30d(0).crisisCountTotal(0)
                    .consecutiveDeteriorations(0).consecutiveImprovements(0)
                    .inactivityDays(0).communicationCollapseActive(false)
                    .build();
        }

        FamilyLongitudinalState newState = FamilyLongitudinalState.builder()
                .family(family)
                .icfCurrent(50.0).icf30dAgo(50.0).icf90dAgo(50.0)
                .currentRiskLevel("MODERADO").riskTrend("STABLE")
                .evolutionPhase("inconsciente").narrativeStage("RECONOCIMIENTO")
                .consciousnessLevel(4).consciousnessLabel("Reactiva")
                .crisisCount30d(0).crisisCountTotal(0)
                .consecutiveDeteriorations(0).consecutiveImprovements(0)
                .inactivityDays(0).communicationCollapseActive(false)
                .build();

        FamilyLongitudinalState saved = longitudinalRepo.save(newState);
        log.info("[LONGITUDINAL] Nuevo estado longitudinal creado para familia {}", familyId);
        return saved;
    }

    private void tryReInference(Long familyId) {
        try {
            causalEngine.infer(familyId);
        } catch (Exception e) {
            log.error("[LONGITUDINAL] Error en re-inferencia por patrón de deterioro: {}", e.getMessage());
        }
    }
}
