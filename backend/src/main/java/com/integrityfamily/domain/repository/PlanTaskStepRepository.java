package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.PlanTaskStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanTaskStepRepository extends JpaRepository<PlanTaskStep, Long> {
}
