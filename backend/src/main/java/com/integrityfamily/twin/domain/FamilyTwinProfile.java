package com.integrityfamily.twin.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "family_twin_profiles")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FamilyTwinProfile {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false, unique = true)
    private Long familyId;

    // Huella conductual
    @Column(name = "behavioral_signature", columnDefinition = "TEXT")
    private String behavioralSignature;

    @Column(name = "communication_pattern", length = 60)
    private String communicationPattern;

    @Column(name = "resilience_index")
    private Double resilienceIndex;

    @Column(name = "bonding_rhythm", length = 60)
    private String bondingRhythm;

    @Column(name = "dominant_strength", length = 100)
    private String dominantStrength;

    @Column(name = "dominant_vulnerability", length = 100)
    private String dominantVulnerability;

    // Patrones y correlaciones
    @Column(name = "detected_patterns", columnDefinition = "JSON")
    private String detectedPatterns;

    @Column(name = "correlations", columnDefinition = "JSON")
    private String correlations;

    @Column(name = "active_predictions", columnDefinition = "JSON")
    private String activePredictions;

    // Ciclo familiar
    @Column(name = "avg_days_between_crises") private Integer avgDaysBetweenCrises;
    @Column(name = "avg_recovery_days")       private Integer avgRecoveryDays;
    @Column(name = "peak_activity_day", length = 20) private String peakActivityDay;

    @Column(name = "computed_at", nullable = false) @Builder.Default
    private LocalDateTime computedAt = LocalDateTime.now();

    @Column(name = "data_richness", length = 20) @Builder.Default
    private String dataRichness = "LOW";

    @PrePersist @PreUpdate
    public void touch() {
        if (computedAt == null) computedAt = LocalDateTime.now();
    }
}
