package com.integrityfamily.evaluation.domain;

import com.integrityfamily.family.domain.Family;
import com.integrityfamily.family.domain.Member;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="evaluations")
public class Evaluation {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false, fetch=FetchType.LAZY)
    @JoinColumn(name="family_id", nullable=false)
    private Family family;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=30)
    private EvaluationStatus status;

    @Column(name="started_at")
    private LocalDateTime startedAt;

    @Column(name="finalized_at")
    private LocalDateTime finalizedAt;

    @Column(name = "has_crisis")
    private Boolean hasCrisis = false;

    @Column(name = "icf")
    private Double icf;

    @Column(name = "milestone_key", length = 50)
    private String milestoneKey;

    @OneToMany(mappedBy="evaluation", cascade=CascadeType.ALL, orphanRemoval=true)
    private List<EvaluationAnswer> answers = new ArrayList<>();

    @OneToMany(mappedBy="evaluation", cascade=CascadeType.ALL, orphanRemoval=true)
    private List<EvaluationDimensionScore> dimensionScores = new ArrayList<>();

    public Evaluation() {}

    @PrePersist
    public void pre() {
        if (startedAt == null) startedAt = LocalDateTime.now();
        if (status == null) status = EvaluationStatus.STARTED;
        if (hasCrisis == null) hasCrisis = false;
    }

    // Manual Getters/Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Family getFamily() { return family; }
    public void setFamily(Family family) { this.family = family; }
    public EvaluationStatus getStatus() { return status; }
    public void setStatus(EvaluationStatus status) { this.status = status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getFinalizedAt() { return finalizedAt; }
    public void setFinalizedAt(LocalDateTime finalizedAt) { this.finalizedAt = finalizedAt; }
    public Boolean getHasCrisis() { return hasCrisis; }
    public void setHasCrisis(Boolean hasCrisis) { this.hasCrisis = hasCrisis; }
    public Double getIcf() { return icf; }
    public void setIcf(Double icf) { this.icf = icf; }
    public String getMilestoneKey() { return milestoneKey; }
    public void setMilestoneKey(String milestoneKey) { this.milestoneKey = milestoneKey; }
    public List<EvaluationAnswer> getAnswers() { return answers; }
    public List<EvaluationDimensionScore> getDimensionScores() { return dimensionScores; }
}
