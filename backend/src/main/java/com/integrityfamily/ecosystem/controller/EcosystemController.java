package com.integrityfamily.ecosystem.controller;

import com.integrityfamily.ecosystem.domain.NetworkType;
import com.integrityfamily.ecosystem.dto.EcosystemDataView;
import com.integrityfamily.ecosystem.dto.EcosystemDtos.*;
import com.integrityfamily.ecosystem.service.EcosystemAuditService;
import com.integrityfamily.ecosystem.service.EcosystemDataViewService;
import com.integrityfamily.ecosystem.service.EcosystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Ecosistema de Apoyo — API REST.
 *
 * /api/ecosystem/participants              → catálogo global de participantes
 * /api/families/{id}/ecosystem            → resumen del ecosistema de una familia
 * /api/families/{id}/ecosystem/links      → vincular / consentir / revocar
 * /api/families/{id}/ecosystem/network    → filtrar por tipo de red
 */
@RestController
@RequiredArgsConstructor
public class EcosystemController {

    private final EcosystemService service;
    private final EcosystemDataViewService dataViewService;
    private final EcosystemAuditService auditService;

    // ── Catálogo ──────────────────────────────────────────────────────────

    @GetMapping("/api/ecosystem/participants")
    public ResponseEntity<List<ParticipantResponse>> listParticipants(
            @RequestParam(required = false) NetworkType networkType) {
        return ResponseEntity.ok(service.listParticipants(networkType));
    }

    @PostMapping("/api/ecosystem/participants")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ParticipantResponse> register(@RequestBody RegisterParticipantRequest req) {
        return ResponseEntity.ok(service.registerParticipant(req));
    }

    // ── Ecosistema de una familia ─────────────────────────────────────────

    @GetMapping("/api/families/{familyId}/ecosystem")
    public ResponseEntity<FamilyEcosystemSummary> getSummary(@PathVariable Long familyId) {
        return ResponseEntity.ok(service.getSummary(familyId));
    }

    @GetMapping("/api/families/{familyId}/ecosystem/active")
    public ResponseEntity<List<LinkResponse>> getActive(@PathVariable Long familyId) {
        return ResponseEntity.ok(service.getActiveLinks(familyId));
    }

    @GetMapping("/api/families/{familyId}/ecosystem/network")
    public ResponseEntity<List<LinkResponse>> getByNetwork(
            @PathVariable Long familyId,
            @RequestParam NetworkType networkType) {
        return ResponseEntity.ok(service.getLinksByNetwork(familyId, networkType));
    }

    // ── Ciclo de consentimiento ───────────────────────────────────────────

    /** La familia vincula un participante a su ecosistema */
    @PostMapping("/api/families/{familyId}/ecosystem/links")
    public ResponseEntity<LinkResponse> link(
            @PathVariable Long familyId,
            @RequestBody LinkRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(service.link(familyId, req, principal.getUsername()));
    }

    /** La familia otorga consentimiento explícito */
    @PostMapping("/api/families/{familyId}/ecosystem/consent")
    public ResponseEntity<LinkResponse> consent(
            @PathVariable Long familyId,
            @RequestBody ConsentRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(service.giveConsent(familyId, req, principal.getUsername()));
    }

    /** La familia revoca el vínculo — sin justificación obligatoria */
    @PostMapping("/api/families/{familyId}/ecosystem/revoke")
    public ResponseEntity<LinkResponse> revoke(
            @PathVariable Long familyId,
            @RequestBody RevokeRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(service.revoke(familyId, req, principal.getUsername()));
    }

    // ── Vista de datos filtrada por scope ─────────────────────────────────

    /**
     * El participante consulta los datos que le están autorizados.
     * TERRITORIAL recibe solo datos geográficos anónimos.
     */
    @GetMapping("/api/families/{familyId}/ecosystem/links/{linkId}/view")
    public ResponseEntity<EcosystemDataView> getDataView(
            @PathVariable Long familyId,
            @PathVariable Long linkId,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(dataViewService.getDataView(familyId, linkId, principal.getUsername()));
    }

    // ── Auditoría ─────────────────────────────────────────────────────────

    /** La familia ve quién accedió a qué y cuándo */
    @GetMapping("/api/families/{familyId}/ecosystem/audit")
    public ResponseEntity<List<AuditLogEntry>> getAuditLog(@PathVariable Long familyId) {
        return ResponseEntity.ok(auditService.getAuditLog(familyId));
    }

    /** Historial de acceso de un vínculo específico */
    @GetMapping("/api/families/{familyId}/ecosystem/links/{linkId}/audit")
    public ResponseEntity<List<AuditLogEntry>> getLinkAuditLog(
            @PathVariable Long familyId,
            @PathVariable Long linkId) {
        return ResponseEntity.ok(auditService.getAuditLogByLink(linkId));
    }
}
