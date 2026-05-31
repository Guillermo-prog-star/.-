package com.integrityfamily.common.event;

import java.time.LocalDateTime;

/**
 * Evento de dominio: Entrada de bitácora agregada.
 *
 * La bitácora NO es texto libre — es un timeline emocional versionado.
 * Cada entrada modifica inferencias, riesgo, evolución y consistencia diagnóstica.
 *
 * Señales derivadas:
 *   - moodAfter <= 2     → posible deterioro emocional → revisar riesgo
 *   - moodAfter >= 4     → mejora emocional → actualizar evolución longitudinal
 *   - communicationDrop  → disparar COMMUNICATION_COLLAPSE
 *   - dimension crítica  → priorizar en siguiente plan adaptativo
 */
public record FamilyJournalEntryEvent(
        Long familyId,
        Long journalEntryId,
        String origin,        // TASK | CRISIS | MANUAL | AI
        String riskDimension, // emociones | comunicacion | habitos | tiempos
        String emotion,
        Integer moodAfter,    // 1-5 (1=muy malo, 5=excelente)
        String complianceStatus,
        LocalDateTime occurredAt
) {
    /** true si el estado de ánimo indica deterioro emocional */
    public boolean indicatesDeterioration() {
        return moodAfter != null && moodAfter <= 2;
    }

    /** true si el estado de ánimo indica mejora emocional */
    public boolean indicatesImprovement() {
        return moodAfter != null && moodAfter >= 4;
    }

    /** true si la entrada es de comunicación */
    public boolean isCommunicationRelated() {
        return "comunicacion".equalsIgnoreCase(riskDimension) ||
               "COMUNICACION".equalsIgnoreCase(riskDimension);
    }

    public static FamilyJournalEntryEvent of(Long familyId, Long entryId,
                                              String origin, String dimension,
                                              String emotion, Integer mood, String compliance) {
        return new FamilyJournalEntryEvent(
                familyId, entryId, origin, dimension, emotion, mood, compliance, LocalDateTime.now());
    }
}
