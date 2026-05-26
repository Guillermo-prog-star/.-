package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "families")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Family {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "family_code", unique = true)
    private String familyCode;

    private String pin;

    @Column(name = "current_milestone")
    private String currentMilestone;

    @Builder.Default
    @Column(name = "sentinel_active")
    private Boolean sentinelActive = false;

    private String whatsapp;

    @Column(name = "next_evaluation_at")
    private LocalDateTime nextEvaluationAt;

    @Column(name = "last_report_sent_at")
    private LocalDateTime lastReportSentAt;

    private String municipio;

    @Column(name = "country_code")
    private String countryCode;

    @Column(name = "department_code")
    private String departmentCode;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** Cuándo empezó la familia su hito actual. Se actualiza con cada avance. */
    @Column(name = "milestone_started_at")
    private LocalDateTime milestoneStartedAt;

    /** ICF promedio acumulado en el hito actual (cacheado por MilestoneService). */
    @Column(name = "milestone_icf_avg")
    private Double milestoneIcfAvg;

    // ---- Guardián Familiar ----

    /** ID del miembro elegido como Guardián Familiar. */
    @Column(name = "guardian_member_id")
    private Long guardianMemberId;

    /** Cuándo fue elegido el Guardián actual. */
    @Column(name = "guardian_since")
    private java.time.LocalDateTime guardianSince;

    /** Si el guardianato rota automáticamente cada mes. */
    @Builder.Default
    @Column(name = "rotation_enabled")
    private Boolean rotationEnabled = false;

    /** Puntuación de participación colectiva de la familia (misiones completadas). */
    @Builder.Default
    @Column(name = "participation_score")
    private Integer participationScore = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User createdBy;

    @Builder.Default
    @OneToMany(mappedBy = "family", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<FamilyMember> members = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (sentinelActive == null) sentinelActive = false;
        if (currentMilestone == null) currentMilestone = "W1";
        if (milestoneStartedAt == null) milestoneStartedAt = LocalDateTime.now();
    }
}
