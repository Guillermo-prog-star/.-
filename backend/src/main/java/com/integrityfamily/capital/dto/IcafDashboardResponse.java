package com.integrityfamily.capital.dto;

import java.util.List;

/**
 * DTO del dashboard ICaF — contiene todo lo que necesita el frontend para
 * renderizar el panel de Capital Familiar: índice global, 11 dominios,
 * trayectoria longitudinal y métricas de eventos críticos.
 */
public record IcafDashboardResponse(

        Long familyId,

        // ── Índice global ─────────────────────────────────────────────────────
        double icaf,
        int madurezNivel,
        String madurezLabel,
        String trend,               // IMPROVING | STABLE | DECLINING

        // ── Trayectoria longitudinal ──────────────────────────────────────────
        Double icaf6mAgo,
        Double icaf12mAgo,
        Double icaf36mAgo,

        // ── 11 dominios ───────────────────────────────────────────────────────
        List<DomainScore> domains,

        // ── Eventos críticos ──────────────────────────────────────────────────
        long activeEvents,
        long resolvedEvents,
        double resolutionRate,
        double avgDaysToResolution,
        long totalRelapses,

        // ── Metadatos ─────────────────────────────────────────────────────────
        String lastCalculatedAt,
        boolean hasRealData         // false = solo estimaciones (sin evaluaciones aún)

) {

    /** Un dominio del ICaF con su score, peso y estado de datos */
    public record DomainScore(
            String key,             // cohesion | confianza | resiliencia | ...
            String label,           // "Cohesión" | "Confianza" | ...
            double score,           // 0-100
            double weight,          // 0.0-1.0
            boolean isEstimated,    // true = estimación, false = datos reales
            String source           // "ICF" | "CUESTIONARIO" | "EVENTOS" | "ESTIMADO"
    ) {}

    // ── Factory method ────────────────────────────────────────────────────────

    public static List<DomainScore> buildDomains(
            double cohesion, double confianza, double resiliencia,
            double comunicacion, double autonomia, double bienestar,
            double proposito, double integracion, double emprendimiento,
            double legado, double madurezScore,
            boolean hasConfianzaData, boolean hasBienestarData, boolean hasResilienciaData) {

        return List.of(
            new DomainScore("cohesion",       "Cohesión",       cohesion,       0.20, false,              "ICF"),
            new DomainScore("confianza",      "Confianza",      confianza,      0.12, !hasConfianzaData,  hasConfianzaData  ? "CUESTIONARIO" : "ESTIMADO"),
            new DomainScore("resiliencia",    "Resiliencia",    resiliencia,    0.12, !hasResilienciaData,hasResilienciaData ? "EVENTOS"      : "ESTIMADO"),
            new DomainScore("comunicacion",   "Comunicación",   comunicacion,   0.10, true,               "ICF"),
            new DomainScore("autonomia",      "Autonomía",      autonomia,      0.08, true,               "ESTIMADO"),
            new DomainScore("bienestar",      "Bienestar",      bienestar,      0.08, !hasBienestarData,  hasBienestarData  ? "CUESTIONARIO" : "ESTIMADO"),
            new DomainScore("proposito",      "Propósito",      proposito,      0.08, true,               "ESTIMADO"),
            new DomainScore("integracion",    "Integración",    integracion,    0.07, true,               "ESTIMADO"),
            new DomainScore("emprendimiento", "Emprendimiento", emprendimiento, 0.05, true,               "ESTIMADO"),
            new DomainScore("legado",         "Legado",         legado,         0.05, true,               "ESTIMADO"),
            new DomainScore("madurez",        "Madurez",        madurezScore,   0.05, false,              "ICF")
        );
    }
}
