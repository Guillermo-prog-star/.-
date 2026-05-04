package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.PlanTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanTaskRepository extends JpaRepository<PlanTask, Long> {
}
