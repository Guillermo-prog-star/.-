package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.PlanTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanTaskRepository extends JpaRepository<PlanTask, Long> {

    // ── Por hito (para MilestoneService) ───────────────────────────────────

    @org.springframework.data.jpa.repository.Query(
        "SELECT COUNT(t) FROM PlanTask t WHERE t.plan.family.id = :familyId AND t.milestoneCode = :milestoneCode")
    long countByFamilyIdAndMilestoneCode(
        @org.springframework.data.repository.query.Param("familyId") Long familyId,
        @org.springframework.data.repository.query.Param("milestoneCode") String milestoneCode);

    @org.springframework.data.jpa.repository.Query(
        "SELECT COUNT(t) FROM PlanTask t WHERE t.plan.family.id = :familyId AND t.milestoneCode = :milestoneCode AND t.completed = true")
    long countCompletedByFamilyIdAndMilestoneCode(
        @org.springframework.data.repository.query.Param("familyId") Long familyId,
        @org.springframework.data.repository.query.Param("milestoneCode") String milestoneCode);

    // ── Globales (para el Dashboard) ────────────────────────────────────────

    /** Total de tareas del plan de la familia (todas las tareas, todos los hitos). */
    @org.springframework.data.jpa.repository.Query(
        "SELECT COUNT(t) FROM PlanTask t WHERE t.plan.family.id = :familyId")
    long countByFamilyId(
        @org.springframework.data.repository.query.Param("familyId") Long familyId);

    /** Tareas completadas del plan de la familia. */
    @org.springframework.data.jpa.repository.Query(
        "SELECT COUNT(t) FROM PlanTask t WHERE t.plan.family.id = :familyId AND t.completed = true")
    long countCompletedByFamilyId(
        @org.springframework.data.repository.query.Param("familyId") Long familyId);

    /** IND-12: promedio de impacto_icf de tareas completadas del plan más reciente */
    @org.springframework.data.jpa.repository.Query("""
        SELECT AVG(t.impactoIcf) FROM PlanTask t
        WHERE t.plan.id = (
            SELECT p.id FROM ImprovementPlan p WHERE p.family.id = :familyId
            ORDER BY p.id DESC LIMIT 1
        )
        AND t.completed = true AND t.impactoIcf IS NOT NULL
""")
    Double avgImpactoIcfCompletedByFamilyId(
        @org.springframework.data.repository.query.Param("familyId") Long familyId);
}
