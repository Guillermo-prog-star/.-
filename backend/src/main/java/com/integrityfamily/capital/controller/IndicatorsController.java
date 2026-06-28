package com.integrityfamily.capital.controller;

import com.integrityfamily.capital.dto.IndicatorResult;
import com.integrityfamily.capital.dto.IndicatorsSnapshot;
import com.integrityfamily.capital.service.FamilyIndicatorsService;
import com.integrityfamily.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints del Sistema de Medición del Fortalecimiento Familiar (SMFF).
 *
 * Base: /api/indicators/family/{familyId}
 *
 * Todos los endpoints requieren que el principal sea creador o miembro de la familia.
 */
@RestController
@RequestMapping("/api/indicators/family/{familyId}")
@RequiredArgsConstructor
public class IndicatorsController {

    private final FamilyIndicatorsService indicatorsService;

    /**
     * Snapshot completo: los 20 indicadores + score SMFF agregado.
     * Es el endpoint principal — calcula bajo demanda en tiempo real.
     */
    @GetMapping
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ResponseEntity<ApiResponse<IndicatorsSnapshot>> getAll(@PathVariable Long familyId) {
        return ResponseEntity.ok(ApiResponse.ok(indicatorsService.computeAll(familyId)));
    }

    /**
     * Sólo los indicadores de un grupo temático.
     * Grupos válidos: cohesion | confianza | transf | resil | long
     */
    @GetMapping("/group/{group}")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ResponseEntity<ApiResponse<List<IndicatorResult>>> getByGroup(
            @PathVariable Long familyId,
            @PathVariable String group) {

        List<String> valid = List.of("cohesion", "confianza", "transf", "resil", "long");
        if (!valid.contains(group)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Grupo inválido. Opciones: " + valid));
        }

        IndicatorsSnapshot snapshot = indicatorsService.computeAll(familyId);
        List<IndicatorResult> filtered = snapshot.indicators().stream()
                .filter(r -> group.equals(r.group()))
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(filtered));
    }

    /**
     * Un indicador específico por ID (ej. IND-03).
     */
    @GetMapping("/{indicatorId}")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ResponseEntity<ApiResponse<IndicatorResult>> getOne(
            @PathVariable Long familyId,
            @PathVariable String indicatorId) {

        IndicatorsSnapshot snapshot = indicatorsService.computeAll(familyId);
        return snapshot.indicators().stream()
                .filter(r -> indicatorId.equalsIgnoreCase(r.id()))
                .findFirst()
                .map(r -> ResponseEntity.ok(ApiResponse.ok(r)))
                .orElse(ResponseEntity.ok(
                        ApiResponse.error("Indicador no encontrado: " + indicatorId)));
    }
}
