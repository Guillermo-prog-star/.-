package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trajectory_impact_indicators")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrajectoryImpactIndicator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_trajectory_id", nullable = false)
    private FamilyRiskTrajectory familyTrajectory;

    @Column(name = "indicator_name", nullable = false, length = 200)
    private String indicatorName;

    @Column(name = "indicator_key", nullable = false, length = 100)
    private String indicatorKey;

    @Column(name = "baseline_value", precision = 10, scale = 2)
    private BigDecimal baselineValue;

    @Column(name = "current_value", precision = 10, scale = 2)
    private BigDecimal currentValue;

    @Column(length = 50)
    private String unit;

    @Column(name = "higher_is_better", nullable = false)
    private Boolean higherIsBetter;

    @Column(name = "measured_at")
    private LocalDateTime measuredAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    public void prePersist() {
        if (measuredAt == null) measuredAt = LocalDateTime.now();
        if (higherIsBetter == null) higherIsBetter = true;
    }
}
