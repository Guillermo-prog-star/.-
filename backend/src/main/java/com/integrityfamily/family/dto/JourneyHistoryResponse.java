package com.integrityfamily.family.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Historial de snapshots del viaje familiar, ordenado cronológicamente.
 * Se usa para renderizar el gráfico de evolución en el frontend.
 */
public record JourneyHistoryResponse(
    Long familyId,
    String familyName,
    List<SnapshotPoint> points,
    int totalLevelUps,
    LocalDate firstSnapshotDate,
    LocalDate lastSnapshotDate
) {
    public record SnapshotPoint(
        LocalDate date,
        int level,
        int progress,
        boolean levelUp,
        Integer previousLevel   // null si no es level-up
    ) {}
}
