package com.integrityfamily.checklist.domain;

import com.integrityfamily.family.domain.Family;
import com.integrityfamily.plan.domain.PlanTask;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "checklist_items")
public class ChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "completed")
    private Boolean completed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id")
    private Family family;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_task_id")
    private PlanTask planTask;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public ChecklistItem() {}

    @PrePersist
    public void prePersist() {
        if (completed == null) completed = false;
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    // Manual Getters/Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }
    public Family getFamily() { return family; }
    public void setFamily(Family family) { this.family = family; }
    public PlanTask getPlanTask() { return planTask; }
    public void setPlanTask(PlanTask planTask) { this.planTask = planTask; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}