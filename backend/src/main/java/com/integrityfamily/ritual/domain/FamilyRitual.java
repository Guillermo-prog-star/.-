package com.integrityfamily.ritual.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "family_rituals")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FamilyRitual {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ritual_type", nullable = false, length = 40)
    private RitualType ritualType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RitualStatus status = RitualStatus.PENDING;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Pasos guiados generados por IA (JSON array de strings). */
    @Column(name = "guided_steps", columnDefinition = "JSON")
    private String guidedSteps;

    @Column(name = "trigger_context", length = 500)
    private String triggerContext;

    @Column(name = "triggered_at", nullable = false)
    @Builder.Default
    private LocalDateTime triggeredAt = LocalDateTime.now();

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "dismissed_at")
    private LocalDateTime dismissedAt;

    @PrePersist
    public void prePersist() {
        if (triggeredAt == null) triggeredAt = LocalDateTime.now();
        if (status == null) status = RitualStatus.PENDING;
    }
}
