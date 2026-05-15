package com.integrityfamily.bitacora.controller;

import com.integrityfamily.bitacora.dto.JournalDtos.*;
import com.integrityfamily.bitacora.service.JournalService;
import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.domain.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SDD Sprint 4: Controlador de Aprendizaje y Transformación Longitudinal.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Aprendizaje Longitudinal y Bitácora", description = "Endpoints para la subida de evidencias, reflexiones guiadas, entradas de bitácora y métricas evolutivas.")
public class JournalController {

    private final JournalService journalService;

    @PostMapping("/tasks/{taskId}/evidence")
    @Operation(summary = "Subir evidencia de tarea", description = "Registra una confirmación contextualizada (foto, texto, etc.) de que una acción/misión fue completada.")
    public ApiResponse<TaskEvidence> uploadEvidence(
            @PathVariable Long taskId,
            @RequestBody EvidenceUploadRequest request) {
        return ApiResponse.ok(journalService.uploadEvidence(taskId, request));
    }

    @PostMapping("/reflections")
    @Operation(summary = "Crear reflexión guiada", description = "Registra una reflexión breve y estructurada, generando automáticamente un aprendizaje consolidado y una entrada en la bitácora familiar.")
    public ApiResponse<Reflection> createReflection(@RequestBody ReflectionCreateRequest request) {
        return ApiResponse.ok(journalService.createReflection(request));
    }

    @PostMapping("/journal")
    @Operation(summary = "Crear entrada de bitácora", description = "Registra de forma híbrida (estructurada y narrativa) un hito en la transformación familiar.")
    public ApiResponse<JournalEntry> createJournal(@RequestBody JournalCreateRequest request) {
        return ApiResponse.ok(journalService.createJournal(request));
    }

    @GetMapping("/families/{familyId}/timeline")
    @Operation(summary = "Obtener línea de tiempo longitudinal", description = "Devuelve el historial evolutivo completo ordenado por fecha (evidencias, reflexiones, aprendizajes y bitácoras).")
    public ApiResponse<List<TimelineEntryDto>> getTimeline(@PathVariable Long familyId) {
        return ApiResponse.ok(journalService.getTimeline(familyId));
    }

    @GetMapping("/families/{familyId}/metrics")
    @Operation(summary = "Obtener métricas longitudinales", description = "Calcula la adherencia a misiones, evolución emocional, persistencia y reflexiones activas de la familia.")
    public ApiResponse<LongitudinalMetricsDto> getMetrics(@PathVariable Long familyId) {
        return ApiResponse.ok(journalService.getMetrics(familyId));
    }
}
