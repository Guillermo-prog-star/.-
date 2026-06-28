package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Ciclo de vida completo de un evento crítico familiar.
 *
 * Permite medir los indicadores de resiliencia del ICaF:
 *   - Tiempo hasta la resolución (detected_at → resolved_at)
 *   - Número de recaídas (relapse_count)
 *   - Velocidad de detección temprana (detected_at vs severity)
 *   - ICaF en el momento de detección vs resolución
 *
 * Estados:
 *   DETECTED     → crisis identificada, sin intervención activa aún
 *   IN_PROGRESS  → ruta de intervención activa
 *   RESOLVED     → crisis resuelta (puede recaer)
 *   RELAPSED     → recaída después de resolución
 *   CLOSED       → cerrado definitivamente (sin más seguimiento)
 *
 * Categorías comunes:
 *   ALCOHOL | VIOLENCIA | COMUNICACION_ROTA | ADOLESCENTE_AISLADO |
 *   PADRE_PERMISIVO | RUPTURA_RIESGO | ANSIEDAD | DUELO | OTRO
 */
@Entity
@Table(name = "family_critical_events")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FamilyCriticalEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @Column(name = "category", nullable = false, length = 60)
    private String category;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "DETECTED";

    @Column(name = "severity", nullable = false, length = 20)
    @Builder.Default
    private String severity = "MODERATE";

    // ── Trayectoria temporal ──────────────────────────────────────────────────

    @Column(name = "detected_at", nullable = false)
    private LocalDate detectedAt;

    @Column(name = "intervention_start_at")
    private LocalDate interventionStartAt;

    @Column(name = "resolved_at")
    private LocalDate resolvedAt;

    @Column(name = "closed_at")
    private LocalDate closedAt;

    // ── Indicadores de resiliencia ────────────────────────────────────────────

    /** Días desde detección hasta resolución (calculado al resolver) */
    @Column(name = "days_to_resolution")
    private Integer daysToResolution;

    @Column(name = "relapse_count")
    @Builder.Default
    private Integer relapseCount = 0;

    @Column(name = "last_relapse_at")
    private LocalDate lastRelapseAt;

    // ── Contexto ──────────────────────────────────────────────────────────────

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "resolution_summary", columnDefinition = "TEXT")
    private String resolutionSummary;

    /** ICaF en el momento de detección — para medir impacto */
    @Column(name = "icaf_at_detection")
    private Double icafAtDetection;

    /** ICaF al resolver — para medir recuperación */
    @Column(name = "icaf_at_resolution")
    private Double icafAtResolution;

    /** ID del CriticalDay original que originó este evento (nullable) */
    @Column(name = "critical_day_id")
    private Long criticalDayId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Métodos de dominio ────────────────────────────────────────────────────

    public boolean isResolved() {
        return "RESOLVED".equals(status) || "CLOSED".equals(status);
    }

    public boolean isActive() {
        return "DETECTED".equals(status) || "IN_PROGRESS".equals(status) || "RELAPSED".equals(status);
    }

    public long daysElapsed() {
        return ChronoUnit.DAYS.between(detectedAt, LocalDate.now());
    }

    /** Registra una recaída: incrementa contador y vuelve a IN_PROGRESS */
    public void registerRelapse() {
        this.relapseCount = (this.relapseCount != null ? this.relapseCount : 0) + 1;
        this.lastRelapseAt = LocalDate.now();
        this.status = "RELAPSED";
        this.resolvedAt = null;
        this.daysToResolution = null;
    }

    /** Marca el evento como resuelto y calcula días hasta resolución */
    public void markResolved(String summary, Double currentIcaf) {
        this.status = "RESOLVED";
        this.resolvedAt = LocalDate.now();
        this.resolutionSummary = summary;
        this.icafAtResolution = currentIcaf;
        if (this.detectedAt != null) {
            this.daysToResolution = (int) ChronoUnit.DAYS.between(this.detectedAt, this.resolvedAt);
        }
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (detectedAt == null) detectedAt = LocalDate.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
