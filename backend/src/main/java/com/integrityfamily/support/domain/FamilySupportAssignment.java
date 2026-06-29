package com.integrityfamily.support.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "family_support_assignments")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FamilySupportAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "support_member_id", nullable = false)
    private SupportNetworkMember supportMember;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SupportSpecialty specialty;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AssignmentStatus status = AssignmentStatus.INVITED;

    // ── Invitación (iniciada por la familia) ──────────────────────────────
    @Column(name = "invited_by_email", nullable = false)
    private String invitedByEmail;

    @Column(name = "invited_at", nullable = false)
    private LocalDateTime invitedAt;

    // ── Consentimiento explícito ──────────────────────────────────────────
    @Column(name = "consented_by_email")
    private String consentedByEmail;

    @Column(name = "consented_at")
    private LocalDateTime consentedAt;

    // ── Revocación ────────────────────────────────────────────────────────
    @Column(name = "revoked_by_email")
    private String revokedByEmail;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revocation_reason")
    private String revocationReason;

    // ── Alcance de acceso ─────────────────────────────────────────────────
    @Builder.Default
    @Column(name = "can_view_icf_score")
    private boolean canViewIcfScore = true;

    @Builder.Default
    @Column(name = "can_view_risk_level")
    private boolean canViewRiskLevel = true;

    @Builder.Default
    @Column(name = "can_view_plan_summary")
    private boolean canViewPlanSummary = false;

    @Builder.Default
    @Column(name = "can_view_sprint_progress")
    private boolean canViewSprintProgress = false;

    @Builder.Default
    @Column(name = "can_view_crisis_history")
    private boolean canViewCrisisHistory = false;

    @Builder.Default
    @Column(name = "can_leave_notes")
    private boolean canLeaveNotes = true;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { this.createdAt = LocalDateTime.now(); }
}
