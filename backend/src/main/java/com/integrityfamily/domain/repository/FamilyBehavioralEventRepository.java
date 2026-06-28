package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.FamilyBehavioralEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FamilyBehavioralEventRepository extends JpaRepository<FamilyBehavioralEvent, Long> {

    List<FamilyBehavioralEvent> findByFamilyIdOrderByOccurredAtDesc(Long familyId);

    /** IND-04: eventos ocurridos en los últimos N días */
    @Query("SELECT COUNT(e) FROM FamilyBehavioralEvent e " +
           "WHERE e.family.id = :familyId AND e.occurredAt >= :since")
    long countByFamilyIdSince(@Param("familyId") Long familyId,
                               @Param("since") LocalDateTime since);

    /** IND-04: eventos reparados (repairedAt != null) en los últimos N días */
    @Query("SELECT COUNT(e) FROM FamilyBehavioralEvent e " +
           "WHERE e.family.id = :familyId AND e.occurredAt >= :since " +
           "AND e.repairedAt IS NOT NULL")
    long countRepairedByFamilyIdSince(@Param("familyId") Long familyId,
                                       @Param("since") LocalDateTime since);
}
