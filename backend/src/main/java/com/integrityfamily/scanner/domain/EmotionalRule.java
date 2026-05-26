package com.integrityfamily.scanner.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.List;

/**
 * IF-EEDSL: Regla emocional ejecutable y serializable.
 *
 * Representa una regla del DSL emocional de Integrity Family:
 *
 *   RULE {ruleKey}
 *   WHEN  {requiredSignals}
 *   FOR   {temporalWindowDays} días
 *   IN    {memberRole} dentro de {milestoneScope}
 *   THEN  projection={projectionLabel}, confidence={confidenceBase}, risk={riskOutput}
 *
 * Propiedades garantizadas: deterministic, executable, replayable, auditable,
 *                           serializable, versionable.
 */
@Entity
@Table(name = "emotional_rules", indexes = {
    @Index(name = "idx_er_key_version", columnList = "rule_key, version", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmotionalRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identificador semántico único de la regla.
     * Ejemplos: "relational_stress", "emotional_exhaustion", "vincular_disconnection"
     */
    @Column(name = "rule_key", length = 60, nullable = false)
    private String ruleKey;

    /** Versión de la regla. Versiones anteriores se mantienen para replay histórico. */
    @Column(nullable = false)
    @Builder.Default
    private int version = 1;

    /** Si false, la regla está deprecada y no se aplica a nuevas evaluaciones. */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Rango de hitos donde aplica esta regla.
     * Ejemplos: "W1-M6", "M9-M36", "*" (todos los hitos)
     */
    @Column(name = "milestone_scope", length = 20)
    @Builder.Default
    private String milestoneScope = "*";

    /**
     * Rol del miembro al que aplica.
     * Valores: "PADRE", "MADRE", "ADOLESCENTE", "NINO", "*"
     */
    @Column(name = "member_role", length = 20)
    @Builder.Default
    private String memberRole = "*";

    /**
     * Señales que deben estar presentes para activar esta regla.
     * Almacenadas como lista de strings: ["voice_tension", "interruptions", "avoidance"]
     */
    @ElementCollection
    @CollectionTable(name = "emotional_rule_signals", joinColumns = @JoinColumn(name = "rule_id"))
    @Column(name = "signal_name", length = 60)
    private List<String> requiredSignals;

    /** Ventana temporal en días durante la cual deben persistir las señales. */
    @Column(name = "temporal_window_days")
    @Builder.Default
    private int temporalWindowDays = 14;

    /**
     * Etiqueta de la proyección emocional resultante si se cumple la regla.
     * Ejemplos: "ESTRES_RELACIONAL", "AGOTAMIENTO_EMOCIONAL", "DESCONEXION_VINCULAR"
     */
    @Column(name = "projection_label", length = 60)
    private String projectionLabel;

    /** Confianza base de la inferencia cuando se activa esta regla (0.0 – 1.0). */
    @Column(name = "confidence_base")
    @Builder.Default
    private double confidenceBase = 0.70;

    /**
     * Nivel de riesgo inferido cuando se activa esta regla.
     * Valores: "BAJO", "MODERADO", "ALTO", "CRITICO"
     */
    @Column(name = "risk_output", length = 20)
    private String riskOutput;

    /** Quién creó esta regla: "RISK_ALGO_V1", "CLINICIAN_OVERRIDE", "ADMIN". */
    @Column(name = "created_by", length = 50)
    @Builder.Default
    private String createdBy = "RISK_ALGO_V1";

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
