package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * SDD Sprint 7: Desglose de ajustes adaptativos por misión/tarea específica.
 */
@Entity
@Table(name = "mission_adjustments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MissionAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_adjustment_id", nullable = false)
    private PlanAdjustment planAdjustment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private PlanTask task;

    @Column(length = 50)
    private String action; // REDUCE_FREQUENCY, PAUSE, REASSIGN

    @Column(name = "old_frequency", length = 50)
    private String oldFrequency;

    @Column(name = "new_frequency", length = 50)
    private String newFrequency;

    @Column(name = "old_difficulty", length = 50)
    private String oldDifficulty;

    @Column(name = "new_difficulty", length = 50)
    private String newDifficulty;

    @Column(name = "old_due_date", length = 50)
    private String oldDueDate;

    @Column(name = "new_due_date", length = 50)
    private String newDueDate;

    @Column(columnDefinition = "TEXT")
    private String reason;
}
