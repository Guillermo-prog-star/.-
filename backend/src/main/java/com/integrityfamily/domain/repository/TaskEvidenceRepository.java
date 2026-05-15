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
}
