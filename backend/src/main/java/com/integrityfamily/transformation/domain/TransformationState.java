package com.integrityfamily.transformation.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * TransformationState — Estado del viaje de transformación de 36 meses de una familia.
 *
 * Es la fuente de verdad del backend sobre en qué paso del flujo maestro
 * se encuentra la familia: configuración inicial → diagnóstico → plan → pilar activo → mes actual.
 */
@Entity
@Table(name = "transformation_states")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransformationState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false, unique = true)
    private Long familyId;

    // ── Onboarding ────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_step", nullable = false)
    @Builder.Default
    private OnboardingStep onboardingStep = OnboardingStep.CREATE_FAMILY;

    // ── Pilar activo ──────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "current_pillar")
    @Builder.Default
    private Pillar currentPillar = Pillar.RECONOCIMIENTO;

    /** Mes activo dentro del plan de 36 meses (1–36) */
    @Column(name = "current_month")
    @Builder.Default
    private int currentMonth = 1;

    /** Sprint activo en el mes corriente */
    @Column(name = "current_sprint_number")
    @Builder.Default
    private int currentSprintNumber = 1;

    /** ID de la misión activa (referencia a plan_tasks) */
    @Column(name = "active_mission_id")
    private Long activeMissionId;

    /** Porcentaje global de avance (0-100) */
    @Column(name = "progress_percent")
    @Builder.Default
    private int progressPercent = 0;

    /** Etiqueta del hito (W1, M1, M6 …) */
    @Column(name = "milestone_label", length = 10)
    @Builder.Default
    private String milestoneLabel = "M1";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist  protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate   protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    // ── Enums ──────────────────────────────────────────────────────
    public enum OnboardingStep {
        CREATE_FAMILY, ADD_MEMBERS, CHOOSE_GUARDIAN, DIAGNOSIS, PLAN_GENERATED, COMPLETED
    }

    public enum Pillar {
        RECONOCIMIENTO, AMOR, ENTREGA
    }
}
