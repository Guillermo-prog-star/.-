package com.integrityfamily.common.domain;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private FamilyMember FamilyMember;

    private String recipientName;
    private String recipientRole;
    private String phoneNumber;

    @Column(columnDefinition = "TEXT")
    private String message;

    private String type; // PLAN_ASSIGNED, CRISIS_ALERT, MILESTONE_UP
    
    private LocalDateTime sentAt;

    @PrePersist
    public void prePersist() {
        this.sentAt = LocalDateTime.now();
    }
}


