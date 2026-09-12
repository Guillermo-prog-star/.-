package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Observación primaria que respalda una hipótesis del sistema (ADR-004).
 *
 * Append-only, independiente del estado operacional: nunca se actualiza ni
 * se sobrescribe una fila existente, solo se insertan nuevas. Una fila
 * válida registra una medición (ej. dimScore=92, dimensión=COMUNICACION),
 * nunca una conclusión (ej. "la familia mejoró") — la interpretación ocurre
 * en el análisis posterior, no al escribir la fila.
 *
 * No tiene FK a una entidad específica: subject_type/subject_id es
 * deliberadamente genérico y polimórfico (familia, miembro, dimensión,
 * misión, etc. — ver ADR-004, Decisión 2).
 */
@Entity
@Table(name = "hypothesis_evidence")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HypothesisEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Qué hipótesis respalda esta observación (ej. "PAF"). */
    @Column(name = "hypothesis", nullable = false)
    private String hypothesis;

    /** Versión de la definición de la hipótesis — no del software. */
    @Column(name = "hypothesis_version", nullable = false)
    private String hypothesisVersion;

    /** Tipo de entidad observada (ej. "FAMILY"). Catálogo abierto. */
    @Column(name = "subject_type", nullable = false)
    private String subjectType;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    /** Qué se midió (ej. "DIM_COMUNICACION"). */
    @Column(name = "measurement_type", nullable = false)
    private String measurementType;

    @Column(name = "measurement_value", nullable = false)
    private Double measurementValue;

    /** Con qué instrumento se midió (ej. "ICF"). */
    @Column(name = "instrument", nullable = false)
    private String instrument;

    @Column(name = "instrument_version", nullable = false)
    private String instrumentVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private EvidenceSource source;

    /** Instante efectivo de la observación (no de persistencia). */
    @Column(name = "observed_at", nullable = false)
    private LocalDateTime observedAt;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    protected void onCreate() {
        if (recordedAt == null) {
            recordedAt = LocalDateTime.now();
        }
    }
}
