package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByFamilyIdOrderByCreatedAtAsc(Long familyId);
    List<ChatMessageSummary> findProjectedByFamilyIdOrderByCreatedAtAsc(Long familyId);
    List<ChatMessage> findByFamilyIdOrderByCreatedAtDesc(Long familyId, Pageable pageable);

    @Query("SELECT m.emotionalSnapshot FROM ChatMessage m WHERE m.sessionId = :sessionId AND m.ai = false AND m.emotionalSnapshot IS NOT NULL ORDER BY m.createdAt DESC")
    List<String> findRecentUserSnapshotsForSession(@Param("sessionId") Long sessionId, Pageable pageable);
}
