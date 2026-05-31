package com.integrityfamily.legado.controller;

import com.integrityfamily.legado.domain.FamilyLegacy;
import com.integrityfamily.legado.domain.FamilyValue;
import com.integrityfamily.legado.dto.LegacyRequest;
import com.integrityfamily.legado.service.LegacyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * LegacyController — API REST del módulo Legado Familiar.
 *
 * GET  /api/families/{familyId}/legacy         → retorna legado + valores
 * PUT  /api/families/{familyId}/legacy         → guarda legado completo
 * GET  /api/families/{familyId}/legacy/values  → retorna solo los valores
 */
@RestController
@RequestMapping("/api/families/{familyId}/legacy")
@RequiredArgsConstructor
public class LegacyController {

    private final LegacyService legacyService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getLegacy(@PathVariable Long familyId) {
        FamilyLegacy legacy = legacyService.getOrCreate(familyId);
        List<FamilyValue> values = legacyService.getValues(familyId);
        return ResponseEntity.ok(Map.of("legacy", legacy, "values", values));
    }

    @PutMapping
    public ResponseEntity<FamilyLegacy> saveLegacy(
            @PathVariable Long familyId,
            @RequestBody LegacyRequest request) {
        return ResponseEntity.ok(legacyService.save(familyId, request));
    }

    @GetMapping("/values")
    public ResponseEntity<List<FamilyValue>> getValues(@PathVariable Long familyId) {
        return ResponseEntity.ok(legacyService.getValues(familyId));
    }
}
