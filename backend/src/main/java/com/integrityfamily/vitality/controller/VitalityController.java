package com.integrityfamily.vitality.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.vitality.domain.DailyVitalityLog;
import com.integrityfamily.vitality.dto.VitalityDtos.DailyVitalityLogDto;
import com.integrityfamily.vitality.dto.VitalityDtos.RecoveryIndexResponse;
import com.integrityfamily.vitality.dto.VitalityDtos.RegisterLogRequest;
import com.integrityfamily.vitality.service.RecoveryIndexService;
import com.integrityfamily.vitality.service.VitalityService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * VitalityController — Fase 4 (base biológica), ADR-009.
 *
 * GET  /api/families/{familyId}/members/{memberId}/vitality/recovery-index no dispara
 * ninguna acción automática (ADR-009, Decisión 4) — solo calcula y devuelve el índice.
 */
@RestController
@RequestMapping("/api/families/{familyId}/members/{memberId}/vitality")
@RequiredArgsConstructor
public class VitalityController {

    private final VitalityService vitalityService;
    private final RecoveryIndexService recoveryIndexService;

    @PostMapping
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<DailyVitalityLogDto> registerLog(
            @PathVariable Long familyId,
            @PathVariable Long memberId,
            @RequestBody RegisterLogRequest request) {
        return ApiResponse.ok(toDto(vitalityService.registerLog(familyId, memberId, request)));
    }

    @GetMapping
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<List<DailyVitalityLogDto>> listLogs(
            @PathVariable Long familyId,
            @PathVariable Long memberId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(vitalityService.listByMember(familyId, memberId, from, to).stream()
                .map(this::toDto)
                .toList());
    }

    @GetMapping("/recovery-index")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<RecoveryIndexResponse> getRecoveryIndex(
            @PathVariable Long familyId,
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "7") int windowDays) {
        Double index = recoveryIndexService.calculateAndRecord(familyId, memberId, windowDays);
        String semaphore = recoveryIndexService.semaphore(index);
        return ApiResponse.ok(new RecoveryIndexResponse(memberId, windowDays, index, semaphore));
    }

    private DailyVitalityLogDto toDto(DailyVitalityLog log) {
        return new DailyVitalityLogDto(
                log.getId(),
                log.getFamilyMember().getId(),
                log.getLogDate(),
                log.getSleepHours(),
                log.getSleepQuality(),
                log.getExerciseMinutes(),
                log.getNutritionQuality(),
                log.getScreenTimeBeforeBedMinutes(),
                log.getFatigueLevel(),
                log.getSource());
    }
}
