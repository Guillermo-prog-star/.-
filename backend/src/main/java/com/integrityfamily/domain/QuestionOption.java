package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "question_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false, length = 500)
    private String text;

    @Column(name = "score_value")
    @Builder.Default
    private Integer scoreValue = 0;

    @Column(name = "rubric_level")
    private Integer rubricLevel;

    @Column(name = "awareness_component", length = 50)
    private String awarenessComponent;

    @Column(name = "vector_tag", length = 50)
    private String vectorTag;

    @Column(name = "inferred_level", length = 50)
    private String inferredLevel;

    @Column(name = "psychometric_weight")
    private Double psychometricWeight;
}
