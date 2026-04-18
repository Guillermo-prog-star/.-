package com.integrityfamily.evaluation.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "evaluation_answers")
public class EvaluationAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Evaluation evaluation;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "answer_value", nullable = false)
    private Integer answerValue;

    public EvaluationAnswer() {}

    // Manual Getters/Setters 
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Evaluation getEvaluation() { return evaluation; }
    public void setEvaluation(Evaluation evaluation) { this.evaluation = evaluation; }
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public Integer getAnswerValue() { return answerValue; }
    public void setAnswerValue(Integer answerValue) { this.answerValue = answerValue; }
}