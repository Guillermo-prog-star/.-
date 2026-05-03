package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {
    Optional<Plan> findFirstByFamilyIdOrderByCreatedAtDesc(Long familyId);
    List<Plan> findByFamilyIdOrderByCreatedAtDesc(Long familyId);
    List<Plan> findByEvaluationId(Long evaluationId);
    boolean existsByFamilyId(Long familyId);
    boolean existsByEvaluationId(Long evaluationId);
}
