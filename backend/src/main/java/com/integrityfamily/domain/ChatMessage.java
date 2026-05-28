package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import org.hibernate.annotations.Filter;

@Entity
@Table(name = "chat_messages")
@Filter(name = "familyFilter", condition = "family_id = :familyId")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_ai")
    @Builder.Default
    private boolean ai = false;

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "session_id")
    private Long sessionId;

    /** Instantánea del estado emocional detectado en el turno del usuario (CALM, ANXIOUS, …) */
    @Column(name = "emotional_snapshot", length = 20)
    private String emotionalSnapshot;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
