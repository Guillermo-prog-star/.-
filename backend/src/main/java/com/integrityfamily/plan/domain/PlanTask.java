package com.integrityfamily.plan.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "plan_tasks")
public class PlanTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Plan plan;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "completed")
    private Boolean completed;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "assigned_member_id")
    private Long assignedMemberId;

    public PlanTask() {}

    @PrePersist
    public void prePersist() {
        if (completed == null) completed = false;
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    // Manual Getters/Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Plan getPlan() { return plan; }
    public void setPlan(Plan plan) { this.plan = plan; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
}