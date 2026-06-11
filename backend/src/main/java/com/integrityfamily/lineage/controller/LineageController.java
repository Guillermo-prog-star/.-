package com.integrityfamily.lineage.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.lineage.dto.*;
import com.integrityfamily.lineage.service.LineageService;
import com.integrityfamily.lineage.dto.GenerationInfoRequest;
import com.integrityfamily.lineage.dto.GenerationInfoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/families/{familyId}/lineage")
@RequiredArgsConstructor
public class LineageController {

    private final LineageService lineageService;

    // ── LINEAGE ────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<LineageResponse> get(@PathVariable Long familyId) {
        return ApiResponse.ok(lineageService.getByFamily(familyId));
    }

    @PostMapping
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<LineageResponse> create(
            @PathVariable Long familyId,
            @Valid @RequestBody CreateLineageRequest req) {
        return ApiResponse.ok(lineageService.create(familyId, req), "Linaje creado exitosamente");
    }

    @PutMapping
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<LineageResponse> update(
            @PathVariable Long familyId,
            @Valid @RequestBody CreateLineageRequest req) {
        return ApiResponse.ok(lineageService.update(familyId, req), "Linaje actualizado");
    }

    // ── MEMBERS ───────────────────────────────────────────────────

    @PostMapping("/members")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<LineageMemberResponse> addMember(
            @PathVariable Long familyId,
            @Valid @RequestBody LineageMemberRequest req) {
        return ApiResponse.ok(lineageService.addMember(familyId, req), "Miembro agregado al linaje");
    }

    @PutMapping("/members/{memberId}")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<LineageMemberResponse> updateMember(
            @PathVariable Long familyId,
            @PathVariable Long memberId,
            @Valid @RequestBody LineageMemberRequest req) {
        return ApiResponse.ok(lineageService.updateMember(familyId, memberId, req), "Miembro actualizado");
    }

    @DeleteMapping("/members/{memberId}")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<Void> deleteMember(
            @PathVariable Long familyId,
            @PathVariable Long memberId) {
        lineageService.deleteMember(familyId, memberId);
        return ApiResponse.ok(null, "Miembro eliminado");
    }

    // ── RELATIONSHIPS ─────────────────────────────────────────────

    @PostMapping("/relationships")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<LineageRelationshipResponse> addRelationship(
            @PathVariable Long familyId,
            @Valid @RequestBody LineageRelationshipRequest req) {
        return ApiResponse.ok(lineageService.addRelationship(familyId, req), "Relación registrada");
    }

    @DeleteMapping("/relationships/{relId}")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<Void> deleteRelationship(
            @PathVariable Long familyId,
            @PathVariable Long relId) {
        lineageService.deleteRelationship(familyId, relId);
        return ApiResponse.ok(null, "Relación eliminada");
    }

    // ── GENERATION INFO ───────────────────────────────────────────

    @PutMapping("/generation-info")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<GenerationInfoResponse> upsertGenerationInfo(
            @PathVariable Long familyId,
            @Valid @RequestBody GenerationInfoRequest req) {
        return ApiResponse.ok(lineageService.upsertGenerationInfo(familyId, req), "Información generacional guardada");
    }
}
