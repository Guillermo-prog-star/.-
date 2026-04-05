package com.integrityfamily.assessment.repository;

import com.integrityfamily.assessment.domain.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    Optional<Assessment> findFirstByFamilyIdOrderByCreatedAtDesc(Long familyId);
    List<Assessment> findByFamilyIdOrderByCreatedAtAsc(Long familyId);
}