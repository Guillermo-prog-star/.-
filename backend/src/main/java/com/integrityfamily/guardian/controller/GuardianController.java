package com.integrityfamily.guardian.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.guardian.dto.*;
import com.integrityfamily.guardian.service.GuardianService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * GuardianController — API del Guardián Familiar.
 *
 * Endpoints públicos (autenticado):
 *   GET  /api/families/{familyId}/guardian          → estado del guardián
 *   POST /api/families/{familyId}/guardian/vote     → votar por un miembro
 *   POST /api/families/{familyId}/guardian/confirm  → confirmar guardián directamente
 *   POST /api/families/{familyId}/guardian/missions → activar misión
 *   POST /api/families/{familyId}/guardian/missions/{missionId}/complete → completar
 *   GET  /api/families/{familyId}/guardian/missions → historial de misiones
 */
@RestController
@RequestMapping("/api/families/{familyId}/guardian")
@RequiredArgsConstructor
public class GuardianController {

    private final GuardianService guardianService;

    @GetMapping
    public ApiResponse<GuardianStatusResponse> getStatus(
            @PathVariable Long familyId,
            @RequestParam(required = false) Long memberId) {
        return ApiResponse.ok(guardianService.getStatus(familyId, memberId));
    }

    @PostMapping("/vote")
    public ApiResponse<GuardianStatusResponse> vote(
            @PathVariable Long familyId,
            @RequestBody VoteRequest request) {
        return ApiResponse.ok(guardianService.vote(familyId, request));
    }

    @PostMapping("/confirm")
    public ApiResponse<GuardianStatusResponse> confirm(
            @PathVariable Long familyId,
            @RequestParam Long memberId) {
        return ApiResponse.ok(guardianService.confirmGuardian(familyId, memberId));
    }

    @PostMapping("/missions")
    public ApiResponse<MissionDto> activateMission(
            @PathVariable Long familyId,
            @RequestBody ActivateMissionRequest request) {
        return ApiResponse.ok(guardianService.activateMission(familyId, request));
    }

    @PostMapping("/missions/{missionId}/complete")
    public ApiResponse<MissionDto> completeMission(
            @PathVariable Long familyId,
            @PathVariable Long missionId,
            @RequestParam Long guardianMemberId) {
        return ApiResponse.ok(guardianService.completeMission(familyId, missionId, guardianMemberId));
    }

    @GetMapping("/missions")
    public ApiResponse<List<MissionDto>> getMissions(@PathVariable Long familyId) {
        return ApiResponse.ok(guardianService.getMissions(familyId));
    }
}
