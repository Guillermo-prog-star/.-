package com.integrityfamily.common.event;

import java.time.LocalDateTime;

/**
 * Evento de dominio: ICaF (Índice de Capital Familiar) recalculado.
 *
 * Paralelo a FamilyIcfRecalculatedEvent. Se publica cada vez que
 * IcafScoringEngine produce un nuevo cálculo. Incluye los 11 dominios
 * y el nivel de madurez para actualización granular del estado longitudinal.
 *
 * Triggers: ASSESSMENT | SPRINT_CLOSE | CRITICAL_EVENT | SCHEDULED
 */
public record FamilyIcafRecalculatedEvent(
        Long familyId,
        double previousIcaf,
        double newIcaf,
        int previousMadurez,
        int newMadurez,

        // 11 dominios (0-100 cada uno; 0.0 = no calculado aún)
        double domCohesion,         // Dominio 1 — ICF actual         (peso 20%)
        double domConfianza,        // Dominio 2 — Confianza mutua     (peso 12%)
        double domResiliencia,      // Dominio 3 — Resolución de crisis(peso 12%)
        double domComunicacion,     // Dominio 4 — Calidad comunicación(peso 10%)
        double domAutonomia,        // Dominio 5 — Autonomía responsable(peso 8%)
        double domBienestar,        // Dominio 6 — Bienestar emocional  (peso 8%)
        double domProposito,        // Dominio 7 — Propósito            (peso 8%)
        double domIntegracion,      // Dominio 8 — Participación activa (peso 7%)
        double domEmprendimiento,   // Dominio 9 — Economía y proyectos (peso 5%)
        double domLegado,           // Dominio 10 — Valores e identidad (peso 5%)
        double domMadurezScore,     // Dominio 11 — Nivel evolutivo     (peso 5%)

        String trigger,             // ASSESSMENT | SPRINT_CLOSE | CRITICAL_EVENT | SCHEDULED
        LocalDateTime occurredAt
) {
    /** true si el nivel de madurez subió */
    public boolean madurezImproved() {
        return newMadurez > previousMadurez;
    }

    /** true si el nivel de madurez bajó */
    public boolean madurezDeclined() {
        return newMadurez < previousMadurez;
    }

    /** Delta ICaF: positivo = mejora, negativo = deterioro */
    public double icafDelta() {
        return newIcaf - previousIcaf;
    }

    /** true si el Capital Familiar mejoró significativamente (≥ 5 puntos) */
    public boolean significantImprovement() {
        return icafDelta() >= 5.0;
    }

    /** Tendencia textual basada en el delta */
    public String trend() {
        double delta = icafDelta();
        if (delta >= 2.0)  return "IMPROVING";
        if (delta <= -2.0) return "DECLINING";
        return "STABLE";
    }
}
