package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "risk_trajectories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskTrajectory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RiskMacrodomain macrodomain;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "early_signals", columnDefinition = "JSON")
    private String earlySignals;

    @Column(name = "potential_evolution", columnDefinition = "TEXT")
    private String potentialEvolution;

    @Column(name = "severity_default", nullable = false, length = 20)
    private String severityDefault;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (active == null) active = true;
    }
}
