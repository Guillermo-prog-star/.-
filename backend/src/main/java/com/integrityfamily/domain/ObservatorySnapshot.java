package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Agregado mensual anonimizado del Observatorio del Desarrollo Familiar.
 *
 * Un registro por mes. Contiene estadísticas poblacionales derivadas de
 * family_capital_snapshots sin ningún identificador familiar — son datos
 * puramente agregados, aptos para publicación y análisis de política pública.
 *
 * Generado automáticamente el 1° de cada mes por ObservatoryScheduler.
 */
@Entity
@Table(name = "observatory_snapshots",
       uniqueConstraints = @UniqueConstraint(columnNames = "snapshot_month"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ObservatorySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Primer día del mes que representa este snapshot (ej. 2026-07-01) */
    @Column(name = "snapshot_month", nullable = false)
    private LocalDate snapshotMonth;

    // ── Distribución ICaF ─────────────────────────────────────────────────────

    @Column(name = "icaf_avg")
    private Double icafAvg;

    @Column(name = "icaf_p25")
    private Double icafP25;

    @Column(name = "icaf_median")
    private Double icafMedian;

    @Column(name = "icaf_p75")
    private Double icafP75;

    @Column(name = "families_count")
    private Integer familiesCount;

    // ── Distribución por nivel de madurez (% sobre families_count) ───────────

    @Column(name = "nivel_1_pct") private Double nivel1Pct;
    @Column(name = "nivel_2_pct") private Double nivel2Pct;
    @Column(name = "nivel_3_pct") private Double nivel3Pct;
    @Column(name = "nivel_4_pct") private Double nivel4Pct;
    @Column(name = "nivel_5_pct") private Double nivel5Pct;

    // ── Eventos críticos del mes ──────────────────────────────────────────────

    @Column(name = "events_detected")      private Integer eventsDetected;
    @Column(name = "events_resolved")      private Integer eventsResolved;
    @Column(name = "avg_days_resolution")  private Double  avgDaysResolution;
    @Column(name = "resolution_rate_pct")  private Double  resolutionRatePct;

    // ── Tendencia de capital ──────────────────────────────────────────────────

    @Column(name = "families_improving") private Integer familiesImproving;
    @Column(name = "families_declining") private Integer familiesDeclining;
    @Column(name = "families_stable")    private Integer familiesStable;

    // ── Promedios por dominio ─────────────────────────────────────────────────

    @Column(name = "avg_dom_cohesion")       private Double avgDomCohesion;
    @Column(name = "avg_dom_confianza")      private Double avgDomConfianza;
    @Column(name = "avg_dom_resiliencia")    private Double avgDomResiliencia;
    @Column(name = "avg_dom_comunicacion")   private Double avgDomComunicacion;
    @Column(name = "avg_dom_autonomia")      private Double avgDomAutonomia;
    @Column(name = "avg_dom_bienestar")      private Double avgDomBienestar;
    @Column(name = "avg_dom_proposito")      private Double avgDomProposito;
    @Column(name = "avg_dom_integracion")    private Double avgDomIntegracion;
    @Column(name = "avg_dom_emprendimiento") private Double avgDomEmprendimiento;
    @Column(name = "avg_dom_legado")         private Double avgDomLegado;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    // ── Métodos de dominio ────────────────────────────────────────────────────

    /** Nivel de madurez dominante (el que tiene mayor porcentaje) */
    public int dominantMadurezNivel() {
        double max = -1;
        int nivel = 1;
        double[] pcts = { orZero(nivel1Pct), orZero(nivel2Pct), orZero(nivel3Pct),
                          orZero(nivel4Pct), orZero(nivel5Pct) };
        for (int i = 0; i < pcts.length; i++) {
            if (pcts[i] > max) { max = pcts[i]; nivel = i + 1; }
        }
        return nivel;
    }

    /** % de familias que mejoran (IMPROVING) */
    public double improvingPct() {
        if (familiesCount == null || familiesCount == 0) return 0.0;
        return orZero(familiesImproving != null ? (double) familiesImproving / familiesCount * 100 : null);
    }

    private double orZero(Double v) { return v != null ? v : 0.0; }
}
