package com.integrityfamily.support.controller;

import com.integrityfamily.support.domain.SupportSpecialty;
import com.integrityfamily.support.dto.SupportNetworkDtos.*;
import com.integrityfamily.support.service.SupportNetworkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Red de Apoyo Humano — API REST.
 *
 * /api/support/professionals        → catálogo de profesionales
 * /api/families/{id}/support        → gestión por familia (solo la propia familia)
 */
@RestController
@RequiredArgsConstructor
public class SupportNetworkController {

    private final SupportNetworkService service;

    // ─────────────────────────────────────────────────────────────────────
    // Catálogo de profesionales (cualquier usuario autenticado puede consultar)
    // ─────────────────────────────────────────────────────────────────────

    @GetMapping("/api/support/professionals")
    public ResponseEntity<List<ProfessionalResponse>> listProfessionals(
            @RequestParam(required = false) SupportSpecialty specialty) {
        return ResponseEntity.ok(service.listProfessionals(specialty));
    }

    /** Solo el admin registra profesionales en el catálogo */
    @PostMapping("/api/support/professionals")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProfessionalResponse> register(@RequestBody RegisterProfessionalRequest req) {
        return ResponseEntity.ok(service.registerProfessional(req));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Gestión por familia — la familia decide quién entra y qué ve
    // ─────────────────────────────────────────────────────────────────────

    @GetMapping("/api/families/{familyId}/support")
    public ResponseEntity<FamilySupportSummary> getSummary(@PathVariable Long familyId) {
        return ResponseEntity.ok(service.getSummary(familyId));
    }

    @GetMapping("/api/families/{familyId}/support/active")
    public ResponseEntity<List<AssignmentResponse>> getActive(@PathVariable Long familyId) {
        return ResponseEntity.ok(service.getActive(familyId));
    }

    /** La familia invita a un profesional */
    @PostMapping("/api/families/{familyId}/support/invite")
    public ResponseEntity<AssignmentResponse> invite(
            @PathVariable Long familyId,
            @RequestBody InviteRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(service.invite(familyId, req, principal.getUsername()));
    }

    /** La familia otorga consentimiento explícito */
    @PostMapping("/api/families/{familyId}/support/consent")
    public ResponseEntity<AssignmentResponse> consent(
            @PathVariable Long familyId,
            @RequestBody ConsentRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(service.giveConsent(familyId, req, principal.getUsername()));
    }

    /** La familia revoca el acceso — sin necesidad de justificación */
    @PostMapping("/api/families/{familyId}/support/revoke")
    public ResponseEntity<AssignmentResponse> revoke(
            @PathVariable Long familyId,
            @RequestBody RevokeRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(service.revoke(familyId, req, principal.getUsername()));
    }

    /** El profesional deja una nota clínica */
    @PostMapping("/api/families/{familyId}/support/notes")
    @PreAuthorize("hasAnyRole('THERAPIST','ORIENTADOR','ADMIN')")
    public ResponseEntity<NoteResponse> addNote(
            @PathVariable Long familyId,
            @RequestBody AddNoteRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        // En producción: resolver supportMemberId desde el principal del profesional autenticado
        // Por ahora usamos el assignmentId para validar autoría en el servicio
        Long supportMemberId = resolveSupportMemberId(principal);
        return ResponseEntity.ok(service.addNote(familyId, req, supportMemberId));
    }

    private Long resolveSupportMemberId(UserDetails principal) {
        // El profesional se autentica con su email; el servicio valida que coincida con la asignación
        // Este método es un stub — en producción se obtiene del token JWT extendido
        return -1L;
    }
}
