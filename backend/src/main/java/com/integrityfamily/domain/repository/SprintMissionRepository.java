package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.SprintMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SprintMissionRepository extends JpaRepository<SprintMission, Long> {
    List<SprintMission> findBySprintId(Long sprintId);
}
