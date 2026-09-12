package com.integrityfamily.transformation.controller;

import com.integrityfamily.transformation.domain.TransformationState;
import com.integrityfamily.transformation.domain.TransformationState.OnboardingStep;
import com.integrityfamily.transformation.service.TransformationStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * TransformationStateController — API REST del estado de transformación familiar.
 *
 * GET  /api/families/{familyId}/transformation          → estado actual
 * PATCH /api/families/{familyId}/transformation/onboarding → avanzar onboarding
 * PATCH /api/families/{familyId}/transformation/month   → avanzar mes
 * PATCH /api/families/{familyId}/transformation/sprint  → establecer sprint
 * PATCH /api/families/{familyId}/transformation/mission → establecer misión activa
 */
@RestController
@RequestMapping("/api/families/{familyId}/transformation")
@RequiredArgsConstructor
public class TransformationStateController {

    private final TransformationStateService service;

    @PreAuthorize("@familySecurity.check(#familyId)")
    @GetMapping
    public ResponseEntity<TransformationState> getState(@PathVariable Long familyId) {
        return ResponseEntity.ok(service.getOrCreate(familyId));
    }

    @PreAuthorize("@familySecurity.check(#familyId)")
    @PatchMapping("/onboarding")
    public ResponseEntity<TransformationState> advanceOnboarding(
            @PathVariable Long familyId,
            @RequestBody Map<String, String> body) {
        OnboardingStep step = OnboardingStep.valueOf(body.get("step").toUpperCase());
        return ResponseEntity.ok(service.advanceOnboarding(familyId, step));
    }

    @PreAuthorize("@familySecurity.check(#familyId)")
    @PatchMapping("/month")
    public ResponseEntity<TransformationState> advanceMonth(
            @PathVariable Long familyId,
            @RequestBody Map<String, Integer> body) {
        return ResponseEntity.ok(service.advanceMonth(familyId, body.get("month")));
    }

    @PreAuthorize("@familySecurity.check(#familyId)")
    @PatchMapping("/sprint")
    public ResponseEntity<TransformationState> setSprint(
            @PathVariable Long familyId,
            @RequestBody Map<String, Integer> body) {
        return ResponseEntity.ok(service.setSprint(familyId, body.get("sprintNumber")));
    }

    @PreAuthorize("@familySecurity.check(#familyId)")
    @PatchMapping("/mission")
    public ResponseEntity<TransformationState> setMission(
            @PathVariable Long familyId,
            @RequestBody Map<String, Long> body) {
        return ResponseEntity.ok(service.setActiveMission(familyId, body.get("missionId")));
    }
}
