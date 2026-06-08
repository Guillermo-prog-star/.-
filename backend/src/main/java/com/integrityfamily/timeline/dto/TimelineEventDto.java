package com.integrityfamily.timeline.dto;

import java.time.LocalDateTime;

/**
 * Evento unificado del Timeline Familiar.
 * Cada entidad del sistema (evaluación, gratitud, bitácora, misión, crisis, ADN)
 * se convierte en un TimelineEventDto para ser consumido por el frontend.
 */
public record TimelineEventDto(
    Long id,
    EventType type,
    String title,
    String description,
    String actor,     // Quién generó el evento (nombre del miembro o "Familia")
    String emotion,   // Emoción asociada si aplica
    String metadata,  // Info extra: ICF, dimensión crítica, etc.
    LocalDateTime occurredAt
) {
    public enum EventType {
        EVALUATION,    // Diagnóstico completado
        GRATITUDE,     // Gratitud enviada
        LOGBOOK,       // Bitácora de transformación registrada
        EVIDENCE,      // Evidencia de misión subida
        CRISIS,        // Crisis registrada
        MISSION,       // Misión del sprint completada
        DNA,           // ADN sintetizado o actualizado
        MEMBER_JOINED  // Nuevo miembro se unió
    }
}
