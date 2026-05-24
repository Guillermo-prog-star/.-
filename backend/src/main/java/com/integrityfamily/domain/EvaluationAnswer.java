package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "evaluation_answers",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_ans_eval_question",
        columnNames = {"evaluation_id", "question_key"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private Evaluation evaluation;

    @Column(nullable = false)
    private String questionKey;

    /** PK numérica de la pregunta — permite reconstruir AnswerDto al finalizar */
    @Column(name = "question_id")
    private Long questionId;

    @Column(nullable = false)
    private Integer score;

    /**
     * Respuesta Sí/No para preguntas dicotómicas.
     * true=Sí (mapeado a score=5), false=No (score=1).
     * null si la pregunta usa escala 1-5.
     */
    @Column(name = "boolean_answer")
    private Boolean booleanAnswer;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private DimensionType dimension;

    @Column(name = "diagnostic_dimension", length = 50)
    private String diagnosticDimension;

    @Column(name = "consciousness_level", length = 30)
    private String consciousnessLevel;

    /** Timestamp del momento en que se guardó esta respuesta (flujo incremental) */
    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @PrePersist
    public void prePersist() {
        if (answeredAt == null) answeredAt = LocalDateTime.now();
    }
}

