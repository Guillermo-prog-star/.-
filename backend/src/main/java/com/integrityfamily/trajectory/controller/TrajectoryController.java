package com.integrityfamily.trajectory.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.domain.RiskMacrodomain;
import com.integrityfamily.domain.TrajectoryStatus;
import com.integrityfamily.trajectory.dto.TrajectoryDtos.*;
import com.integrityfamily.trajectory.service.SafetyProtocolService;
import com.integrityfamily.trajectory.service.TrajectoryService;
import com.integrityfamily.trajectory.service.TrajectorySuggestionService;
import com.integrityfamily.trajectory.service.TrajectorySuggestionService.TrajectorySuggestion;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trajectories")
@RequiredArgsConstructor
public class TrajectoryController {

    private final TrajectoryService trajectoryService;
    private final TrajectorySuggestionService suggestionService;
    private final SafetyProtocolService safetyProtocolService;

    // ─── Bank endpoints ───────────────────────────────────────────────────────

    @GetMapping("/bank")
    public ApiResponse<TrajectoryBankResponse> getBank() {
        return ApiResponse.ok(trajectoryService.getBank());
    }

    @GetMapping("/bank/{macrodomain}")
    public ApiResponse<List<TrajectoryBankItem>> getBankByMacrodomain(@PathVariable String macrodomain) {
        RiskMacrodomain domain = RiskMacrodomain.valueOf(macrodomain.toUpperCase());
        return ApiResponse.ok(trajectoryService.getBankByMacrodomain(domain));
    }

    // ─── Family trajectory endpoints ──────────────────────────────────────────

    @GetMapping("/family/{familyId}")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<List<FamilyTrajectoryDto>> getFamilyTrajectories(@PathVariable Long familyId) {
        return ApiResponse.ok(trajectoryService.getFamilyTrajectories(familyId));
    }

    @PostMapping("/family/{familyId}/assign")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<FamilyTrajectoryDto> assignTrajectory(
            @PathVariable Long familyId,
            @RequestBody AssignTrajectoryRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        String email = principal != null ? principal.getUsername() : "system";
        return ApiResponse.ok(trajectoryService.assignTrajectory(familyId, request.code(), email, request.notes()));
    }

    @PatchMapping("/family/{id}/status")
    @PreAuthorize("@familySecurity.checkFamilyTrajectory(#id)")
    public ApiResponse<Void> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateStatusRequest request) {
        TrajectoryStatus status = TrajectoryStatus.valueOf(request.status().toUpperCase());
        trajectoryService.updateStatus(id, status, request.notes());
        return ApiResponse.ok(null);
    }

    // ─── Timeline endpoints ───────────────────────────────────────────────────

    @GetMapping("/family/{id}/timeline")
    @PreAuthorize("@familySecurity.checkFamilyTrajectory(#id)")
    public ApiResponse<List<TrajectoryTimelineDto>> getTimeline(@PathVariable Long id) {
        return ApiResponse.ok(trajectoryService.getTimeline(id));
    }

    @PostMapping("/family/{id}/timeline")
    @PreAuthorize("@familySecurity.checkFamilyTrajectory(#id)")
    public ApiResponse<TrajectoryTimelineDto> addTimelineEvent(
            @PathVariable Long id,
            @RequestBody TimelineEventRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        String email = principal != null ? principal.getUsername() : "system";
        return ApiResponse.ok(trajectoryService.addTimelineEvent(id, request, email));
    }

    // ─── Impact indicator endpoints ───────────────────────────────────────────

    @GetMapping("/family/{id}/impact")
    @PreAuthorize("@familySecurity.checkFamilyTrajectory(#id)")
    public ApiResponse<List<TrajectoryImpactDto>> getImpact(@PathVariable Long id) {
        return ApiResponse.ok(trajectoryService.getImpactSummary(id));
    }

    @PostMapping("/family/{id}/indicator")
    @PreAuthorize("@familySecurity.checkFamilyTrajectory(#id)")
    public ApiResponse<TrajectoryImpactDto> upsertIndicator(
            @PathVariable Long id,
            @RequestBody IndicatorRequest request) {
        return ApiResponse.ok(trajectoryService.upsertIndicator(id, request));
    }

    // ─── Protocolo de seguridad ────────────────────────────────────────────────

    @PostMapping("/family/{id}/safety-protocol")
    @PreAuthorize("@familySecurity.checkFamilyTrajectory(#id)")
    public ApiResponse<SafetyProtocolDto> activateSafetyProtocol(
            @PathVariable Long id,
            @RequestBody ActivateSafetyProtocolRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        String email = principal != null ? principal.getUsername() : "system";
        return ApiResponse.ok(safetyProtocolService.activate(id, request, email));
    }

    @GetMapping("/family/{id}/safety-protocol")
    @PreAuthorize("@familySecurity.checkFamilyTrajectory(#id)")
    public ApiResponse<List<SafetyProtocolDto>> getSafetyProtocols(@PathVariable Long id) {
        return ApiResponse.ok(safetyProtocolService.getByFamilyTrajectory(id));
    }

    @PostMapping("/family/{id}/safety-protocol/{activationId}/close")
    @PreAuthorize("@familySecurity.canCloseSafetyProtocol(#id)")
    public ApiResponse<SafetyProtocolDto> closeSafetyProtocol(
            @PathVariable Long id,
            @PathVariable Long activationId,
            @RequestBody CloseSafetyProtocolRequest request) {
        return ApiResponse.ok(safetyProtocolService.close(id, activationId, request));
    }

    // ─── Sugerencias automáticas ──────────────────────────────────────────────

    @GetMapping("/family/{familyId}/suggestions")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<List<TrajectorySuggestion>> getSuggestions(@PathVariable Long familyId) {
        return ApiResponse.ok(suggestionService.suggest(familyId));
    }
}
