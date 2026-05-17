package com.integrityfamily.risk.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.domain.CriticalDay;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.dto.CreateCrisisRequest;
import com.integrityfamily.risk.service.CrisisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;

/**
 * SDD ALIGNMENT: Controlador sincronizado con el Protocolo Sentinel.
 * Ofrece soporte para registrar crisis y recuperar historial, resolviendo la compatibilidad con la UI.
 */
@RestController
@RequestMapping("/api/crisis")
@RequiredArgsConstructor
public class CrisisController {

    private final CrisisService crisisService;

    /**
     * POST /api/crisis/report — Registra una crisis e invoca al Mentor IA para contención.
     */
    @PostMapping("/report")
    @PreAuthorize("@familySecurity.check(#request.familyId)")
    public ApiResponse<CriticalDay> reportCrisis(@Valid @RequestBody CreateCrisisRequest request) {
        CriticalDay response = crisisService.registerCrisis(
                request.familyId(),
                null, // memberId (opcional)
                request.category(),
                request.description(),
                request.emotion()
        );
        return ApiResponse.ok(response, "Protocolo de ayuda Sentinel activado.");
    }

    /**
     * GET /api/crisis/family/{familyId} — Recupera el historial de días críticos/crisis de una familia.
     */
    @GetMapping("/family/{familyId}")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<List<CriticalDay>> getHistory(@PathVariable Long familyId) {
        List<CriticalDay> history = crisisService.getHistory(familyId);
        return ApiResponse.ok(history, "Historial de contención recuperado.");
    }

    /**
     * POST /api/crisis/protocol/activate — Forzar activación de protocolo.
     */
    @PostMapping("/protocol/activate")
    @PreAuthorize("@familySecurity.check(#body['familyId'])")
    public ApiResponse<Void> activate(@RequestBody Map<String, Object> body) {
        Long familyId = ((Number) body.get("familyId")).longValue();
        String reason = (String) body.get("reason");

        crisisService.activateProtocol(familyId, reason);
        return ApiResponse.ok(null);
    }

    /**
     * POST /api/crisis/FamilyMember/handle — Solicitud genérica de membresía de crisis.
     */
    @PostMapping("/FamilyMember/handle")
    @PreAuthorize("@familySecurity.check(#body['familyId'])")
    public ApiResponse<Void> handleMemberCrisis(@RequestBody Map<String, Object> body) {
        Long familyId = ((Number) body.get("familyId")).longValue();
        String observation = (String) body.get("observation");
        List<FamilyMember> involvedMembers = (List<FamilyMember>) body.get("involvedMembers");

        crisisService.handleMemberCrisis(familyId, involvedMembers, observation);
        return ApiResponse.ok(null);
    }

    /**
     * GET /api/crisis/status/{familyId} — Estado actual de crisis de la familia.
     */
    @GetMapping("/status/{familyId}")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<Boolean> getStatus(@PathVariable Long familyId) {
        return ApiResponse.ok(crisisService.isUnderCrisis(familyId));
    }
}
