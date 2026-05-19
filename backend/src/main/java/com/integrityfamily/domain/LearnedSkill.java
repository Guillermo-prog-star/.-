package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * SDD Fase 1 — Motor de Habilidades Cognitivas.
 * Habilidad operacional aprendida por el sistema a partir de intervenciones exitosas.
 * No es una regla fija: es una heurística validada empíricamente por la IA.
 */
@Entity
@Table(name = "learned_skills", indexes = {
    @Index(name = "idx_learned_skills_name", columnList = "skill_name"),
    @Index(name = "idx_learned_skills_success", columnList = "success_rate DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearnedSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador semántico de la habilidad (ej: "reduce_resistance_high_stress") */
    @Column(name = "skill_name", nullable = false, unique = true, length = 100)
    private String skillName;

    /** Descripción legible para el equipo y para la IA */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * Condiciones bajo las cuales esta skill se activa.
     * JSON array: ["low_adherence", "high_fatigue", "communication_score < 40"]
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String conditions;

    /**
     * Estrategia recomendada cuando se activa.
     * JSON array: ["micro-missions", "short-reflections", "positive-reinforcement"]
     */
    @Column(name = "recommended_strategy", nullable = false, columnDefinition = "TEXT")
    private String recommendedStrategy;

    /** Dimensión familiar principal que impacta */
    @Column(length = 50)
    private String dimension; // COMUNICACION, EMOCIONES, HABITOS, TIEMPOS

    /** Tasa de éxito validada (0.0 - 1.0) */
    @Column(name = "success_rate")
    @Builder.Default
    private Double successRate = 0.0;

    /** Cuántas veces se ha aplicado esta skill */
    @Column(name = "reuse_count")
    @Builder.Default
    private Integer reuseCount = 0;

    /** Cuántas veces fue aplicada y resultó exitosa */
    @Column(name = "success_count")
    @Builder.Default
    private Integer successCount = 0;

    /** Si fue generada por el motor de reflexión IA o definida manualmente */
    @Column(name = "created_by_ai")
    @Builder.Default
    private Boolean createdByAi = true;

    /** Confianza actual del sistema en esta habilidad (se actualiza con cada aplicación) */
    @Column(name = "confidence")
    @Builder.Default
    private Double confidence = 0.5;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_applied_at")
    private LocalDateTime lastAppliedAt;

    /** Registra una aplicación exitosa y recalcula métricas */
    public void recordSuccess() {
        this.reuseCount++;
        this.successCount++;
        this.successRate = (double) successCount / reuseCount;
        this.confidence = Math.min(1.0, confidence + 0.05);
        this.lastAppliedAt = LocalDateTime.now();
    }

    /** Registra una aplicación sin éxito */
    public void recordFailure() {
        this.reuseCount++;
        this.successRate = (double) successCount / reuseCount;
        this.confidence = Math.max(0.0, confidence - 0.03);
        this.lastAppliedAt = LocalDateTime.now();
    }
}
