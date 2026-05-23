package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sprint_retrospectives")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class SprintRetrospective {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sprint_id", nullable = false)
    private FamilySprint sprint;

    @Column(name = "what_went_well", columnDefinition = "TEXT")
    private String whatWentWell;

    @Column(name = "what_was_difficult", columnDefinition = "TEXT")
    private String whatWasDifficult;

    @Column(name = "what_learned", columnDefinition = "TEXT")
    private String whatLearned;

    @Column(name = "what_to_adjust", columnDefinition = "TEXT")
    private String whatToAdjust;

    @Column(name = "tension_level")
    private Integer tensionLevel; // 1-10

    @Column(name = "mindful_compliance")
    private Integer mindfulCompliance; // 1-10

    @Column(name = "shared_time")
    private Integer sharedTime; // 1-10

    @Column(name = "positive_interactions")
    private Integer positiveInteractions; // 1-10

    @Column(name = "emotional_persistence")
    private Integer emotionalPersistence; // 1-10

    @Column(name = "consistency_score")
    private Integer consistencyScore; // 1-10 (Consistencia Evolutiva)

    @Column(name = "ai_feedback", columnDefinition = "TEXT")
    private String aiFeedback;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
