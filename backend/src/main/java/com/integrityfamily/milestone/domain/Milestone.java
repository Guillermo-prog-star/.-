package com.integrityfamily.milestone.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "milestones")
public class Milestone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "milestone_key", unique = true, nullable = false, length = 50)
    private String milestoneKey;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "bloque", length = 50)
    private String bloque;

    @Column(name = "phase", length = 50)
    private String phase;

    @Column(name = "months")
    private Integer months;

    @Column(name = "sort_order")
    private Integer sortOrder;

    public Milestone() {}

    // Manual Getters/Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMilestoneKey() { return milestoneKey; }
    public void setMilestoneKey(String key) { this.milestoneKey = key; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String desc) { this.description = desc; }
    public String getBloque() { return bloque; }
    public void setBloque(String bloque) { this.bloque = bloque; }
    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }
    public Integer getMonths() { return months; }
    public void setMonths(Integer months) { this.months = months; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer order) { this.sortOrder = order; }
}
