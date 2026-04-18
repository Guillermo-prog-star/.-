package com.integrityfamily.plan.repository;

import com.integrityfamily.plan.domain.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * PlanRepository: Gestiona el acceso a datos de los planes de acción.
 * Sincronizado con la propiedad 'createdAt' de la entidad Plan.
 */
@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {

    /**
     * Recupera los planes de una familia ordenados por fecha de creación (más recientes primero).
     * Requisito: La entidad Plan debe tener el atributo 'createdAt'.
     */
    List<Plan> findByFamilyIdOrderByCreatedAtDesc(Long familyId);

    /**
     * Busca planes asociados a una evaluación específica.
     */
    List<Plan> findByEvaluationId(Long evaluationId);

    /**
     * Verifica si existe algún plan para una familia específica.
     */
    boolean existsByFamilyId(Long familyId);
    boolean existsByEvaluationId(Long evaluationId);
}