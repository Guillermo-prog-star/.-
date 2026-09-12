package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.ImprovementPlan;
import com.integrityfamily.domain.PlanAcceptanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ImprovementPlanRepository extends JpaRepository<ImprovementPlan, Long> {
    List<ImprovementPlan> findByFamilyId(Long familyId);

    // SDD: Verifica existencia por evaluación base
    boolean existsByEvaluationId(Long evaluationId);

    // Reporte operativo semanal: familia "activa" = plan con aceptacion explicita (ADR-010)
    List<ImprovementPlan> findByAcceptanceStatus(PlanAcceptanceStatus acceptanceStatus);
}
