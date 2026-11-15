package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sprint_dailies")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class SprintDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sprint_id", nullable = false)
    private FamilySprint sprint;

    @Column(name = "member_name", nullable = false, length = 120)
    private String memberName;

    @Column(name = "checkin_date", nullable = false)
    private LocalDate checkinDate;

    @Column(name = "yesterday_text", nullable = false, columnDefinition = "TEXT")
    private String yesterdayText;

    @Column(name = "today_text", nullable = false, columnDefinition = "TEXT")
    private String todayText;

    @Column(name = "blockages_text", nullable = false, columnDefinition = "TEXT")
    private String blockagesText;

    @Column(name = "resolution_text", nullable = false, columnDefinition = "TEXT")
    private String resolutionText;

    @Column(name = "emotional_indicator", length = 50)
    private String emotionalIndicator; // e.g. HAPPY, CALM, TIRED, STRESSED

    /** V114 — null = no respondido (también todos los check-ins previos). */
    @Enumerated(EnumType.STRING)
    @Column(name = "attempt_status", length = 30)
    private AttemptStatus attemptStatus;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
