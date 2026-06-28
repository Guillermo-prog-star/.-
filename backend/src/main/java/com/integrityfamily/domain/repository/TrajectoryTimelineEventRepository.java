package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.TrajectoryTimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrajectoryTimelineEventRepository extends JpaRepository<TrajectoryTimelineEvent, Long> {
    List<TrajectoryTimelineEvent> findByFamilyTrajectoryIdOrderByEventDateAsc(Long familyTrajectoryId);
}
