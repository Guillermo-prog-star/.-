package com.integrityfamily.family.domain;

import com.integrityfamily.auth.domain.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "families")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Family {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "municipio", length = 120)
    private String municipio;

    @Column(name = "whatsapp", length = 30)
    private String whatsapp;

    @Column(name = "pin_hash", length = 255)
    private String pin;

    @Column(name = "family_code", length = 50, unique = true)
    private String familyCode;

    @Column(name = "current_milestone", length = 50)
    private String currentMilestone;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "next_evaluation_at")
    private LocalDateTime nextEvaluationAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @OneToMany(mappedBy = "family", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Member> members = new ArrayList<>();

    public Family() {}

    @PrePersist
    public void prePersist() {
        if (this.familyCode == null || this.familyCode.isBlank()) {
            this.familyCode = "FAM-" + System.currentTimeMillis();
        }
        if (this.currentMilestone == null || this.currentMilestone.isBlank()) {
            this.currentMilestone = "DIAGNOSTICO_INICIAL";
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // Manual Getters/Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getWhatsapp() { return whatsapp; }
    public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }
    public String getFamilyCode() { return familyCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getNextEvaluationAt() { return nextEvaluationAt; }
    public void setNextEvaluationAt(LocalDateTime date) { this.nextEvaluationAt = date; }
    public String getCurrentMilestone() { return currentMilestone; }
    public void setCurrentMilestone(String milestone) { this.currentMilestone = milestone; }
    public List<Member> getMembers() { return members; }
}