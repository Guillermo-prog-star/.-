package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.Filter;

/**
 * SDD Sprint 5: Snapshot histórico de métricas operativas de convivencia familiar.
 */
@Entity
@Table(name = "family_metrics_snapshots")
@Filter(name = "familyFilter", condition = "family_id = :familyId")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyMetricsSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "snapshot_date")
    private LocalDate snapshotDate;

    @Column(name = "convivence_index")
    private Double convivenceIndex;

    @Column(name = "risk_level", length = 50)
    private String riskLevel;

    private Double adherence; // ej. 78.5 (%)

    private Double participation; // ej. 65.0 (%)

    @Column(name = "emotions_score")
    private Double emotionsScore;

    @Column(name = "communication_score")
    private Double communicationScore;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
