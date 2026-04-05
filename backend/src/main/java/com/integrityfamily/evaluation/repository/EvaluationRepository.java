package com.integrityfamily.evaluation.repository;

import com.integrityfamily.evaluation.domain.Evaluation;
import com.integrityfamily.evaluation.domain.EvaluationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    // Para el historial de William
    List<Evaluation> findByMemberIdOrderByStartedAtDesc(Long memberId);

    // Para el Dashboard de la Familia López (útil para AnalyticsService)
    List<Evaluation> findByFamilyIdOrderByStartedAtDesc(Long familyId);

    /**
     * MÉTODO CLAVE PARA EL DASHBOARD:
     * Busca la evaluación más reciente de la familia que ya esté terminada.
     */
    Optional<Evaluation> findFirstByFamilyIdAndStatusOrderByFinalizedAtDesc(
        Long familyId, 
        EvaluationStatus status
    );
}