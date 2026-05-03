package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "evaluations")
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
    private Family family;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private FamilyMember member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EvaluationStatus status;

    private LocalDateTime startedAt;
    private LocalDateTime finalizedAt;

    @Builder.Default
    private Boolean hasCrisis = false;

    private Double icf;

    @Column(length = 50)
    private String milestoneKey;

    @Column(columnDefinition = "TEXT")
    private String spiritualSynthesis;

    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EvaluationAnswer> answers = new ArrayList<>();

    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EvaluationDimensionScore> dimensionScores = new ArrayList<>();

    @PrePersist
    public void pre() {
        if (startedAt == null)
            startedAt = LocalDateTime.now();
        if (status == null)
            status = EvaluationStatus.STARTED;
    }
}
