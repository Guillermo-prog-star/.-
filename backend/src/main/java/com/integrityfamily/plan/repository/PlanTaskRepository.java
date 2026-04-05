package com.integrityfamily.plan.repository;

import com.integrityfamily.plan.domain.PlanTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * PlanTaskRepository: Gestiona el acceso a datos de las tareas de bienestar.
 * Optimizada para el ordenamiento cronológico basado en el nuevo campo 'createdAt'.
 */
@Repository
public interface PlanTaskRepository extends JpaRepository<PlanTask, Long> {

    /**
     * Recupera todas las tareas de un plan específico ordenadas por fecha de creación.
     * Esto permite que William vea la secuencia de pasos de forma lógica.
     */
    List<PlanTask> findByPlanIdOrderByCreatedAtAsc(Long planId);

    /**
     * Busca tareas pendientes (no completadas) dentro de un plan.
     */
    List<PlanTask> findByPlanIdAndCompletedFalse(Long planId);

    /**
     * Cuenta las tareas completadas para calcular el progreso del plan en el Dashboard.
     */
    long countByPlanIdAndCompletedTrue(Long planId);
}