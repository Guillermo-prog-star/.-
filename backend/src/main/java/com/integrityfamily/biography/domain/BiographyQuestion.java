package com.integrityfamily.biography.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Catálogo fijo y global (igual para todas las familias) del diario guiado
 * biográfico: 12 capítulos de vida, cada uno con preguntas estructuradas y
 * un "para profundizar" opcional. Sembrado de forma idempotente por
 * {@link com.integrityfamily.biography.initializer.BiographyQuestionDataInitializer},
 * no por SQL crudo en la migración (ver ADR-017: V75 sembró un banco similar
 * directo por SQL y eso produjo el defecto corregido en V100-V101).
 */
@Entity
@Table(name = "biography_questions")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class BiographyQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chapter_number", nullable = false)
    private Integer chapterNumber;

    @Column(name = "chapter_title", nullable = false, length = 150)
    private String chapterTitle;

    @Column(name = "question_key", nullable = false, unique = true, length = 80)
    private String questionKey;

    @Column(name = "prompt_text", columnDefinition = "TEXT", nullable = false)
    private String promptText;

    @Column(name = "deepen_text", columnDefinition = "TEXT")
    private String deepenText;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
