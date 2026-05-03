package com.integrityfamily.plan.service;

import com.integrityfamily.domain.Plan;
import com.integrityfamily.domain.PlanTask;
import com.integrityfamily.domain.repository.PlanRepository;
import com.integrityfamily.domain.repository.PlanTaskRepository;
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

    /** Planes de una familia ordenados por fecha (mÃƒÂ¡s recientes primero) */
    public List<Plan> findByFamilyId(Long familyId) {
        return planRepository.findByFamilyIdOrderByCreatedAtDesc(familyId);
    }

    public Plan findPlanById(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan no encontrado: " + id));
    }

    public Plan createPlan(Plan plan) {
        return planRepository.save(plan);
    }

    public Plan updatePlan(Long id, Plan request) {
        Plan existing = planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan no encontrado: " + id));
        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        return planRepository.save(existing);
    }

    public void deletePlan(Long id) {
        if (!planRepository.existsById(id)) {
            throw new RuntimeException("Plan no encontrado: " + id);
        }
        planRepository.deleteById(id);
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬ Tareas Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    public List<PlanTask> findAllTasks() {
        return planTaskRepository.findAll();
    }

    public PlanTask findTaskById(Long id) {
        return planTaskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada: " + id));
    }

    public PlanTask createTask(PlanTask task) {
        return planTaskRepository.save(task);
    }

    public PlanTask updateTask(Long id, PlanTask request) {
        PlanTask existing = planTaskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada: " + id));
        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        existing.setCompleted(request.getCompleted());
        return planTaskRepository.save(existing);
    }

    /** Marca o desmarca una tarea como completada */
    public PlanTask completeTask(Long id, boolean completed) {
        PlanTask task = planTaskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada: " + id));
        task.setCompleted(completed);
        return planTaskRepository.save(task);
    }

    public void deleteTask(Long id) {
        if (!planTaskRepository.existsById(id)) {
            throw new RuntimeException("Tarea no encontrada: " + id);
        }
        planTaskRepository.deleteById(id);
    }
}


