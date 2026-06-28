package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.SprintMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SprintMissionRepository extends JpaRepository<SprintMission, Long> {
    List<SprintMission> findBySprintId(Long sprintId);

    /** IND-09: misiones completadas de todos los sprints de la familia */
    @Query("SELECT COUNT(m) FROM SprintMission m " +
           "WHERE m.sprint.family.id = :familyId AND m.completedAt IS NOT NULL")
    long countCompletedByFamilyId(@Param("familyId") Long familyId);

    /** IND-09: total misiones de todos los sprints de la familia */
    @Query("SELECT COUNT(m) FROM SprintMission m WHERE m.sprint.family.id = :familyId")
    long countTotalByFamilyId(@Param("familyId") Long familyId);
}
