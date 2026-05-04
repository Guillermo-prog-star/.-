package com.integrityfamily.common.event;

import java.time.LocalDateTime;

/**
 * SDD: Sobre de Transporte Universal para Eventos de Dominio.
 */
public record SystemEvent(
    String routingKey,       // e.g., "family.created", "members.updated"
    Long familyId,
    Object payload,          // El DTO o entidad relevante
    String triggeredBy,      // Email del usuario que causó el evento
    LocalDateTime timestamp
) {
    public static SystemEvent of(String routingKey, Long familyId, Object payload, String user) {
        return new SystemEvent(routingKey, familyId, payload, user, LocalDateTime.now());
    }
}
