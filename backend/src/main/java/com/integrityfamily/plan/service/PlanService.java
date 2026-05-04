package com.integrityfamily.plan.service;

import com.integrityfamily.domain.ImprovementPlan;
import com.integrityfamily.domain.PlanTask;
import com.integrityfamily.domain.repository.ImprovementPlanRepository;
import com.integrityfamily.domain.repository.PlanTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * SDD: Servicio de Planificación Harmonizado.
 * Gestiona el ciclo de vida de los planes de transformación familiar.
 */
@Service
@RequiredArgsConstructor
public class PlanService {

    private final ImprovementPlanRepository planRepository;
    private final PlanTaskRepository planTaskRepository;

    public List<ImprovementPlan> findAllPlans() {
        return planRepository.findAll();
    }

    public List<ImprovementPlan> findByFamilyId(Long familyId) {
        return planRepository.findByFamilyId(familyId);
    }

    public ImprovementPlan findPlanById(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan no encontrado: " + id));
    }

    public ImprovementPlan createPlan(ImprovementPlan plan) {
        return planRepository.save(plan);
    }

    public ImprovementPlan updatePlan(Long id, ImprovementPlan request) {
        ImprovementPlan existing = findPlanById(id);
        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        return planRepository.save(existing);
    }

    public void deletePlan(Long id) {
        planRepository.deleteById(id);
    }

    // --- Tareas ---

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
        PlanTask existing = findTaskById(id);
        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        existing.setCompleted(request.isCompleted());
        return planTaskRepository.save(existing);
    }

    public PlanTask completeTask(Long id, boolean completed) {
        PlanTask task = findTaskById(id);
        task.setCompleted(completed);
        return planTaskRepository.save(task);
    }

    public void deleteTask(Long id) {
        planTaskRepository.deleteById(id);
    }
}
