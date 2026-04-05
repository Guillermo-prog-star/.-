package com.integrityfamily.assessment.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "assessment_details")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssessmentDetail {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id")
    private Assessment assessment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    private String questionKey; // Referencia rápida a la pregunta
    private String category;
    private Integer selectedOption;
    private Integer score;
}