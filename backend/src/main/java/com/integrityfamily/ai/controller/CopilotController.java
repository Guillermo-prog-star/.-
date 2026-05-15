package com.integrityfamily.ai.controller;

import com.integrityfamily.ai.dto.CopilotDtos.*;
import com.integrityfamily.ai.service.CopilotService;
import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.domain.AiInferenceEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SDD Sprint 6: Controlador del Copiloto Estructurado Operacional.
 */
@RestController
@RequestMapping("/api/copilot")
@RequiredArgsConstructor
@Tag(name = "Copiloto Estructurado IA", description = "Endpoints para la inferencia híbrida (reglas + Claude) y consulta de sugerencias proactivas de contención familiar.")
public class CopilotController {

    private final CopilotService copilotService;

    @GetMapping("/family/{familyId}")
    @Operation(summary = "Obtener sugerencias proactivas", description = "Devuelve el panel estructurado del Copiloto con el resumen del estado familiar, riesgo prioritario, acciones sugeridas y contención.")
    public ApiResponse<StructuredAiInferenceResponse> getLatestSuggestion(@PathVariable Long familyId) {
        return ApiResponse.ok(copilotService.getLatestSuggestion(familyId));
    }

    @PostMapping("/infer")
    @Operation(summary = "Generar inferencia contextual", description = "Ensambla un resumen estructurado compacto (Context Builder), consulta a Claude y persiste la salida en formato JSON auditado.")
    public ApiResponse<StructuredAiInferenceResponse> generateInference(@RequestBody CopilotInferRequest request) {
        return ApiResponse.ok(copilotService.generateInference(request));
    }

    @GetMapping("/history/{familyId}")
    @Operation(summary = "Obtener historial de inferencias y gobernanza", description = "Devuelve los registros inmutables de prompts enviados, contextos compactos y versiones de modelo utilizados (Gobernanza IA).")
    public ApiResponse<List<AiInferenceEntity>> getHistory(@PathVariable Long familyId) {
        return ApiResponse.ok(copilotService.getHistory(familyId));
    }
}
