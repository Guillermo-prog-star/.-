package com.integrityfamily.family.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.dto.BehavioralEventResponse;
import com.integrityfamily.dto.CreateBehavioralEventRequest;
import com.integrityfamily.dto.FamilyIvrSummary;
import com.integrityfamily.dto.RepairBehavioralEventRequest;
import com.integrityfamily.family.service.FamilyBehavioralEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SDD: Controlador de Eventos Conductuales y Medición IVR.
 * Ofrece soporte robusto para el registro cronológico de conflictos e incidentes de reparación.
 */
@RestController
@RequestMapping("/api/family-behavioral-events")
@RequiredArgsConstructor
public class FamilyBehavioralEventController {

    private final FamilyBehavioralEventService service;

    /**
     * POST /api/family-behavioral-events — Registra un nuevo evento de fricción.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BehavioralEventResponse> create(
            @Valid @RequestBody CreateBehavioralEventRequest request
    ) {
        BehavioralEventResponse response = service.create(request);
        return ApiResponse.ok(response, "Incidente de fricción registrado correctamente.");
    }

    /**
     * PUT /api/family-behavioral-events/{id}/repair — Repara/soluciona una fricción existente.
     */
    @PutMapping("/{id}/repair")
    public ApiResponse<BehavioralEventResponse> repair(
            @PathVariable Long id,
            @Valid @RequestBody RepairBehavioralEventRequest request
    ) {
        BehavioralEventResponse response = service.repair(id, request);
        return ApiResponse.ok(response, "Se ha registrado la reparación del incidente.");
    }

    /**
     * GET /api/family-behavioral-events/family/{familyId} — Recupera el historial cronológico de la familia.
     */
    @GetMapping("/family/{familyId}")
    public ApiResponse<List<BehavioralEventResponse>> findByFamily(
            @PathVariable Long familyId
    ) {
        List<BehavioralEventResponse> history = service.findByFamily(familyId);
        return ApiResponse.ok(history, "Historial cronológico de eventos recuperado.");
    }

    /**
     * GET /api/family-behavioral-events/family/{familyId}/ivr — Calcula el Índice de Velocidad de Reparación.
     */
    @GetMapping("/family/{familyId}/ivr")
    public ApiResponse<FamilyIvrSummary> calculateIvr(
            @PathVariable Long familyId
    ) {
        FamilyIvrSummary summary = service.calculateIvr(familyId);
        return ApiResponse.ok(summary, "Índice de Velocidad de Reparación (IVR) calculado con éxito.");
    }
}
