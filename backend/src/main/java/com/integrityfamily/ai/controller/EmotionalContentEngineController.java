package com.integrityfamily.ai.controller;

import com.integrityfamily.ai.dto.EmotionalContentDtos.*;
import com.integrityfamily.ai.service.EmotionalContentEngineService;
import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.domain.EmotionalStimulus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/emotional-engine")
@RequiredArgsConstructor
@Tag(name = "Motor de Contenido Emocional IA", description = "Endpoints para el visualizador de Historias Invisibles, reflexiones y el Índice de Observación Consciente (IOC).")
public class EmotionalContentEngineController {

    private final EmotionalContentEngineService service;

    @GetMapping("/active")
    @Operation(summary = "Obtener estímulo reflexivo activo", description = "Retorna el estímulo activo del día para la familia (video de Historias Invisibles).")
    public ApiResponse<EmotionalStimulus> getActiveStimulus() {
        return service.getActiveStimulus()
                .map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.error("No hay estímulos activos catalogados en el sistema."));
    }

    @PostMapping("/{id}/reflect")
    @Operation(summary = "Procesar reflexión introspectiva familiar", description = "Recibe la reflexión del miembro, corre el análisis cognitivo con la IA y devuelve los indicadores del veredicto relacional.")
    public ApiResponse<EmotionalInferenceDto> processReflection(
            @PathVariable Long id,
            @RequestBody ReflectionRequest request
    ) {
        try {
            return ApiResponse.ok(service.processReflection(id, request));
        } catch (Exception e) {
            return ApiResponse.error("Error procesando reflexión con el nodo central: " + e.getMessage());
        }
    }

    @GetMapping("/stats/family/{familyId}")
    @Operation(summary = "Obtener estadísticas del Índice de Observación Consciente", description = "Retorna el IOC calculado y los promedios de empatía, presencia y reactividad del hogar.")
    public ApiResponse<FamilyEmotionalStats> getFamilyStats(@PathVariable Long familyId) {
        return ApiResponse.ok(service.getFamilyStats(familyId));
    }
}
