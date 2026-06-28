package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.FamilyRiskTrajectory;
import com.integrityfamily.domain.TrajectoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FamilyRiskTrajectoryRepository extends JpaRepository<FamilyRiskTrajectory, Long> {
    List<FamilyRiskTrajectory> findByFamilyId(Long familyId);
    List<FamilyRiskTrajectory> findByFamilyIdAndStatus(Long familyId, TrajectoryStatus status);
    List<FamilyRiskTrajectory> findByFamilyIdAndStatusIn(Long familyId, List<TrajectoryStatus> statuses);
}
