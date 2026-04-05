package com.integrityfamily.plan.controller;

import com.integrityfamily.plan.domain.Plan;
import com.integrityfamily.plan.domain.PlanTask;
import com.integrityfamily.plan.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @GetMapping
    public List<Plan> getAllPlans() {
        return planService.findAllPlans();
    }

    @GetMapping("/{id}")
    public Plan getPlanById(@PathVariable Long id) {
        return planService.findPlanById(id);
    }

    @PostMapping
    public Plan createPlan(@RequestBody Plan plan) {
        return planService.createPlan(plan);
    }

    @PutMapping("/{id}")
    public Plan updatePlan(@PathVariable Long id, @RequestBody Plan plan) {
        return planService.updatePlan(id, plan);
    }

    @DeleteMapping("/{id}")
    public void deletePlan(@PathVariable Long id) {
        planService.deletePlan(id);
    }

    @GetMapping("/tasks")
    public List<PlanTask> getAllTasks() {
        return planService.findAllTasks();
    }

    @GetMapping("/tasks/{id}")
    public PlanTask getTaskById(@PathVariable Long id) {
        return planService.findTaskById(id);
    }

    @PostMapping("/tasks")
    public PlanTask createTask(@RequestBody PlanTask task) {
        return planService.createTask(task);
    }

    @PutMapping("/tasks/{id}")
    public PlanTask updateTask(@PathVariable Long id, @RequestBody PlanTask task) {
        return planService.updateTask(id, task);
    }

    @DeleteMapping("/tasks/{id}")
    public void deleteTask(@PathVariable Long id) {
        planService.deleteTask(id);
    }
}