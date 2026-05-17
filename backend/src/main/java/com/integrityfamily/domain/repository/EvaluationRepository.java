package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.EvaluationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
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
    List<EvaluationSummary> findSummaryByFamilyId(Long familyId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"dimensionScores"})
    List<Evaluation> findWithScoresByFamilyId(Long familyId);
}
