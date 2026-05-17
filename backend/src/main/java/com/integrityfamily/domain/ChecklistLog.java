package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import org.hibernate.annotations.Filter;

@Entity
@Table(name = "checklist_logs")
@Filter(name = "familyFilter", condition = "family_id = :familyId")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChecklistLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private FamilyMember member;

    @Column(length = 50)
    private String milestoneKey; // Equivalente a hito_key

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LogStatus status;

    @Column(length = 500)
    private String comment;

    @Column(length = 255)
    private String evidenceUrl;

    @Column(length = 20)
    private String source;

    private LocalDateTime createdAt;

    @PrePersist
    public void pre() {
        if (createdAt == null)
            createdAt = LocalDateTime.now();
    }

    public enum EventType {
        CHECK, UNCHECK, COMMENT, EVIDENCE
    }

    public enum LogStatus {
        DONE, PENDING, SKIPPED, LATE
    }
}
