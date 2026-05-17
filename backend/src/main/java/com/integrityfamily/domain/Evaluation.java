package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SDD: Entidad de Evaluación.
 * Refactorizada con campos para trazabilidad del algoritmo oficial RISK_ALGO_V1.
 */
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@Entity
@Table(name = "evaluations")
@FilterDef(name = "familyFilter", parameters = @ParamDef(name = "familyId", type = Long.class))
@Filter(name = "familyFilter", condition = "family_id = :familyId")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Family family;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private FamilyMember member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EvaluationStatus status;

    private LocalDateTime startedAt;
    private LocalDateTime finalizedAt;

    @Builder.Default
    private Boolean hasCrisis = false;

    private Double icf; // Índice Saludable General (healthyIndex)

    @Column(length = 30)
    private String riskLevel; // LOW, MODERATE, HIGH, CRITICAL

    @Column(length = 50)
    private String criticalDimension; // Dimensión más vulnerable

    @Column(length = 50)
    @Builder.Default
    private String algorithmVersion = "RISK_ALGO_V1";

    @Column(length = 50)
    private String milestoneKey;

    @Column(columnDefinition = "TEXT")
    private String spiritualSynthesis;

    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<EvaluationAnswer> answers = new ArrayList<>();

    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<EvaluationDimensionScore> dimensionScores = new ArrayList<>();

    @PrePersist
    public void pre() {
        if (startedAt == null)
            startedAt = LocalDateTime.now();
        if (status == null)
            status = EvaluationStatus.STARTED;
        if (algorithmVersion == null)
            algorithmVersion = "RISK_ALGO_V1";
    }
}
