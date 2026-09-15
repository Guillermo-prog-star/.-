package com.integrityfamily.errorprotocol.controller;

import com.integrityfamily.errorprotocol.domain.FamilyErrorProtocol;
import com.integrityfamily.errorprotocol.service.ErrorProtocolService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ErrorProtocolController — API REST del protocolo de gestión del error familiar.
 *
 * GET    /api/families/{familyId}/error-protocols        → todos
 * GET    /api/families/{familyId}/error-protocols/open   → abiertos
 * POST   /api/families/{familyId}/error-protocols        → crear
 * PATCH  /api/families/{familyId}/error-protocols/{id}   → actualizar paso
 * POST   /api/families/{familyId}/error-protocols/{id}/close → cerrar con aprendizaje
 *
 * {@code update} y {@code close} añaden {@code @familySecurity.checkErrorProtocol(#id)} para
 * verificar que el protocolo pertenezca a la familia del usuario (docs/auditoria-2026-09.md,
 * hallazgo 1) — el servicio recibe solo {@code id}.
 */
@RestController
@RequestMapping("/api/families/{familyId}/error-protocols")
@RequiredArgsConstructor
public class ErrorProtocolController {

    private final ErrorProtocolService service;

    @PreAuthorize("@familySecurity.check(#familyId)")
    @GetMapping
    public ResponseEntity<List<FamilyErrorProtocol>> getAll(@PathVariable Long familyId) {
        return ResponseEntity.ok(service.getAll(familyId));
    }

    @PreAuthorize("@familySecurity.check(#familyId)")
    @GetMapping("/open")
    public ResponseEntity<List<FamilyErrorProtocol>> getOpen(@PathVariable Long familyId) {
        return ResponseEntity.ok(service.getOpen(familyId));
    }

    @PreAuthorize("@familySecurity.check(#familyId)")
    @PostMapping
    public ResponseEntity<FamilyErrorProtocol> create(
            @PathVariable Long familyId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.create(familyId, body.getOrDefault("missionFailed", "")));
    }

    @PreAuthorize("@familySecurity.check(#familyId) and @familySecurity.checkErrorProtocol(#id)")
    @PatchMapping("/{id}")
    public ResponseEntity<FamilyErrorProtocol> update(
            @PathVariable Long familyId,
            @PathVariable Long id,
            @RequestBody Map<String, Object> fields) {
        return ResponseEntity.ok(service.update(id, fields));
    }

    @PreAuthorize("@familySecurity.check(#familyId) and @familySecurity.checkErrorProtocol(#id)")
    @PostMapping("/{id}/close")
    public ResponseEntity<FamilyErrorProtocol> close(
            @PathVariable Long familyId,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.close(id, body.getOrDefault("learning", "")));
    }
}
