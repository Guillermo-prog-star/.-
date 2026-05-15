package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.PlanTemplateActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanTemplateActivityRepository extends JpaRepository<PlanTemplateActivity, Long> {
    List<PlanTemplateActivity> findByTemplateCode(String templateCode);
}
