package com.integrityfamily.vitality.dto;

import java.time.LocalDate;

public class VitalityDtos {

    /** Todos los campos son opcionales — se sobreescriben solo los presentes (ADR-009, Decisión 1). */
    public record RegisterLogRequest(
            LocalDate logDate,
            Double sleepHours,
            Integer sleepQuality,
            Integer exerciseMinutes,
            Integer nutritionQuality,
            Integer screenTimeBeforeBedMinutes,
            Integer fatigueLevel
    ) {}

    public record DailyVitalityLogDto(
            Long id,
            Long familyMemberId,
            LocalDate logDate,
            Double sleepHours,
            Integer sleepQuality,
            Integer exerciseMinutes,
            Integer nutritionQuality,
            Integer screenTimeBeforeBedMinutes,
            Integer fatigueLevel,
            String source
    ) {}

    /**
     * semaphore es una vista calculada, nunca persistida (ADR-009, Decisión 3).
     * recoveryIndex es null cuando no hay ningún dato en la ventana solicitada
     * — en ese caso tampoco se escribe fila en hypothesis_evidence.
     */
    public record RecoveryIndexResponse(
            Long familyMemberId,
            int windowDays,
            Double recoveryIndex,
            String semaphore
    ) {}
}
