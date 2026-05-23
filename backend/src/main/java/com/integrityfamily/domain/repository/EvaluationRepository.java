package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.EvaluationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    List<Evaluation> findByFamilyId(Long familyId);
    List<Evaluation> findByFamilyIdOrderByStartedAtDesc(Long familyId);
    Optional<Evaluation> findFirstByFamilyIdAndStatusOrderByFinalizedAtDesc(Long familyId, EvaluationStatus status);
    Optional<Evaluation> findTopByFamilyIdAndStatusOrderByFinalizedAtDesc(Long familyId, EvaluationStatus status);
    List<Evaluation> findByFamilyIdOrderByFinalizedAtAsc(Long familyId);
    /**
     * Closed projection: alias JPQL → getter name.
     * Una sola consulta sin lazy-load: accede a family.id y member.id directamente
     * desde las columnas FK (family_id, member_id) sin inicializar proxies.
     */
    @Query("""
        SELECT e.id                AS id,
               e.family.id         AS familyId,
               e.member.id         AS memberId,
               e.status            AS status,
               e.startedAt         AS startedAt,
               e.finalizedAt       AS finalizedAt,
               e.icf               AS icf,
               e.riskLevel         AS riskLevel,
               e.criticalDimension AS criticalDimension
        FROM Evaluation e
        WHERE e.family.id = :familyId
        ORDER BY e.startedAt DESC
        """)
    List<EvaluationSummary> findSummaryByFamilyId(@Param("familyId") Long familyId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"dimensionScores"})
    List<Evaluation> findWithScoresByFamilyId(Long familyId);

    // Para calcular ICF promedio en el hito actual
    List<Evaluation> findByFamilyIdAndMilestoneKeyAndStatus(
        Long familyId, String milestoneKey, EvaluationStatus status);
}
