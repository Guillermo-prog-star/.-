package com.integrityfamily.plan.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.plan.dto.AdaptivePlanDtos.*;
import com.integrityfamily.plan.service.AdaptivePlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SDD Sprint 7: Controlador REST de Planes Adaptativos IA (AdaptivePlanController).
 * Expone los endpoints para proponer, aprobar y aplicar mutaciones de contención al plan familiar.
 */
@RestController
@RequestMapping("/api/adaptive-plans")
@RequiredArgsConstructor
@Tag(name = "Planes Adaptativos IA", description = "Endpoints de inteligencia híbrida para ajustar proactivamente los planes familiares según métricas de adherencia y convivencia.")
public class AdaptivePlanController {

    private final AdaptivePlanService adaptivePlanService;

    @PostMapping("/family/{familyId}/propose")
    @Operation(summary = "Proponer ajuste adaptativo", description = "Evalúa la adherencia, inactividad y calidad de comunicación para proponer ajustes de contención (reducción de carga, reinicio suave, pausa de misiones).")
    public ApiResponse<PlanAdjustmentResponse> proposeAdjustment(
            @PathVariable Long familyId,
            @RequestBody(required = false) ProposeAdjustmentRequest request) {
        return ApiResponse.ok(adaptivePlanService.proposeAdjustment(familyId, request));
    }

    @PostMapping("/adjustment/{adjustmentId}/approve")
    @Operation(summary = "Aprobar ajuste propuesto", description = "Aprueba una propuesta de ajuste por parte del Consejo de Familia o el Copiloto.")
    public ApiResponse<PlanAdjustmentResponse> approveAdjustment(
            @PathVariable Long adjustmentId,
            @RequestBody(required = false) AdjustmentApprovalRequest request) {
        return ApiResponse.ok(adaptivePlanService.approveAdjustment(adjustmentId, request));
    }

    @PostMapping("/adjustment/{adjustmentId}/apply")
    @Operation(summary = "Aplicar mutaciones de ajuste", description = "Ejecuta las mutaciones de frecuencia, fechas o pausas directamente sobre las misiones activas del plan.")
    public ApiResponse<PlanAdjustmentResponse> applyAdjustment(
            @PathVariable Long adjustmentId) {
        return ApiResponse.ok(adaptivePlanService.applyAdjustment(adjustmentId));
    }

    @GetMapping("/family/{familyId}")
    @Operation(summary = "Historial de ajustes", description = "Devuelve todo el historial de propuestas y mutaciones adaptativas de una familia.")
    public ApiResponse<List<PlanAdjustmentResponse>> getFamilyAdjustments(
            @PathVariable Long familyId) {
        return ApiResponse.ok(adaptivePlanService.getFamilyAdjustments(familyId));
    }
}
