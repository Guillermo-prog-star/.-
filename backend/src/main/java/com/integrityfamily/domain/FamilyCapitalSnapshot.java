package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Snapshot del Índice de Capital Familiar (ICaF).
 *
 * Un registro por cada recálculo del ICaF. Permite reconstruir
 * la trayectoria completa de Capital Familiar por familia y
 * alimentar el Observatorio del Desarrollo Familiar.
 */
@Entity
@Table(name = "family_capital_snapshots")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FamilyCapitalSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    // ── Índice global ─────────────────────────────────────────────────────────

    @Column(name = "icaf", nullable = false)
    private Double icaf;

    @Column(name = "madurez_nivel", nullable = false)
    private Integer madurezNivel;

    // ── 11 dominios ───────────────────────────────────────────────────────────

    @Column(name = "dom_cohesion")
    private Double domCohesion;

    @Column(name = "dom_confianza")
    private Double domConfianza;

    @Column(name = "dom_resiliencia")
    private Double domResiliencia;

    @Column(name = "dom_comunicacion")
    private Double domComunicacion;

    @Column(name = "dom_autonomia")
    private Double domAutonomia;

    @Column(name = "dom_bienestar")
    private Double domBienestar;

    @Column(name = "dom_proposito")
    private Double domProposito;

    @Column(name = "dom_integracion")
    private Double domIntegracion;

    @Column(name = "dom_emprendimiento")
    private Double domEmprendimiento;

    @Column(name = "dom_legado")
    private Double domLegado;

    @Column(name = "dom_madurez")
    private Double domMadurez;

    // ── Metadatos ─────────────────────────────────────────────────────────────

    @Column(name = "trigger_type", nullable = false, length = 50)
    private String triggerType;

    @Column(name = "algorithm_version", nullable = false, length = 30)
    @Builder.Default
    private String algorithmVersion = "ICAF_V1";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
