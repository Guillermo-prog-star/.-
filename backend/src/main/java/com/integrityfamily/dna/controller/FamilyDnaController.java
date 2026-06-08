package com.integrityfamily.dna.controller;

import com.integrityfamily.dna.dto.FamilyDnaDto;
import com.integrityfamily.dna.service.FamilyDnaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/families/{familyId}/dna")
@RequiredArgsConstructor
@Slf4j
public class FamilyDnaController {

    private final FamilyDnaService dnaService;

    /** Obtiene el ADN actual. 404 si aún no se ha sintetizado. */
    @GetMapping
    public ResponseEntity<FamilyDnaDto> get(@PathVariable Long familyId) {
        return dnaService.findByFamilyId(familyId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Solicita una nueva síntesis del ADN a partir de todo el historial disponible. */
    @PostMapping("/synthesize")
    public ResponseEntity<FamilyDnaDto> synthesize(@PathVariable Long familyId) {
        log.info("[DNA] Síntesis solicitada para familia {}", familyId);
        FamilyDnaDto dna = dnaService.synthesize(familyId);
        return ResponseEntity.ok(dna);
    }
}
