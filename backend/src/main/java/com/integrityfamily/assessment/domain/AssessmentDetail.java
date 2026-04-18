package com.integrityfamily.assessment.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "assessment_details")
public class AssessmentDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Assessment assessment;

    @Column(name = "dimension_name", nullable = false)
    private String dimensionName;

    @Column(name = "score", nullable = false)
    private Double score;

    public AssessmentDetail() {}

    // Manual Getters/Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Assessment getAssessment() { return assessment; }
    public void setAssessment(Assessment a) { this.assessment = a; }
    public String getDimensionName() { return dimensionName; }
    public void setDimensionName(String name) { this.dimensionName = name; }
    public Double getScore() { return score; }
    public void setScore(Double s) { this.score = s; }
}