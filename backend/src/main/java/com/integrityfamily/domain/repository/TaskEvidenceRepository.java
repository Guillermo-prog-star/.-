package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.TaskEvidence;
import com.integrityfamily.domain.EvidenceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * SDD SPEC 6.5: Repositorio JPA para persistencia de evidencias.
 */
@Repository
public interface TaskEvidenceRepository extends JpaRepository<TaskEvidence, Long> {
    
    List<TaskEvidence> findByFamilyId(Long familyId);

    List<TaskEvidence> findByTaskId(Long taskId);

    List<TaskEvidence> findByFamilyIdAndStatus(Long familyId, EvidenceStatus status);

    /** IND-09: evidencias validadas (validated=true) por familia */
    @org.springframework.data.jpa.repository.Query(
        "SELECT COUNT(e) FROM TaskEvidence e WHERE e.family.id = :familyId AND e.validated = true")
    long countValidatedByFamilyId(
        @org.springframework.data.repository.query.Param("familyId") Long familyId);

    /** IND-07: submittedBy distintos en los últimos N días */
    @org.springframework.data.jpa.repository.Query(
        "SELECT COUNT(DISTINCT e.submittedBy) FROM TaskEvidence e " +
        "WHERE e.family.id = :familyId AND e.createdAt >= :since")
    long countDistinctSubmittersSince(
        @org.springframework.data.repository.query.Param("familyId") Long familyId,
        @org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);
}
