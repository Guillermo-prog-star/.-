package com.integrityfamily.weeklyplan.repository;

import com.integrityfamily.weeklyplan.domain.WeeklyPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WeeklyPlanRepository extends JpaRepository<WeeklyPlan, Long> {
    List<WeeklyPlan> findByFamilyIdOrderBySprintNumberAscPhaseAsc(Long familyId);
    Optional<WeeklyPlan> findByFamilyIdAndSprintNumberAndPhase(Long familyId, int sprintNumber, String phase);
}
