package com.integrityfamily.family.dto;

import java.time.LocalDateTime;

/**
 * Resumen ejecutivo de salud familiar para el encabezado del dashboard.
 * Agrega en una sola llamada: ICF, viaje, radar de señales y sprint activo.
 */
public record FamilyHealthSummaryResponse(

    Long familyId,
    String familyName,

    // ── ICF ───────────────────────────────────────────────────────────────
    Double currentIcf,          // último ICF calculado (null si sin diagnóstico)
    Double icfDelta30d,         // cambio respecto a hace 30 días
    String icfLabel,            // "Fortaleza" | "Creciendo" | "Atención" | "Crítico"
    String icfDirection,        // IMPROVING | STABLE | DECLINING | CRITICAL_DECLINE | NO_DATA

    // ── Nivel de riesgo ───────────────────────────────────────────────────
    String riskLevel,           // BAJO | MODERADO | ALTO | CRITICO
    boolean sentinelActive,

    // ── Radar de evolución ────────────────────────────────────────────────
    String evolutionPhase,      // inconsciente | reactivo | consciente | pleno
    int highSignalCount,        // señales activas de severidad HIGH

    // ── Viaje familiar ────────────────────────────────────────────────────
    int journeyCurrentLevel,
    int journeyProgress,        // 0-100
    String journeyNextAction,

    // ── Sprint activo ─────────────────────────────────────────────────────
    boolean hasActiveSprint,
    String activeSprintStatus,  // null si no hay sprint
    Long activeSprintId,

    // ── Métricas rápidas ──────────────────────────────────────────────────
    long memberCount,
    long evidenceCount,
    long totalSprints,

    LocalDateTime generatedAt

) {
    public static String icfLabel(Double icf) {
        if (icf == null) return "Sin datos";
        if (icf >= 80) return "Fortaleza";
        if (icf >= 60) return "Creciendo";
        if (icf >= 40) return "Atención";
        return "Crítico";
    }
}
