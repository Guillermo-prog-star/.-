package com.integrityfamily.errorprotocol.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * FamilyErrorProtocol — Registro del protocolo de gestión del error familiar.
 *
 * Ciclo: Detectar → Sentir → Comprender → Accionar → Acordar → Seguimiento → Aprender
 */
@Entity
@Table(name = "family_error_protocols")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyErrorProtocol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "mission_failed", columnDefinition = "TEXT")
    private String missionFailed;

    // Sentir
    @Column(columnDefinition = "TEXT")
    private String feelings;

    // Comprender
    @Column(name = "what_happened", columnDefinition = "TEXT")
    private String whatHappened;

    // Accionar
    @Column(name = "corrective_action", columnDefinition = "TEXT")
    private String correctiveAction;

    @Column(name = "who_helps", length = 200)
    private String whoHelps;

    // Acordar
    @Column(columnDefinition = "TEXT")
    private String agreement;

    @Column(name = "followup_date")
    private LocalDate followupDate;

    // Aprender
    @Column(columnDefinition = "TEXT")
    private String learning;

    @Column(nullable = false)
    @Builder.Default
    private boolean closed = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_step", nullable = false)
    @Builder.Default
    private ProtocolStep currentStep = ProtocolStep.DETECT;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }

    public enum ProtocolStep {
        DETECT, FEEL, UNDERSTAND, ACTION, AGREEMENT, FOLLOWUP, LEARNING
    }
}
