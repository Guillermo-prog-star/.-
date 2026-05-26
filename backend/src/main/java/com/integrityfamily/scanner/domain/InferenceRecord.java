package com.integrityfamily.scanner.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * IF-CIS: Registro de inferencia epistemológicamente estable.
 *
 * Captura el resultado del RISK_ALGO_V1 de forma inmutable y versionada.
 * Garantiza: misma evidencia (evidenceHash) → misma inferencia.
 *
 * Estados epistemológicos: OBSERVED → CORRELATED → INFERRED → STABILIZED → REVISED → ARCHIVED
 */
@Entity
@Table(name = "inference_records", indexes = {
    @Index(name = "idx_ir_family",     columnList = "family_id"),
    @Index(name = "idx_ir_evaluation", columnList = "evaluation_id"),
    @Index(name = "idx_ir_hash",       columnList = "evidence_hash", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InferenceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Familia a la que pertenece esta inferencia. */
    @Column(name = "family_id", nullable = false)
    private Long familyId;

    /** Evaluación fuente de esta inferencia. */
    @Column(name = "evaluation_id", nullable = false)
    private Long evaluationId;

    /**
     * Clave semántica del tipo de inferencia.
     * Valores: "ICF_CALC", "RELAPSE_DETECTION", "STATE_TRANSITION"
     */
    @Column(name = "inference_key", length = 50)
    private String inferenceKey;

    /** Versión del algoritmo que produjo esta inferencia. 1 = RISK_ALGO_V1 */
    @Column(name = "algo_version", nullable = false)
    @Builder.Default
    private int algoVersion = 1;

    /**
     * Estado epistemológico actual.
     * Valores: OBSERVED / CORRELATED / INFERRED / STABILIZED / REVISED / ARCHIVED
     */
    @Column(name = "epistemic_state", length = 20, nullable = false)
    @Builder.Default
    private String epistemicState = "INFERRED";

    /**
     * SHA-256 (primeros 32 chars) de los inputs del algoritmo.
     * Garantiza que la misma evidencia no genere registros duplicados.
     */
    @Column(name = "evidence_hash", length = 64, nullable = false)
    private String evidenceHash;

    // ── Resultado de la inferencia ───────────────────────────────────────────

    @Column(name = "icf_value")
    private Double icfValue;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(name = "critical_dimension", length = 50)
    private String criticalDimension;

    /** Estado operacional IF-TOS calculado sobre el historial familiar. */
    @Column(name = "operational_state", length = 20)
    private String operationalState;

    @Column(name = "simulation_suspected")
    private Boolean simulationSuspected;

    // ── Incertidumbre estructural IF-SUM ─────────────────────────────────────

    /** Incertidumbre total (0.0 – 1.0). > 0.40 = alta; > 0.50 = reduce proyección de riesgo. */
    @Column(name = "uncertainty_total")
    private Double uncertaintyTotal;

    // ── Versionado epistemológico ─────────────────────────────────────────────

    /** ID del InferenceRecord anterior (para trazabilidad de revisiones). */
    @Column(name = "previous_version")
    private Long previousVersion;

    /** Razón del cambio si epistemicState = REVISED. */
    @Column(name = "revision_reason", length = 200)
    private String revisionReason;

    @Column(name = "stabilized_at")
    private Instant stabilizedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
