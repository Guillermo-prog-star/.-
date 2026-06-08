package com.integrityfamily.context.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Estado contextual unificado de la familia.
 * Sintetiza todas las fuentes del sistema en un único objeto
 * que el dashboard, la IA y cada módulo pueden consumir.
 */
public record FamilyContextDto(
    Long   familyId,
    String familyName,

    // ── Señales de estado ────────────────────────────────────────────────────
    String connectionLevel,    // ALTA | MEDIA | BAJA
    String stressLevel,        // BAJO | MODERADO | ALTO | CRITICO
    String communicationTrend, // MEJORANDO | ESTABLE | DETERIORANDO
    String participationLevel, // ALTA | MEDIA | BAJA
    String overallTrend,       // ASCENDENTE | ESTABLE | DESCENDENTE | CRITICA
    String overallMood,        // CELEBRANDO | CRECIENDO | SERENO | TENSO | EN_CRISIS

    // ── Métricas clave ───────────────────────────────────────────────────────
    Double  icfCurrent,
    String  riskLevel,
    int     daysWithoutActivity,
    int     currentStreak,
    int     activeRitualsCount,
    Double  sprintProgress,

    // ── Alertas y recomendaciones ────────────────────────────────────────────
    List<String> alerts,
    List<String> recommendations,

    LocalDateTime computedAt,
    boolean fresh // true si fue calculado en este request, false si viene del caché
) {}
