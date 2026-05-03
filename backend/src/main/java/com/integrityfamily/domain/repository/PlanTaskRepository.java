package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.PlanTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PlanTaskRepository extends JpaRepository<PlanTask, Long> {
    List<PlanTask> findByPlanIdOrderByCreatedAtAsc(Long planId);
    List<PlanTask> findByPlanIdAndCompletedFalse(Long planId);
    long countByPlanIdAndCompletedTrue(Long planId);
}
