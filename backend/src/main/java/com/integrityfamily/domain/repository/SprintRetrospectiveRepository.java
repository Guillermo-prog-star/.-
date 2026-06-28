package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.SprintRetrospective;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SprintRetrospectiveRepository extends JpaRepository<SprintRetrospective, Long> {
    Optional<SprintRetrospective> findBySprintId(Long sprintId);

    /** IND-10: sprints de la familia que tienen retrospectiva */
    @Query("SELECT COUNT(r) FROM SprintRetrospective r WHERE r.sprint.family.id = :familyId")
    long countByFamilyId(@Param("familyId") Long familyId);
}
