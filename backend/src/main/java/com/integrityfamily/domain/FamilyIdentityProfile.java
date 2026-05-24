package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * SDD Fase 1 — Perfil de Identidad Cognitiva Familiar.
 * Define la "personalidad operativa" de la familia: cómo se comunica, qué la estresa,
 * cuáles son sus rituales, su ritmo de transformación y su narrativa de identidad.
 * Este perfil evoluciona con cada ciclo de diagnóstico y reflexión.
 */
@Entity
@Table(name = "family_identity_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyIdentityProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false, unique = true)
    private Family family;

    /**
     * Estilo dominante de comunicación familiar.
     * RESERVED | EXPRESSIVE | CONFLICTIVE | AVOIDANT | COLLABORATIVE
     */
    @Column(name = "communication_style", length = 30)
    @Builder.Default
    private String communicationStyle = "UNKNOWN";

    /**
     * Cómo gestiona el conflicto la familia.
     * CONFRONTATIONAL | AVOIDANT | NEGOTIATING | EXPLOSIVE | PASSIVE
     */
    @Column(name = "conflict_style", length = 30)
    @Builder.Default
    private String conflictStyle = "UNKNOWN";

    /**
     * Rituales identificados que refuerzan la cohesión familiar.
     * JSON array: ["cena_semanal", "salida_dominical"]
     */
    @Column(name = "ritual_patterns", columnDefinition = "TEXT")
    private String ritualPatterns;

    /**
     * Factores que incrementan el estrés familiar.
     * JSON array: ["presion_economica", "horarios_laborales"]
     */
    @Column(name = "stress_triggers", columnDefinition = "TEXT")
    private String stressTriggers;

    /**
     * Nivel de expresión emocional observable.
     * LOW | MEDIUM | HIGH
     */
    @Column(name = "emotional_expression", length = 20)
    @Builder.Default
    private String emotionalExpression = "MEDIUM";

    /**
     * Velocidad de adaptación al cambio (0.0 lenta - 1.0 alta).
     * Se calcula comparando planes anteriores con adherencia histórica.
     */
    @Column(name = "adaptability_index")
    @Builder.Default
    private Double adaptabilityIndex = 0.5;

    /**
     * Narrativa identitaria de la familia en texto libre.
     * Generada y actualizada por el motor de reflexión IA.
     * Ejemplo: "Familia con liderazgo paterno fuerte, alta resistencia al cambio emocional
     *           pero buena adherencia a rutinas cuando las tareas son cortas."
     */
    @Column(name = "identity_narrative", columnDefinition = "TEXT")
    private String identityNarrative;

    /**
     * Etapa evolutiva actual de la familia en el sistema.
     * INITIAL | RECOGNITION | ADJUSTMENT | CONSOLIDATION | AUTONOMOUS
     */
    @Column(name = "evolution_stage", length = 20)
    @Builder.Default
    private String evolutionStage = "INITIAL";

    /** Número de ciclos completos de diagnóstico-plan-evidencia-reflexión */
    @Column(name = "completed_cycles")
    @Builder.Default
    private Integer completedCycles = 0;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Avanza el contador de ciclos y ajusta la etapa evolutiva */
    public void registerCompletedCycle() {
        this.completedCycles++;
        this.updatedAt = LocalDateTime.now();
        if (completedCycles >= 12) evolutionStage = "AUTONOMOUS";
        else if (completedCycles >= 6) evolutionStage = "CONSOLIDATION";
        else if (completedCycles >= 3) evolutionStage = "ADJUSTMENT";
        else if (completedCycles >= 1) evolutionStage = "RECOGNITION";
    }
}
