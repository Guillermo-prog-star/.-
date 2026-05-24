package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * SDD Fase 5 — Arista del grafo de identidad familiar.
 *
 * Modela la dinámica entre dos miembros de la familia.
 * El grafo es no-dirigido: siempre memberA.id < memberB.id para evitar duplicados.
 * Las puntuaciones (cohesión, tensión, comunicación) evolucionan con cada evaluación.
 */
@Entity
@Table(name = "member_relation_edges",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_edge_pair",
                columnNames = {"family_id", "member_a_id", "member_b_id"}),
        indexes = {
            @Index(name = "idx_edge_family", columnList = "family_id"),
            @Index(name = "idx_edge_member_a", columnList = "member_a_id"),
            @Index(name = "idx_edge_member_b", columnList = "member_b_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberRelationEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    /** Miembro con ID menor (garantiza unicidad del par sin orden) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_a_id", nullable = false)
    private FamilyMember memberA;

    /** Miembro con ID mayor */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_b_id", nullable = false)
    private FamilyMember memberB;

    /**
     * Tipo de vínculo entre los dos miembros.
     * SPOUSE | PARENT_CHILD | SIBLING | GUARDIAN_CHILD | OTHER
     */
    @Column(name = "relationship_type", length = 20)
    @Builder.Default
    private String relationshipType = "OTHER";

    /**
     * Calidad dinámica del vínculo observada en evaluaciones recientes.
     * SUPPORTIVE | BALANCED | DISTANT | CONFLICTIVE
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "dynamic_type", length = 20)
    @Builder.Default
    private DynamicType dynamicType = DynamicType.BALANCED;

    /** Puntuación de cohesión del par (0–100) */
    @Column(name = "cohesion_score")
    @Builder.Default
    private Double cohesionScore = 50.0;

    /** Puntuación de tensión del par (0–100) */
    @Column(name = "tension_score")
    @Builder.Default
    private Double tensionScore = 30.0;

    /** Calidad de comunicación entre los dos miembros (0–100) */
    @Column(name = "communication_score")
    @Builder.Default
    private Double communicationScore = 50.0;

    /** Tendencia observable: IMPROVING | STABLE | DECLINING */
    @Column(name = "evolution_trend", length = 15)
    @Builder.Default
    private String evolutionTrend = "STABLE";

    /** Rol sistémico detectado para memberA en esta díada */
    @Column(name = "role_a", length = 20)
    @Builder.Default
    private String roleA = "NEUTRAL";

    /** Rol sistémico detectado para memberB en esta díada */
    @Column(name = "role_b", length = 20)
    @Builder.Default
    private String roleB = "NEUTRAL";

    /** ID de la evaluación que generó la última actualización */
    @Column(name = "from_evaluation_id")
    private Long fromEvaluationId;

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum DynamicType { SUPPORTIVE, BALANCED, DISTANT, CONFLICTIVE }

    /** Calcula la puntuación de salud relacional combinada (0–100) */
    public double healthScore() {
        return Math.max(0, Math.min(100,
                (cohesionScore * 0.4) + (communicationScore * 0.4) + ((100 - tensionScore) * 0.2)));
    }
}
