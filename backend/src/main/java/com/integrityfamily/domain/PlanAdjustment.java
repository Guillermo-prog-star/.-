package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SDD Sprint 7: Entidad de Ajuste Adaptativo Gobernada (PlanAdjustment).
 */
@Entity
@Table(name = "plan_adjustments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_plan_id", nullable = false)
    private ImprovementPlan familyPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_inference_id")
    private AiInferenceEntity sourceInference;

    @Column(name = "adjustment_type", length = 50)
    private String adjustmentType; // REDUCE_LOAD, SOFT_RESET, GUIDED_LISTENING, PAUSE_NON_CRITICAL, MAINTAIN

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private AdjustmentStatus status = AdjustmentStatus.PROPOSED;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by", length = 120)
    private String approvedBy;

    @OneToMany(mappedBy = "planAdjustment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MissionAdjustment> missionAdjustments = new ArrayList<>();
}
