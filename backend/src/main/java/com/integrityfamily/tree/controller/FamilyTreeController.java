package com.integrityfamily.tree.controller;

import com.integrityfamily.tree.domain.GenerationalMessage;
import com.integrityfamily.tree.dto.*;
import com.integrityfamily.tree.service.FamilyTreeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/families/{familyId}/tree")
@RequiredArgsConstructor
public class FamilyTreeController {

    private final FamilyTreeService treeService;

    /** Árbol completo desde la raíz ancestral */
    @GetMapping
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ResponseEntity<FamilyTreeDto> getFullTree(@PathVariable Long familyId) {
        return ResponseEntity.ok(treeService.getFullTree(familyId));
    }

    /** Línea directa de ancestros */
    @GetMapping("/ancestors")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ResponseEntity<List<FamilyTreeDto>> getAncestors(@PathVariable Long familyId) {
        return ResponseEntity.ok(treeService.getAncestorLine(familyId));
    }

    /** Todo lo heredado de las generaciones anteriores */
    @GetMapping("/heritage")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ResponseEntity<HeritageDto> getHeritage(@PathVariable Long familyId) {
        return ResponseEntity.ok(treeService.getHeritage(familyId));
    }

    /** Vincular con una familia origen usando su código */
    @PostMapping("/link")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ResponseEntity<Void> link(
            @PathVariable Long familyId,
            @Valid @RequestBody LinkRequest req) {
        treeService.link(familyId, req);
        return ResponseEntity.ok().build();
    }

    /** Desvincularse de la familia origen */
    @DeleteMapping("/link")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ResponseEntity<Void> unlink(@PathVariable Long familyId) {
        treeService.unlink(familyId);
        return ResponseEntity.ok().build();
    }

    /** Crear mensaje para generaciones futuras */
    @PostMapping("/messages")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ResponseEntity<GenerationalMessage> createMessage(
            @PathVariable Long familyId,
            @Valid @RequestBody MessageRequest req) {
        return ResponseEntity.ok(treeService.createMessage(familyId, req));
    }

    /** Mensajes de ancestros que ya se pueden leer */
    @GetMapping("/messages/ancestors")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ResponseEntity<List<GenerationalMessage>> getAncestorMessages(@PathVariable Long familyId) {
        return ResponseEntity.ok(treeService.getReadableAncestorMessages(familyId));
    }

    /** Mensajes que esta familia ha escrito */
    @GetMapping("/messages/own")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ResponseEntity<List<GenerationalMessage>> getOwnMessages(@PathVariable Long familyId) {
        return ResponseEntity.ok(treeService.getOwnMessages(familyId));
    }
}
