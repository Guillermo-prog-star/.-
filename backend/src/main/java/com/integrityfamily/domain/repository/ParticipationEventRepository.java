package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.ParticipationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ParticipationEventRepository extends JpaRepository<ParticipationEvent, Long> {

    @Query("SELECT DISTINCT e.memberId FROM ParticipationEvent e WHERE e.familyId = :familyId AND e.occurredAt >= :since AND e.memberId IS NOT NULL")
    List<Long> findActiveMemberIds(@Param("familyId") Long familyId, @Param("since") LocalDateTime since);

    @Query("SELECT MAX(e.occurredAt) FROM ParticipationEvent e WHERE e.familyId = :familyId AND e.memberId = :memberId")
    java.util.Optional<LocalDateTime> findLastActivityByMember(@Param("familyId") Long familyId, @Param("memberId") Long memberId);

    @Query("SELECT e.occurredAt FROM ParticipationEvent e WHERE e.familyId = :familyId AND e.occurredAt >= :since")
    List<LocalDateTime> findAllOccurredAt(@Param("familyId") Long familyId, @Param("since") LocalDateTime since);

    @Query("SELECT e.occurredAt, e.memberId, e.eventType FROM ParticipationEvent e WHERE e.familyId = :familyId AND e.occurredAt >= :since ORDER BY e.occurredAt DESC")
    List<Object[]> findRecentEvents(@Param("familyId") Long familyId, @Param("since") LocalDateTime since);
}
