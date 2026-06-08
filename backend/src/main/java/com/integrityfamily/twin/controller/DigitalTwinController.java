package com.integrityfamily.twin.controller;

import com.integrityfamily.twin.dto.DigitalTwinDto;
import com.integrityfamily.twin.service.DigitalTwinService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/families/{familyId}/twin")
@RequiredArgsConstructor
public class DigitalTwinController {

    private final DigitalTwinService twinService;

    /** Devuelve el Gemelo Digital actual (desde BD si existe). */
    @GetMapping
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ResponseEntity<DigitalTwinDto> get(@PathVariable Long familyId) {
        return twinService.getTwin(familyId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Fuerza el recómputo completo del Gemelo Digital. */
    @PostMapping("/compute")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ResponseEntity<DigitalTwinDto> compute(@PathVariable Long familyId) {
        return ResponseEntity.ok(twinService.compute(familyId));
    }
}
