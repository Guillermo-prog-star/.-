package com.integrityfamily.evaluation.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "evaluation_dimension_scores")
public class EvaluationDimensionScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Evaluation evaluation;

    @Column(name = "dimension_name", nullable = false, length = 100)
    private String dimensionName;

    @Column(name = "score", nullable = false)
    private Double score;

    public EvaluationDimensionScore() {}

    // Manual Getters/Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Evaluation getEvaluation() { return evaluation; }
    public void setEvaluation(Evaluation evaluation) { this.evaluation = evaluation; }
    public String getDimensionName() { return dimensionName; }
    public void setDimensionName(String dimensionName) { this.dimensionName = dimensionName; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
}
