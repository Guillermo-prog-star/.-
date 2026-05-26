package com.integrityfamily.analytics.domain;

import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.Family;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "progress_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgressSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "current_evaluation_id", nullable = false)
    private Evaluation currentEvaluation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_evaluation_id")
    private Evaluation previousEvaluation;

    @Column(name = "milestone_code", length = 50)
    private String milestoneCode;

    @Column(name = "previous_icf")
    private Double previousIcf;

    @Column(name = "current_icf")
    private Double currentIcf;

    @Column(name = "delta_icf")
    private Double deltaIcf;

    @Column(length = 30)
    private String classification; // INICIAL, MEJORA_FUERTE, MEJORA_LEVE, ESTANCAMIENTO, DETERIORO

    @Column(columnDefinition = "TEXT")
    private String interpretation;

    @ElementCollection
    @CollectionTable(name = "progress_snapshot_dimensions", joinColumns = @JoinColumn(name = "snapshot_id"))
    @MapKeyColumn(name = "dimension_name")
    @Column(name = "delta_score")
    @Builder.Default
    private Map<String, Double> dimensionEvolution = new HashMap<>();

    @Column(columnDefinition = "TEXT")
    private String recommendedAction;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void pre() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
