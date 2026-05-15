package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.PlanAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanAdjustmentRepository extends JpaRepository<PlanAdjustment, Long> {
    List<PlanAdjustment> findByFamilyPlanFamilyIdOrderByCreatedAtDesc(Long familyId);
}
