package com.integrityfamily.plan.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.domain.ImprovementPlan;
import com.integrityfamily.domain.PlanTask;
import com.integrityfamily.domain.AuditEventType;
import com.integrityfamily.auth.service.AuditService;
import com.integrityfamily.plan.dto.PlanDtos.*;
import com.integrityfamily.plan.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * SDD: Controlador de Planes de Transformación Familiar.
 * Refactorizado para usar la arquitectura de ImprovementPlan e Hitos, con telemetría integrada y motor determinístico.
 */
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
@Tag(name = "Planes de Transformación", description = "Endpoints para la gestión y generación determinística de planes de mejora familiar.")
public class PlanController {

    private final PlanService planService;
    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "Obtener todos los planes", description = "Devuelve la lista de todos los planes de mejora familiar registrados.")
    public ApiResponse<List<PlanResponse>> getAllPlans() {
        return ApiResponse.ok(planService.findAllPlans());
    }

    @GetMapping("/family/{familyId}")
    @Operation(summary = "Planes por familia", description = "Obtiene los planes de mejora asignados a una familia específica.")
    public ApiResponse<List<PlanResponse>> getPlansByFamily(@PathVariable Long familyId) {
        return ApiResponse.ok(planService.findByFamilyId(familyId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener plan por ID", description = "Devuelve el detalle completo de un plan de mejora y sus misiones asociadas.")
    public ApiResponse<PlanResponse> getPlanById(@PathVariable Long id) {
        return ApiResponse.ok(planService.findPlanById(id));
    }

    @PostMapping
    @Operation(summary = "Crear plan manualmente", description = "Permite registrar un nuevo plan de mejora de forma manual.")
    public ApiResponse<PlanResponse> createPlan(@RequestBody ImprovementPlan plan) {
        return ApiResponse.ok(planService.createPlan(plan));
    }

    @PostMapping("/generate-deterministic")
    @Operation(summary = "Generar Plan Determinístico (Sprint 3)", description = "Ensambla dinámicamente un plan de mejora parametrizado con misiones clínicas por fase (1 semana, 1 mes, 3 meses, 6 meses) basado en el nivel de riesgo y dimensión crítica de una evaluación.")
    public ApiResponse<PlanResponse> generateDeterministicPlan(@RequestBody PlanGenerateRequest request) {
        return ApiResponse.ok(planService.generateDeterministicPlan(request.evaluationId()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar plan", description = "Modifica los datos principales de un plan existente.")
    public ApiResponse<PlanResponse> updatePlan(@PathVariable Long id, @RequestBody ImprovementPlan plan) {
        return ApiResponse.ok(planService.updatePlan(id, plan));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar plan", description = "Borra un plan de mejora familiar.")
    public ApiResponse<Void> deletePlan(@PathVariable Long id) {
        planService.deletePlan(id);
        return ApiResponse.ok(null);
    }

    // --- Tareas ---

    @GetMapping("/tasks")
    @Operation(summary = "Obtener todas las tareas", description = "Lista todas las misiones/tareas clínicas en el sistema.")
    public ApiResponse<List<PlanTaskResponse>> getAllTasks() {
        return ApiResponse.ok(planService.findAllTasks());
    }

    @GetMapping("/tasks/{id}")
    @Operation(summary = "Obtener tarea por ID", description = "Devuelve el detalle de una misión clínica específica.")
    public ApiResponse<PlanTaskResponse> getTaskById(@PathVariable Long id) {
        return ApiResponse.ok(planService.findTaskById(id));
    }

    @PostMapping("/tasks")
    @Operation(summary = "Crear tarea", description = "Registra una nueva misión clínica dentro de un plan.")
    public ApiResponse<PlanTaskResponse> createTask(@RequestBody PlanTask task) {
        return ApiResponse.ok(planService.createTask(task));
    }

    @PutMapping("/tasks/{id}")
    @Operation(summary = "Actualizar tarea", description = "Modifica los detalles de una misión clínica.")
    public ApiResponse<PlanTaskResponse> updateTask(@PathVariable Long id, @RequestBody PlanTask task) {
        return ApiResponse.ok(planService.updateTask(id, task));
    }

    @PutMapping("/tasks/{id}/complete")
    @Operation(summary = "Alternar estado de tarea (Auto-Evidence Sentinel)", description = "Marca una misión como completada o pendiente y genera automáticamente el registro de auto-evidencia en la bitácora familiar.")
    public ApiResponse<PlanTaskResponse> completeTask(
            @PathVariable Long id,
            @RequestBody TaskCompleteRequest body,
            Principal principal,
            HttpServletRequest httpServletRequest) {
        boolean completed = Boolean.TRUE.equals(body.completed());
        PlanTaskResponse response = planService.completeTask(id, completed);
        
        String email = principal != null ? principal.getName() : "ANONYMOUS";
        String metadata = String.format("{\"taskId\":%d,\"completed\":%b}", id, completed);
        
        auditService.register(email, AuditEventType.PLAN_TASK_TOGGLED, httpServletRequest, metadata);
        
        return ApiResponse.ok(response);
    }

    @DeleteMapping("/tasks/{id}")
    @Operation(summary = "Eliminar tarea", description = "Borra una misión clínica.")
    public ApiResponse<Void> deleteTask(@PathVariable Long id) {
        planService.deleteTask(id);
        return ApiResponse.ok(null);
    }
}
