package com.integrityfamily.family.dto;

import java.util.List;

/**
 * Estado del viaje familiar a través de los 13 niveles del ecosistema Integrity Family.
 *
 * Cada nivel representa una etapa de madurez real del sistema familiar:
 *   0 - Plataforma (siempre activo)
 *   1 - Identidad familiar
 *   2 - Miembros
 *   3 - Guardián familiar
 *   4 - ADN familiar
 *   5 - Diagnóstico vivo
 *   6 - Plan familiar
 *   7 - Misiones
 *   8 - Sprint
 *   9 - Daily
 *  10 - Evidencias
 *  11 - Consultor IA
 *  12 - Consejo familiar
 *  13 - Legado
 */
public record FamilyJourneyResponse(

    Long familyId,
    String familyName,

    /** Nivel actual alcanzado (0-13). El más alto nivel completado. */
    int currentLevel,

    /** Porcentaje de completitud total del viaje (0-100). */
    int journeyProgress,

    /** Detalle de cada nivel. */
    List<JourneyLevel> levels,

    /** El siguiente paso concreto que debe dar la familia. */
    String nextAction,

    /** Nivel que contiene el siguiente paso. */
    int nextLevel

) {

    public record JourneyLevel(
        int level,
        String name,
        String description,
        JourneyStatus status,   // COMPLETE | IN_PROGRESS | LOCKED | NEXT
        String statusLabel,     // texto para mostrar en UI
        String icon,
        String route,           // ruta Angular para navegar a este nivel
        String metric           // dato clave de progreso (ej: "3 miembros", "ICF 67")
    ) {}

    public enum JourneyStatus {
        COMPLETE,     // tiene datos reales
        IN_PROGRESS,  // iniciado pero incompleto
        NEXT,         // el próximo que debería completar
        LOCKED        // no desbloqueado aún
    }
}
