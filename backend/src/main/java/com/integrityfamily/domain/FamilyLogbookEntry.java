package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * SDD: Entidad de Bitácora de Transformación Familiar.
 */
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "family_logbook_entries")
@Filter(name = "familyFilter", condition = "family_id = :familyId")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class FamilyLogbookEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @Column(nullable = false, length = 1000)
    private String situation;

    @Column(name = "difficulty_detected", nullable = false, length = 1000)
    private String difficultyDetected;

    @Column(name = "emotion_identified", nullable = false, length = 255)
    private String emotionIdentified;

    @Column(nullable = false, length = 1000)
    private String understanding;

    @Column(name = "correction_action", nullable = false, length = 1000)
    private String correctionAction;

    @Column(name = "family_agreement", nullable = false, length = 1000)
    private String familyAgreement;

    @Column(name = "progress_evidence", length = 1000)
    private String progressEvidence;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LogbookStatus status = LogbookStatus.OPEN;

    @Column(name = "created_by", length = 120)
    private String createdBy;

    @Column(name = "resolved_by", length = 120)
    private String resolvedBy;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public FamilyLogbookEntry(
            Family family,
            String situation,
            String difficultyDetected,
            String emotionIdentified,
            String understanding,
            String correctionAction,
            String familyAgreement,
            String createdBy
    ) {
        this.family = family;
        this.situation = situation;
        this.difficultyDetected = difficultyDetected;
        this.emotionIdentified = emotionIdentified;
        this.understanding = understanding;
        this.correctionAction = correctionAction;
        this.familyAgreement = familyAgreement;
        this.createdBy = createdBy;
        this.status = LogbookStatus.OPEN;
        this.createdAt = LocalDateTime.now();
    }

    public void resolve(String progressEvidence, String resolvedBy) {
        if (this.status == LogbookStatus.RESOLVED) {
            throw new IllegalStateException("La entrada de bitácora ya está resuelta.");
        }
        this.progressEvidence = progressEvidence;
        this.resolvedBy = resolvedBy;
        this.status = LogbookStatus.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
    }
}
