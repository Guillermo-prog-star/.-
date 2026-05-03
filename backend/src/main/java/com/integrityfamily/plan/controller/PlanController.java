package com.integrityfamily.plan.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.domain.Plan;
import com.integrityfamily.domain.PlanTask;
import com.integrityfamily.plan.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    // Ã¢â€â‚¬Ã¢â€â‚¬ Planes Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    @GetMapping
    public ApiResponse<List<Plan>> getAllPlans() {
        return ApiResponse.ok(planService.findAllPlans());
    }

    /** Endpoint que usa el frontend: GET /api/plans/family/{familyId} */
    @GetMapping("/family/{familyId}")
    public ApiResponse<List<Plan>> getPlansByFamily(@PathVariable Long familyId) {
        return ApiResponse.ok(planService.findByFamilyId(familyId));
    }

    @GetMapping("/{id}")
    public ApiResponse<Plan> getPlanById(@PathVariable Long id) {
        return ApiResponse.ok(planService.findPlanById(id));
    }

    @PostMapping
    public ApiResponse<Plan> createPlan(@RequestBody Plan plan) {
        return ApiResponse.ok(planService.createPlan(plan));
    }

    @PutMapping("/{id}")
    public ApiResponse<Plan> updatePlan(@PathVariable Long id, @RequestBody Plan plan) {
        return ApiResponse.ok(planService.updatePlan(id, plan));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePlan(@PathVariable Long id) {
        planService.deletePlan(id);
        return ApiResponse.ok(null);
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬ Tareas Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

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

    /** Endpoint que usa el frontend: PUT /api/plans/tasks/{taskId}/complete */
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


