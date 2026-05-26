package com.integrityfamily.scanner.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * IF-ALT: Alerta clínica generada por el motor de detección de patrones.
 *
 * Una alerta representa un patrón que requiere atención clínica:
 * no es un diagnóstico (eso es InferenceRecord), sino una señal de aviso
 * sobre la evolución del estado familiar.
 */
@Entity
@Table(name = "family_alerts", indexes = {
    @Index(name = "idx_fa_family",   columnList = "family_id"),
    @Index(name = "idx_fa_resolved", columnList = "family_id, resolved")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    /**
     * Tipo de alerta.
     * CONSECUTIVE_HIGH_RISK     → 2+ evaluaciones consecutivas ALTO o CRITICO
     * CRITICAL_STATE_SUSTAINED  → Estado IF-TOS CRITICAL por 3+ inferencias
     * SIMULATION_REPEAT         → simulationSuspected en 2+ evaluaciones recientes
     * RELAPSE_CONFIRMED         → relapseDetected en evaluación activa
     * MULTI_RULE_ACTIVATION     → 3+ reglas EEDSL activadas en la misma evaluación
     */
    @Column(name = "alert_type", length = 50, nullable = false)
    private String alertType;

    /** LOW | MEDIUM | HIGH | CRITICAL */
    @Column(name = "severity", length = 20, nullable = false)
    private String severity;

    @Column(name = "title", length = 100, nullable = false)
    private String title;

    @Column(name = "detail", length = 500)
    private String detail;

    /** inferenceKey o ruleKey que originó la alerta. */
    @Column(name = "inference_key", length = 60)
    private String inferenceKey;

    @Column(name = "evaluation_id")
    private Long evaluationId;

    @Column(name = "resolved", nullable = false)
    @Builder.Default
    private boolean resolved = false;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
