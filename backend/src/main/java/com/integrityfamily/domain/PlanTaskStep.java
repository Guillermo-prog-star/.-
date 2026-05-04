package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * SDD SPEC 6.2: Paso individual de una tarea de plan.
 */
@Entity
@Table(name = "plan_task_steps")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanTaskStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private PlanTask task;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StepType type; // PLANIFICAR, EJECUTAR, EVALUAR

    @Column(nullable = false, columnDefinition = "TEXT")
    private String detail;

    @Builder.Default
    private boolean completed = false;

    // SDD SPEC 6.7: Conexión con Bitácora Cognitiva
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logbook_entry_id")
    private FamilyLogbookEntry logbookEntry;
}
