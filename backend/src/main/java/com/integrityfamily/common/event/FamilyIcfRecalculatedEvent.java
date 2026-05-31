package com.integrityfamily.common.event;

import java.time.LocalDateTime;

/**
 * Evento de dominio: ICF recalculado.
 *
 * Notifica a Dashboard, Consultor IA y Portal de Transformación
 * que el índice de convivencia familiar ha cambiado.
 * Incluye las dimensiones individuales para actualización granular.
 */
public record FamilyIcfRecalculatedEvent(
        Long familyId,
        double previousIcf,
        double newIcf,
        String previousRiskLevel,
        String newRiskLevel,
        double emociones,
        double comunicacion,
        double habitos,
        double tiempos,
        String trigger,   // CRISIS | ASSESSMENT | JOURNAL | ADAPTIVE
        LocalDateTime occurredAt
) {
    public boolean riskEscalated() {
        return riskWeight(newRiskLevel) > riskWeight(previousRiskLevel);
    }

    public boolean riskImproved() {
        return riskWeight(newRiskLevel) < riskWeight(previousRiskLevel);
    }

    private int riskWeight(String level) {
        return switch (level == null ? "" : level) {
            case "BAJO"     -> 1;
            case "MODERADO" -> 2;
            case "MEDIO"    -> 2;
            case "ALTO"     -> 3;
            case "CRITICO"  -> 4;
            default          -> 0;
        };
    }
}
