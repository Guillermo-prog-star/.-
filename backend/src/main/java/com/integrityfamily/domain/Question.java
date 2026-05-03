package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * SDD SPEC: Entidad de Pregunta (Question) centralizada.
 */
@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_key", unique = true)
    private String questionKey; // REC_EMO_001, etc.

    @Column(nullable = false, length = 500)
    private String text;

    @Column(length = 50)
    private String dimension;

    @Column(length = 50)
    private String area; // EMOCIONES, COMUNICACION, etc.

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private int weight = 1;

    private Integer sortOrder;
}
