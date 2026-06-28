package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.TrajectoryImpactIndicator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrajectoryImpactIndicatorRepository extends JpaRepository<TrajectoryImpactIndicator, Long> {
    List<TrajectoryImpactIndicator> findByFamilyTrajectoryId(Long familyTrajectoryId);
    Optional<TrajectoryImpactIndicator> findByFamilyTrajectoryIdAndIndicatorKey(Long familyTrajectoryId, String indicatorKey);
}
