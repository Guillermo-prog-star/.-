package com.integrityfamily.ecosystem.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "family_ecosystem_links")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FamilyEcosystemLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private EcosystemParticipant participant;

    @Enumerated(EnumType.STRING)
    @Column(name = "network_type", nullable = false)
    private NetworkType networkType;

    // 1=Familiar, 2=Interdisciplinario, 3=Intersectorial
    @Column(name = "access_level", nullable = false)
    private int accessLevel;

    @Column(columnDefinition = "TEXT")
    private String objective;

    @Column(columnDefinition = "TEXT")
    private String responsibilities;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EcosystemLinkStatus status = EcosystemLinkStatus.INVITED;

    // ── Alcance de acceso ─────────────────────────────────────────────────
    @Builder.Default @Column(name = "can_view_icf_score")        private boolean canViewIcfScore      = false;
    @Builder.Default @Column(name = "can_view_risk_level")       private boolean canViewRiskLevel     = false;
    @Builder.Default @Column(name = "can_view_plan_summary")     private boolean canViewPlanSummary   = false;
    @Builder.Default @Column(name = "can_view_sprint_progress")  private boolean canViewSprintProgress = false;
    @Builder.Default @Column(name = "can_view_crisis_history")   private boolean canViewCrisisHistory  = false;
    @Builder.Default @Column(name = "can_receive_alerts")        private boolean canReceiveAlerts      = false;

    // ── Trazabilidad de consentimiento ────────────────────────────────────
    @Column(name = "invited_by_email", nullable = false, length = 180)
    private String invitedByEmail;

    @Column(name = "invited_at", nullable = false)
    private LocalDateTime invitedAt;

    @Column(name = "consented_by_email", length = 180)
    private String consentedByEmail;

    @Column(name = "consented_at")
    private LocalDateTime consentedAt;

    @Column(name = "revoked_by_email", length = 180)
    private String revokedByEmail;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revocation_reason", columnDefinition = "TEXT")
    private String revocationReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { this.createdAt = LocalDateTime.now(); }

    public boolean isExpired() {
        return validUntil != null && LocalDate.now().isAfter(validUntil);
    }
}
