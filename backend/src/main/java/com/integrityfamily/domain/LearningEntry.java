package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * SDD Sprint 4: Entidad de Aprendizaje Familiar Longitudinal.
 * Certifica cambios reales en el comportamiento y nivel de consciencia familiar.
 */
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "learning_entries")
@Filter(name = "familyFilter", condition = "family_id = :familyId")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private PlanTask task;

    @Column(name = "behavioral_change", nullable = false, columnDefinition = "TEXT")
    private String behavioralChange;

    @Column(name = "awareness_shift", nullable = false, columnDefinition = "TEXT")
    private String awarenessShift;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
