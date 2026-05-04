package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SDD SPEC 6.2: Tarea de Plan Harmonizada.
 */
@Entity
@Table(name = "plan_tasks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private ImprovementPlan plan;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String dimension;

    private LocalDateTime dueDate;
    
    private int periodicityMonths;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_id")
    private Milestone milestone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_id")
    private FamilyMember responsible;

    @Builder.Default
    private boolean completed = false;

    @Builder.Default
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlanTaskStep> steps = new ArrayList<>();

    // SDD-FIX: Métodos explícitos para el build de Docker
    public String getDescription() { return this.description; }
    public void setDescription(String description) { this.description = description; }
    
    public boolean isCompleted() { return this.completed; }
    public boolean getCompleted() { return this.completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public LocalDateTime getDueDate() { return this.dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
}
