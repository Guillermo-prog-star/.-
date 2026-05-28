package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.ConversationSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ConversationSessionRepository extends JpaRepository<ConversationSession, Long> {

    /**
     * Sesiones activas (sin ended_at) para un miembro dentro de la ventana de tiempo.
     * Se limita con Pageable para tomar solo la más reciente.
     */
    @Query("""
        SELECT s FROM ConversationSession s
        WHERE s.familyId = :familyId
          AND s.memberId = :memberId
          AND s.endedAt IS NULL
          AND s.startedAt >= :since
        ORDER BY s.startedAt DESC
        """)
    List<ConversationSession> findActiveSessionsForMember(
            @Param("familyId") Long familyId,
            @Param("memberId") Long memberId,
            @Param("since") LocalDateTime since,
            Pageable pageable);

    /** Sesiones abiertas (sin ended_at) anteriores al corte de tiempo — para cierre de sesiones obsoletas. */
    @Query("""
        SELECT s FROM ConversationSession s
        WHERE s.familyId = :familyId
          AND s.memberId = :memberId
          AND s.endedAt IS NULL
          AND s.startedAt < :before
        """)
    List<ConversationSession> findOpenStaleSessionsForMember(
            @Param("familyId") Long familyId,
            @Param("memberId") Long memberId,
            @Param("before") LocalDateTime before);
}
