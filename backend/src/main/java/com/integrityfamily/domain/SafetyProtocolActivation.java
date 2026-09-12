package com.integrityfamily.domain;

import com.integrityfamily.support.domain.FamilySupportAssignment;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * SafetyProtocolActivation — respuesta estructurada a una trayectoria con
 * requires_safety_protocol=true (ver RiskTrajectory / V97).
 *
 * Deliberadamente separada de FamilyErrorProtocol: va directo a acción y
 * seguimiento, con responsable real (FK a FamilyMember) y fecha de
 * seguimiento obligatoria. Se activa manualmente, nunca en automático.
 */
@Entity
@Table(name = "safety_protocol_activations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafetyProtocolActivation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_risk_trajectory_id", nullable = false)
    private FamilyRiskTrajectory familyTrajectory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_id", nullable = false)
    private FamilyMember responsible;

    @Column(name = "initial_action", nullable = false, columnDefinition = "TEXT")
    private String initialAction;

    @Column(name = "follow_up_date", nullable = false)
    private LocalDate followUpDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "support_assignment_id")
    private FamilySupportAssignment supportAssignment;

    @Column(nullable = false)
    @Builder.Default
    private Boolean closed = false;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "activated_by", length = 255)
    private String activatedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (closed == null) closed = false;
    }
}
