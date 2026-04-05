package com.integrityfamily.plan.service;

import com.integrityfamily.plan.domain.Plan;
import com.integrityfamily.plan.domain.PlanTask;
import com.integrityfamily.plan.repository.PlanRepository;
import com.integrityfamily.plan.repository.PlanTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final PlanTaskRepository planTaskRepository;

    public List<Plan> findAllPlans() {
        return planRepository.findAll();
    }

    public Plan findPlanById(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan no encontrado"));
    }

    public Plan createPlan(Plan plan) {
        return planRepository.save(plan);
    }

    public Plan updatePlan(Long id, Plan request) {
        Plan existing = planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan no encontrado"));

        return planRepository.save(existing);
    }

    public void deletePlan(Long id) {
        if (!planRepository.existsById(id)) {
            throw new RuntimeException("Plan no encontrado");
        }
        planRepository.deleteById(id);
    }

    public List<PlanTask> findAllTasks() {
        return planTaskRepository.findAll();
    }

    public PlanTask findTaskById(Long id) {
        return planTaskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada"));
    }

    public PlanTask createTask(PlanTask task) {
        return planTaskRepository.save(task);
    }

    public PlanTask updateTask(Long id, PlanTask request) {
        PlanTask existing = planTaskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada"));

        return planTaskRepository.save(existing);
    }

    public void deleteTask(Long id) {
        if (!planTaskRepository.existsById(id)) {
            throw new RuntimeException("Tarea no encontrada");
        }
        planTaskRepository.deleteById(id);
    }
}