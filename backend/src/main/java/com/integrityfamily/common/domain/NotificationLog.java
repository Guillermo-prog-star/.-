package com.integrityfamily.common.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_logs")
@Data
public class NotificationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", insertable = false, updatable = false)
    private Long familyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    @JsonIgnore
    private Family family;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    @JsonIgnore
    private FamilyMember familyMember;

    private String recipientName;
    private String recipientRole;
    private String phoneNumber;

    @Column(columnDefinition = "TEXT")
    private String message;

    private String type; // PLAN_ASSIGNED, CRISIS_ALERT, MILESTONE_UP, EVIDENCE_VALIDATED, EVIDENCE_REJECTED

    private String title;

    private boolean viewed = false;

    private LocalDateTime sentAt;

    @PrePersist
    public void prePersist() {
        this.sentAt = LocalDateTime.now();
    }
}


