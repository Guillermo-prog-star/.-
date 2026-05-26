package com.integrityfamily.scanner.repository;

import com.integrityfamily.scanner.domain.InferenceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface InferenceRecordRepository extends JpaRepository<InferenceRecord, Long> {

    List<InferenceRecord> findByFamilyIdOrderByCreatedAtDesc(Long familyId);

    Optional<InferenceRecord> findByEvaluationId(Long evaluationId);

    boolean existsByEvidenceHash(String evidenceHash);

    List<InferenceRecord> findByFamilyIdAndEpistemicState(Long familyId, String epistemicState);

    /** IF-STAB: registros INFERRED creados antes de :cutoff, candidatos a estabilización. */
    @Query("SELECT r FROM InferenceRecord r WHERE r.epistemicState = 'INFERRED' AND r.createdAt < :cutoff")
    List<InferenceRecord> findInferredBefore(@Param("cutoff") Instant cutoff);

    /** IF-STAB: conteo de registros STABILIZED de una familia (para métricas). */
    long countByFamilyIdAndEpistemicState(Long familyId, String epistemicState);
}
