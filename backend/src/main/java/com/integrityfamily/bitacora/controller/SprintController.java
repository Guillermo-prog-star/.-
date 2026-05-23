package com.integrityfamily.bitacora.controller;

import com.integrityfamily.bitacora.dto.SprintDtos.*;
import com.integrityfamily.bitacora.service.SprintService;
import com.integrityfamily.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sprints")
@RequiredArgsConstructor
@Tag(name = "Sprints de Evolución Familiar", description = "Endpoints para el agilismo familiar (sprints, check-ins diarios y retrospectivas).")
public class SprintController {

    private final SprintService sprintService;

    @GetMapping("/active")
    @Operation(summary = "Obtener el sprint activo de la familia")
    public ApiResponse<SprintResponse> getActiveSprint(@RequestParam Long familyId) {
        return ApiResponse.ok(sprintService.getActiveSprint(familyId));
    }

    @GetMapping("/history")
    @Operation(summary = "Obtener el historial de sprints de la familia")
    public ApiResponse<List<SprintResponse>> getSprintHistory(@RequestParam Long familyId) {
        return ApiResponse.ok(sprintService.getSprintHistory(familyId));
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo sprint familiar")
    public ApiResponse<SprintResponse> createSprint(
            @RequestParam Long familyId,
            @RequestBody CreateSprintRequest request
    ) {
        return ApiResponse.ok(sprintService.createSprint(familyId, request));
    }

    @PutMapping("/{sprintId}/missions/{missionId}/toggle")
    @Operation(summary = "Alternar estado de una misión en el sprint")
    public ApiResponse<SprintResponse> toggleMission(
            @PathVariable Long sprintId,
            @PathVariable Long missionId
    ) {
        return ApiResponse.ok(sprintService.toggleMission(sprintId, missionId));
    }

    @PostMapping("/{sprintId}/dailies")
    @Operation(summary = "Registrar un Check-In Diario (Daily de Conciencia)")
    public ApiResponse<SprintDailyResponse> submitDaily(
            @PathVariable Long sprintId,
            @RequestBody CreateDailyCheckinRequest request
    ) {
        return ApiResponse.ok(sprintService.submitDailyCheckin(sprintId, request));
    }

    @PostMapping("/{sprintId}/retrospective")
    @Operation(summary = "Cerrar el sprint y registrar retrospectiva familiar")
    public ApiResponse<SprintResponse> closeSprint(
            @PathVariable Long sprintId,
            @RequestBody CloseSprintRequest request
    ) {
        return ApiResponse.ok(sprintService.closeSprintAndCreateRetrospective(sprintId, request));
    }
}
