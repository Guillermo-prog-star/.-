package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.HypothesisEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HypothesisEvidenceRepository extends JpaRepository<HypothesisEvidence, Long> {

    /** Observaciones de una hipótesis para un sujeto, en orden cronológico — base para análisis posterior (ADR-004). */
    List<HypothesisEvidence> findByHypothesisAndSubjectTypeAndSubjectIdOrderByObservedAtAsc(
            String hypothesis, String subjectType, Long subjectId);
}
