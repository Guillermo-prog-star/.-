package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "trajectory_timeline_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrajectoryTimelineEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_trajectory_id", nullable = false)
    private FamilyRiskTrajectory familyTrajectory;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "age_at_event")
    private Integer ageAtEvent;

    @Column(name = "event_description", nullable = false, columnDefinition = "TEXT")
    private String eventDescription;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(name = "action_taken", columnDefinition = "TEXT")
    private String actionTaken;

    @Column(columnDefinition = "TEXT")
    private String result;

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;

    @Column(name = "recorded_by", length = 255)
    private String recordedBy;

    @PrePersist
    public void prePersist() {
        if (recordedAt == null) recordedAt = LocalDateTime.now();
        if (riskLevel == null) riskLevel = "MEDIUM";
    }
}
