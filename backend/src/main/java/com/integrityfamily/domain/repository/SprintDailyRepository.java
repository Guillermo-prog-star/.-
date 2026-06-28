package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.SprintDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SprintDailyRepository extends JpaRepository<SprintDaily, Long> {
    List<SprintDaily> findBySprintIdOrderByCheckinDateDesc(Long sprintId);

    boolean existsBySprintIdAndMemberNameAndCheckinDate(Long sprintId, String memberName, LocalDate checkinDate);

    /** IND-02: dailies realizados en sprints de la familia en un rango */
    @Query("SELECT COUNT(d) FROM SprintDaily d WHERE d.sprint.family.id = :familyId " +
           "AND d.checkinDate >= :from AND d.checkinDate <= :to")
    long countByFamilyIdAndDateRange(@Param("familyId") Long familyId,
                                     @Param("from") LocalDate from,
                                     @Param("to") LocalDate to);

    /** IND-07: miembros distintos con daily en los últimos N días */
    @Query("SELECT COUNT(DISTINCT d.memberName) FROM SprintDaily d " +
           "WHERE d.sprint.family.id = :familyId AND d.createdAt >= :since")
    long countDistinctMembersWithDailySince(@Param("familyId") Long familyId,
                                             @Param("since") LocalDateTime since);
}
