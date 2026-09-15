package com.integrityfamily.vitality.domain;

import com.integrityfamily.domain.FamilyMember;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DailyVitalityLog — Fase 4 (base biológica) de ADR-009.
 *
 * Un registro por family_member_id + log_date. Todos los campos numéricos
 * son nullable: un miembro puede registrar solo sueño un día y solo
 * ejercicio otro, sin forzar el panel completo (ADR-009, Decisión 1).
 *
 * Dato crudo de captura diaria — no confundir con la dimensión "hábitos"
 * del ICF, que mide conciencia percibida, no el dato objetivo (ver ADR-009,
 * "Relación con la dimensión hábitos del ICF").
 */
@Entity
@Table(name = "daily_vitality_logs",
        uniqueConstraints = @UniqueConstraint(name = "uq_dvl_member_date", columnNames = {"family_member_id", "log_date"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyVitalityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_member_id", nullable = false)
    private FamilyMember familyMember;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(name = "sleep_hours")
    private Double sleepHours;

    @Column(name = "sleep_quality")
    private Integer sleepQuality;

    @Column(name = "exercise_minutes")
    private Integer exerciseMinutes;

    @Column(name = "nutrition_quality")
    private Integer nutritionQuality;

    @Column(name = "screen_time_before_bed_minutes")
    private Integer screenTimeBeforeBedMinutes;

    @Column(name = "fatigue_level")
    private Integer fatigueLevel;

    @Builder.Default
    @Column(name = "source", nullable = false, length = 20)
    private String source = "MANUAL";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (source == null) source = "MANUAL";
    }
}
