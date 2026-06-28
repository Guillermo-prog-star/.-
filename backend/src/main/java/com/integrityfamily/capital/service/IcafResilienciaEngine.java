package com.integrityfamily.capital.service;

import com.integrityfamily.capital.service.CriticalEventLifecycleService.ResilienceMetrics;
import com.integrityfamily.domain.FamilyLongitudinalState;
import com.integrityfamily.domain.repository.FamilyCriticalEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Calcula el score del dominio resiliencia (0-100) del ICaF.
 *
 * Fórmula en 4 componentes:
 *
 *   A. Tasa de resolución (40%)
 *      resolved / total × 100
 *      "¿Qué porcentaje de crisis han sido resueltas?"
 *
 *   B. Velocidad de recuperación (30%)
 *      Penaliza por cada 30 días adicionales vs umbral de 60 días.
 *      Una familia que resuelve en 30 días obtiene puntaje máximo;
 *      una que tarda 180+ días obtiene 0 en este componente.
 *
 *   C. Control de recaídas (20%)
 *      Penaliza por recaídas relativas al número de eventos.
 *      0 recaídas = 100; ≥1 recaída por evento = 0.
 *
 *   D. Carga activa inversa (10%)
 *      Penaliza por eventos sin resolver actualmente.
 *      Sin eventos activos = 100; ≥3 activos = 0.
 *
 * Fallback: si la familia no tiene ningún evento en family_critical_events,
 * usa estimación desde FamilyLongitudinalState (igual que S2) para no
 * penalizar familias nuevas con ICaF = 0 en resiliencia.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IcafResilienciaEngine {

    private static final double OPTIMAL_DAYS_RESOLUTION = 60.0;   // 60 días = puntaje máximo en velocidad
    private static final double MAX_DAYS_RESOLUTION     = 180.0;  // 180+ días = 0 en velocidad

    private final FamilyCriticalEventRepository criticalEventRepo;

    /**
     * Calcula el score de resiliencia para una familia (0-100).
     * Si no hay datos en FamilyCriticalEvent, usa el fallback longitudinal.
     */
    public double compute(Long familyId, FamilyLongitudinalState state) {
        long totalEvents = criticalEventRepo.countByFamilyIdAndStatus(familyId, "DETECTED")
                         + criticalEventRepo.countByFamilyIdAndStatus(familyId, "IN_PROGRESS")
                         + criticalEventRepo.countByFamilyIdAndStatus(familyId, "RESOLVED")
                         + criticalEventRepo.countByFamilyIdAndStatus(familyId, "CLOSED")
                         + criticalEventRepo.countByFamilyIdAndStatus(familyId, "RELAPSED");

        if (totalEvents == 0) {
            return fallbackFromLongitudinal(state);
        }

        long resolved   = criticalEventRepo.countByFamilyIdAndStatus(familyId, "RESOLVED")
                        + criticalEventRepo.countByFamilyIdAndStatus(familyId, "CLOSED");
        long active     = criticalEventRepo.countActiveByFamilyId(familyId);
        double avgDays  = criticalEventRepo.avgDaysToResolutionByFamilyId(familyId);
        long relapses   = criticalEventRepo.totalRelapsesByFamilyId(familyId);

        double scoreA = computeResolutionRate(resolved, totalEvents);    // 0-100
        double scoreB = computeRecoverySpeed(avgDays);                   // 0-100
        double scoreC = computeRelapseControl(relapses, totalEvents);    // 0-100
        double scoreD = computeActiveLoad(active);                       // 0-100

        double score = scoreA * 0.40 + scoreB * 0.30 + scoreC * 0.20 + scoreD * 0.10;
        double result = Math.round(score * 100.0) / 100.0;

        log.debug("[ICaF-Resiliencia] Familia {} | resRate={} veloc={} recaidas={} carga={} → score={}",
                familyId,
                String.format("%.1f", scoreA), String.format("%.1f", scoreB),
                String.format("%.1f", scoreC), String.format("%.1f", scoreD),
                String.format("%.1f", result));

        return result;
    }

    // ── Componentes ───────────────────────────────────────────────────────────

    /** A: % de eventos resueltos sobre el total */
    private double computeResolutionRate(long resolved, long total) {
        if (total == 0) return 100.0;
        return (double) resolved / total * 100.0;
    }

    /**
     * B: velocidad de recuperación.
     * ≤ 60 días → 100. Entre 60 y 180 → lineal decreciente. > 180 → 0.
     * Sin resoluciones aún (avgDays = 0) → score neutro 50.
     */
    private double computeRecoverySpeed(double avgDays) {
        if (avgDays <= 0) return 50.0; // sin resoluciones aún: neutral
        if (avgDays <= OPTIMAL_DAYS_RESOLUTION) return 100.0;
        if (avgDays >= MAX_DAYS_RESOLUTION)     return 0.0;
        return (MAX_DAYS_RESOLUTION - avgDays) / (MAX_DAYS_RESOLUTION - OPTIMAL_DAYS_RESOLUTION) * 100.0;
    }

    /**
     * C: control de recaídas.
     * 0 recaídas → 100. Penaliza por ratio recaídas/eventos (max 1 recaída por evento = 0).
     */
    private double computeRelapseControl(long relapses, long total) {
        if (total == 0 || relapses == 0) return 100.0;
        double ratio = Math.min((double) relapses / total, 1.0);
        return (1.0 - ratio) * 100.0;
    }

    /**
     * D: carga activa inversa.
     * 0 activos → 100. 1 activo → 70. 2 → 40. ≥3 → 0.
     */
    private double computeActiveLoad(long active) {
        return switch ((int) Math.min(active, 3)) {
            case 0  -> 100.0;
            case 1  -> 70.0;
            case 2  -> 40.0;
            default -> 0.0;
        };
    }

    /**
     * Fallback para familias sin eventos críticos registrados.
     * Usa la estimación de S2 basada en crisis del estado longitudinal.
     */
    private double fallbackFromLongitudinal(FamilyLongitudinalState state) {
        if (state == null) return 50.0;
        int crisisRecientes = state.getCrisisCount30d() != null ? state.getCrisisCount30d() : 0;
        double base = "IMPROVING".equals(state.getRiskTrend()) ? 70.0 : 55.0;
        double score = base - (crisisRecientes * 10.0);
        int mejoras = state.getConsecutiveImprovements() != null ? state.getConsecutiveImprovements() : 0;
        score += Math.min(mejoras * 3.0, 15.0);
        return Math.max(0.0, Math.min(100.0, score));
    }
}
