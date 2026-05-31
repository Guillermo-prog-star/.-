package com.integrityfamily.common.event;

import java.time.LocalDateTime;

/**
 * Evento de dominio: Crisis familiar registrada.
 *
 * Cuando se publica este evento, el sistema en cascada debe:
 *   1. Recalcular ICF (Motor Inferencial)
 *   2. Ajustar planes adaptativos (AdaptivePlanService)
 *   3. Elevar prioridad del Consultor IA
 *   4. Actualizar estado longitudinal familiar
 *   5. Notificar Dashboard y portal
 */
public record FamilyCrisisEvent(
        Long familyId,
        Long criticalDayId,
        String category,
        String emotion,
        String description,
        LocalDateTime occurredAt
) {
    public static FamilyCrisisEvent of(Long familyId, Long criticalDayId,
                                       String category, String emotion, String description) {
        return new FamilyCrisisEvent(familyId, criticalDayId, category, emotion, description, LocalDateTime.now());
    }
}
