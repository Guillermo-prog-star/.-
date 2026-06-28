package com.integrityfamily.trajectory.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.domain.RiskMacrodomain;
import com.integrityfamily.domain.TrajectoryStatus;
import com.integrityfamily.trajectory.dto.TrajectoryDtos.*;
import com.integrityfamily.trajectory.service.TrajectoryService;
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
    public ApiResponse<Void> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateStatusRequest request) {
        TrajectoryStatus status = TrajectoryStatus.valueOf(request.status().toUpperCase());
        trajectoryService.updateStatus(id, status, request.notes());
        return ApiResponse.ok(null);
    }

    // ─── Timeline endpoints ───────────────────────────────────────────────────

    @GetMapping("/family/{id}/timeline")
    public ApiResponse<List<TrajectoryTimelineDto>> getTimeline(@PathVariable Long id) {
        return ApiResponse.ok(trajectoryService.getTimeline(id));
    }

    @PostMapping("/family/{id}/timeline")
    public ApiResponse<TrajectoryTimelineDto> addTimelineEvent(
            @PathVariable Long id,
            @RequestBody TimelineEventRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        String email = principal != null ? principal.getUsername() : "system";
        return ApiResponse.ok(trajectoryService.addTimelineEvent(id, request, email));
    }

    // ─── Impact indicator endpoints ───────────────────────────────────────────

    @GetMapping("/family/{id}/impact")
    public ApiResponse<List<TrajectoryImpactDto>> getImpact(@PathVariable Long id) {
        return ApiResponse.ok(trajectoryService.getImpactSummary(id));
    }

    @PostMapping("/family/{id}/indicator")
    public ApiResponse<TrajectoryImpactDto> upsertIndicator(
            @PathVariable Long id,
            @RequestBody IndicatorRequest request) {
        return ApiResponse.ok(trajectoryService.upsertIndicator(id, request));
    }
}
