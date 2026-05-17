package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * SDD Sprint 4: Entidad de Reflexión Guiada Estructurada.
 */
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "reflections")
@Filter(name = "familyFilter", condition = "family_id = :familyId")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reflection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private PlanTask task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @Column(name = "emotional_impact")
    private Integer emotionalImpact; // escala 1-5

    @Column(name = "communication_improved")
    private Boolean communicationImproved;

    @Column(length = 500)
    private String difficulty;

    @Column(length = 1000)
    private String learning;

    @Column(name = "repeat_intent")
    private Boolean repeatIntent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ReflectionStatus status = ReflectionStatus.DRAFT;

    @Column(name = "submitted_by")
    private String submittedBy;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
