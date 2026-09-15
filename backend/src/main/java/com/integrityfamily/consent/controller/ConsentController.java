package com.integrityfamily.consent.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.consent.dto.ConsentDtos.ConsentResponse;
import com.integrityfamily.consent.dto.ConsentDtos.GrantRequest;
import com.integrityfamily.consent.dto.ConsentDtos.RevokeRequest;
import com.integrityfamily.consent.service.ConsentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * /api/families/{familyId}/consents → ciclo de vida del consentimiento
 * (Fase 2 del programa de interoperabilidad con el ecosistema de salud).
 */
@RestController
@RequestMapping("/api/families/{familyId}/consents")
@RequiredArgsConstructor
public class ConsentController {

    private final ConsentService consentService;

    @PreAuthorize("@familySecurity.check(#familyId)")
    @PostMapping
    public ApiResponse<ConsentResponse> grant(
            @PathVariable Long familyId,
            @Valid @RequestBody GrantRequest req,
            Authentication auth) {
        return ApiResponse.ok(consentService.grant(familyId, req, auth.getName()));
    }

    @PreAuthorize("@familySecurity.check(#familyId)")
    @PostMapping("/{consentId}/revoke")
    public ApiResponse<ConsentResponse> revoke(
            @PathVariable Long familyId,
            @PathVariable Long consentId,
            @RequestBody(required = false) RevokeRequest req,
            Authentication auth) {
        return ApiResponse.ok(consentService.revoke(familyId, consentId, req, auth.getName()));
    }

    @PreAuthorize("@familySecurity.check(#familyId)")
    @GetMapping
    public ApiResponse<List<ConsentResponse>> list(@PathVariable Long familyId) {
        return ApiResponse.ok(consentService.list(familyId));
    }

    @PreAuthorize("@familySecurity.check(#familyId)")
    @GetMapping("/active")
    public ApiResponse<List<ConsentResponse>> listActive(@PathVariable Long familyId) {
        return ApiResponse.ok(consentService.listActive(familyId));
    }
}
