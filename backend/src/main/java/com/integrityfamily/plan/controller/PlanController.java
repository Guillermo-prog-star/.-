package com.integrityfamily.plan.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.domain.ImprovementPlan;
import com.integrityfamily.domain.PlanTask;
import com.integrityfamily.plan.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * SDD: Controlador de Planes de Transformación Familiar.
 * Refactorizado para usar la arquitectura de ImprovementPlan e Hitos.
 */
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @GetMapping
    public ApiResponse<List<ImprovementPlan>> getAllPlans() {
        return ApiResponse.ok(planService.findAllPlans());
    }

    @GetMapping("/family/{familyId}")
    public ApiResponse<List<ImprovementPlan>> getPlansByFamily(@PathVariable Long familyId) {
        return ApiResponse.ok(planService.findByFamilyId(familyId));
    }

    @GetMapping("/{id}")
    public ApiResponse<ImprovementPlan> getPlanById(@PathVariable Long id) {
        return ApiResponse.ok(planService.findPlanById(id));
    }

    @PostMapping
    public ApiResponse<ImprovementPlan> createPlan(@RequestBody ImprovementPlan plan) {
        return ApiResponse.ok(planService.createPlan(plan));
    }

    @PutMapping("/{id}")
    public ApiResponse<ImprovementPlan> updatePlan(@PathVariable Long id, @RequestBody ImprovementPlan plan) {
        return ApiResponse.ok(planService.updatePlan(id, plan));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePlan(@PathVariable Long id) {
        planService.deletePlan(id);
        return ApiResponse.ok(null);
    }

    // --- Tareas ---

    @GetMapping("/tasks")
    public ApiResponse<List<PlanTask>> getAllTasks() {
        return ApiResponse.ok(planService.findAllTasks());
    }

    @GetMapping("/tasks/{id}")
    public ApiResponse<PlanTask> getTaskById(@PathVariable Long id) {
        return ApiResponse.ok(planService.findTaskById(id));
    }

    @PostMapping("/tasks")
    public ApiResponse<PlanTask> createTask(@RequestBody PlanTask task) {
        return ApiResponse.ok(planService.createTask(task));
    }

    @PutMapping("/tasks/{id}")
    public ApiResponse<PlanTask> updateTask(@PathVariable Long id, @RequestBody PlanTask task) {
        return ApiResponse.ok(planService.updateTask(id, task));
    }

    @PutMapping("/tasks/{id}/complete")
    public ApiResponse<PlanTask> completeTask(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        boolean completed = Boolean.TRUE.equals(body.get("completed"));
        return ApiResponse.ok(planService.completeTask(id, completed));
    }

    @DeleteMapping("/tasks/{id}")
    public ApiResponse<Void> deleteTask(@PathVariable Long id) {
        planService.deleteTask(id);
        return ApiResponse.ok(null);
    }
}
