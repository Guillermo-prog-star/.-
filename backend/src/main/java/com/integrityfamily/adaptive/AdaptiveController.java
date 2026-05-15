package com.integrityfamily.adaptive;

import com.integrityfamily.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * SDD Sprint 8: Controlador REST de Integración Adaptativa Real.
 * Expone los endpoints oficiales de evaluación, aprobación y aplicación conectados a adaptive_adjustments.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Integración Adaptativa Real", description = "Endpoints de producción para proponer, aprobar y aplicar ajustes en la convivencia familiar.")
public class AdaptiveController {

    private final AdaptivePlanService adaptivePlanService;

    @PostMapping("/families/{familyId}/adaptive/evaluate")
    @Operation(summary = "Evaluar métricas y proponer ajustes", description = "Construye el contexto real de la familia y devuelve las propuestas guardadas en adaptive_adjustments con estado PROPOSED.")
    public ApiResponse<List<AdaptiveAdjustmentEntity>> evaluateAdaptive(@PathVariable Long familyId) {
        return ApiResponse.ok(adaptivePlanService.evaluateAndProposeForFamily(familyId));
    }

    @PostMapping("/adaptive-adjustments/{adjustmentId}/approve")
    @Operation(summary = "Aprobar ajuste propuesto", description = "Pasa un ajuste de PROPOSED a APPROVED, registrando la auditoría en la Bitácora.")
    public ApiResponse<AdaptiveAdjustmentEntity> approveAdjustment(
            @PathVariable UUID adjustmentId,
            @RequestParam(required = false, defaultValue = "Consejo de Familia") String approvedBy) {
        return ApiResponse.ok(adaptivePlanService.approveAdjustment(adjustmentId, approvedBy));
    }

    @PostMapping("/adaptive-adjustments/{adjustmentId}/apply")
    @Operation(summary = "Aplicar mutaciones de ajuste", description = "Ejecuta las mutaciones específicas sobre PlanTask (espaciamiento, misiones introductorias o de escucha) y deja entrada automática en Bitácora.")
    public ApiResponse<AdaptiveAdjustmentEntity> applyAdjustment(
            @PathVariable UUID adjustmentId) {
        return ApiResponse.ok(adaptivePlanService.applyAdjustment(adjustmentId));
    }

    // Endpoints de compatibilidad para pruebas de QA de contrato en memoria
    @PostMapping("/v1/adaptive/evaluate")
    @Operation(summary = "Evaluación determinística en memoria", description = "Evalúa un snapshot en memoria sin persistir.")
    public ApiResponse<List<AdaptiveAdjustment>> evaluateInMemory(@RequestBody AdaptivePlanContext context) {
        return ApiResponse.ok(adaptivePlanService.evaluate(context));
    }

    @PostMapping("/v1/adaptive/approve")
    @Operation(summary = "Aprobación determinística en memoria", description = "Aprueba un ajuste en memoria para contrato QA.")
    public ApiResponse<AdaptiveAdjustment> approveInMemory(@RequestBody AdaptiveAdjustment adjustment) {
        return ApiResponse.ok(adaptivePlanService.approve(adjustment));
    }

    @PostMapping("/v1/adaptive/apply")
    @Operation(summary = "Aplicación determinística en memoria", description = "Aplica un ajuste en memoria para contrato QA.")
    public ApiResponse<AdaptiveAdjustment> applyInMemory(@RequestBody AdaptiveAdjustment adjustment) {
        return ApiResponse.ok(adaptivePlanService.apply(adjustment));
    }
}
