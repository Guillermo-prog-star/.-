package com.integrityfamily.context.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "family_context_snapshots")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FamilyContextSnapshot {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false, unique = true)
    private Long familyId;

    // ── Señales de estado ────────────────────────────────────────────────────
    @Column(name = "connection_level",    nullable = false, length = 20) @Builder.Default
    private String connectionLevel    = "MEDIA";

    @Column(name = "stress_level",        nullable = false, length = 20) @Builder.Default
    private String stressLevel        = "BAJO";

    @Column(name = "communication_trend", nullable = false, length = 20) @Builder.Default
    private String communicationTrend = "ESTABLE";

    @Column(name = "participation_level", nullable = false, length = 20) @Builder.Default
    private String participationLevel = "MEDIA";

    @Column(name = "overall_trend",       nullable = false, length = 20) @Builder.Default
    private String overallTrend       = "ESTABLE";

    @Column(name = "overall_mood",        nullable = false, length = 30) @Builder.Default
    private String overallMood        = "SERENO";

    // ── Métricas clave ───────────────────────────────────────────────────────
    @Column(name = "icf_current")
    private Double icfCurrent;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(name = "days_without_activity", nullable = false) @Builder.Default
    private Integer daysWithoutActivity = 0;

    @Column(name = "current_streak", nullable = false) @Builder.Default
    private Integer currentStreak = 0;

    @Column(name = "active_rituals_count", nullable = false) @Builder.Default
    private Integer activeRitualsCount = 0;

    @Column(name = "sprint_progress")
    private Double sprintProgress;

    // ── Listas JSON ──────────────────────────────────────────────────────────
    @Column(columnDefinition = "JSON")
    private String alerts;

    @Column(columnDefinition = "JSON")
    private String recommendations;

    @Column(name = "computed_at", nullable = false) @Builder.Default
    private LocalDateTime computedAt = LocalDateTime.now();

    @PrePersist @PreUpdate
    public void touch() {
        if (computedAt == null) computedAt = LocalDateTime.now();
    }
}
