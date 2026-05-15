package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.PlanTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanTemplateRepository extends JpaRepository<PlanTemplate, String> {
    List<PlanTemplate> findByDimension(String dimension);
    List<PlanTemplate> findByRiskLevel(String riskLevel);
    List<PlanTemplate> findByDimensionAndRiskLevel(String dimension, String riskLevel);
}
