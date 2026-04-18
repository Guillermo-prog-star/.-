package com.integrityfamily.assessment.domain;

import com.integrityfamily.family.domain.Family;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "assessments")
public class Assessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @Column(name = "assessment_date")
    private LocalDateTime assessmentDate;

    @Column(name = "spiritual_synthesis", columnDefinition = "TEXT")
    private String spiritualSynthesis;

    @OneToMany(mappedBy = "assessment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AssessmentDetail> details = new ArrayList<>();

    public Assessment() {}

    @PrePersist
    public void prePersist() {
        if (assessmentDate == null) assessmentDate = LocalDateTime.now();
    }

    // Manual Getters/Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Family getFamily() { return family; }
    public void setFamily(Family family) { this.family = family; }
    public LocalDateTime getAssessmentDate() { return assessmentDate; }
    public void setAssessmentDate(LocalDateTime date) { this.assessmentDate = date; }
    public String getSpiritualSynthesis() { return spiritualSynthesis; }
    public void setSpiritualSynthesis(String synthesis) { this.spiritualSynthesis = synthesis; }
    public List<AssessmentDetail> getDetails() { return details; }
}