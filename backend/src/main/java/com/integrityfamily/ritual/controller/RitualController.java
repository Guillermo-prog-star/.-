package com.integrityfamily.ritual.controller;

import com.integrityfamily.ritual.dto.RitualDto;
import com.integrityfamily.ritual.service.RitualEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/families/{familyId}/rituals")
@RequiredArgsConstructor
public class RitualController {

    private final RitualEngineService ritualEngineService;

    /** Rituales pendientes — los que la familia debe vivir hoy */
    @GetMapping("/active")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ResponseEntity<List<RitualDto>> getActive(@PathVariable Long familyId) {
        return ResponseEntity.ok(ritualEngineService.getActiveRituals(familyId));
    }

    /** Historial de rituales */
    @GetMapping("/history")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ResponseEntity<List<RitualDto>> getHistory(@PathVariable Long familyId) {
        return ResponseEntity.ok(ritualEngineService.getHistory(familyId));
    }

    /** Forzar detección manual (útil para pruebas y al crear una familia) */
    @PostMapping("/detect")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ResponseEntity<Map<String, String>> detect(@PathVariable Long familyId) {
        ritualEngineService.detectAndCreateRituals();
        return ResponseEntity.ok(Map.of("status", "detection triggered"));
    }

    @PostMapping("/{ritualId}/activate")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ResponseEntity<RitualDto> activate(@PathVariable Long familyId,
                                              @PathVariable Long ritualId) {
        return ResponseEntity.ok(ritualEngineService.activate(ritualId));
    }

    @PostMapping("/{ritualId}/complete")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ResponseEntity<RitualDto> complete(@PathVariable Long familyId,
                                              @PathVariable Long ritualId) {
        return ResponseEntity.ok(ritualEngineService.complete(ritualId));
    }

    @PostMapping("/{ritualId}/dismiss")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ResponseEntity<Void> dismiss(@PathVariable Long familyId,
                                        @PathVariable Long ritualId) {
        ritualEngineService.dismiss(ritualId);
        return ResponseEntity.ok().build();
    }
}
