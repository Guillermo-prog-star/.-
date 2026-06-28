package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "family_risk_trajectories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyRiskTrajectory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trajectory_id", nullable = false)
    private RiskTrajectory trajectory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TrajectoryStatus status;

    @Column(name = "detected_at")
    private LocalDateTime detectedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "assigned_by", length = 255)
    private String assignedBy;

    @PrePersist
    public void prePersist() {
        if (detectedAt == null) detectedAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (status == null) status = TrajectoryStatus.DETECTED;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
