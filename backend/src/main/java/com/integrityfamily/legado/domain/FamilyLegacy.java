package com.integrityfamily.legado.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * FamilyLegacy — Entidad de Legado Familiar.
 *
 * Contiene la Constitución, Misión, Visión, Historia y Carta al Futuro
 * de una familia dentro del flujo de Transformación de 36 meses.
 */
@Entity
@Table(name = "family_legacies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyLegacy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    // ── Historia ────────────────────────────────────────────────────
    @Column(columnDefinition = "TEXT")
    private String historyLessons;

    @Column(columnDefinition = "TEXT")
    private String historyConserve;

    @Column(columnDefinition = "TEXT")
    private String historyAvoidErrors;

    @Column(columnDefinition = "TEXT")
    private String historyToLeave;

    @Column(columnDefinition = "TEXT")
    private String historyRecognition;

    // ── Constitución ────────────────────────────────────────────────
    @Column(length = 200)
    private String constitutionFamilyName;

    private Integer constitutionYear;

    @Column(columnDefinition = "TEXT")
    private String foundingPrinciple;

    @Column(columnDefinition = "TEXT")
    private String commitments;

    @Column(columnDefinition = "TEXT")
    private String neverDo;

    @Column(columnDefinition = "TEXT")
    private String conflictResolution;

    // ── Misión & Visión ─────────────────────────────────────────────
    @Column(columnDefinition = "TEXT")
    private String familyMission;

    @Column(columnDefinition = "TEXT")
    private String familyVision;

    @Column(length = 300)
    private String familyTagline;

    // ── Carta al Futuro ─────────────────────────────────────────────
    @Column(length = 200)
    private String letterFrom;

    @Column(length = 200)
    private String letterTo;

    private Integer letterOpenInYear;

    @Column(columnDefinition = "TEXT")
    private String letterContent;

    @Column(name = "letter_sealed", nullable = false)
    @Builder.Default
    private boolean letterSealed = false;

    // ── Metadata ────────────────────────────────────────────────────
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
