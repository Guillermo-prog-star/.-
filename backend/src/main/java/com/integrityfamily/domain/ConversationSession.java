package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "conversation_sessions", indexes = {
    @Index(name = "idx_cs_family_member",  columnList = "family_id, member_id"),
    @Index(name = "idx_cs_family_started", columnList = "family_id, started_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "member_id")
    private Long memberId;

    /** GENERAL | SUPPORT | REFLECTION | PLANNING | GUARDIAN_SYNC */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String goal = "GENERAL";

    /** CALM | ANXIOUS | FRUSTRATED | HOPEFUL | CONFUSED | ENGAGED */
    @Column(name = "emotional_state", length = 20)
    private String emotionalState;

    @Column(name = "turn_count", nullable = false)
    @Builder.Default
    private int turnCount = 0;

    /** COMPLETED | ABANDONED | ESCALATED */
    @Column(length = 30)
    private String outcome;

    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    public boolean isActive() {
        return endedAt == null;
    }
}
