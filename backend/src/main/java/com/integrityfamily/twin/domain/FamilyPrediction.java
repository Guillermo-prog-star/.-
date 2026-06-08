package com.integrityfamily.twin.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "family_predictions")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FamilyPrediction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "prediction_type", nullable = false, length = 50)
    private String predictionType; // TENSION_RISK | GROWTH_OPPORTUNITY | COMMUNICATION_ALERT | RITUAL_READINESS | EVALUATION_DUE

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer confidence; // 0-100

    @Column(name = "time_horizon", length = 30)
    private String timeHorizon;

    @Column(name = "recommended_action", columnDefinition = "TEXT")
    private String recommendedAction;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE"; // ACTIVE | CONFIRMED | DISMISSED | EXPIRED

    @Column(name = "predicted_at", nullable = false) @Builder.Default
    private LocalDateTime predictedAt = LocalDateTime.now();

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @PrePersist
    public void prePersist() {
        if (predictedAt == null) predictedAt = LocalDateTime.now();
        if (status == null) status = "ACTIVE";
    }
}
