package com.integrityfamily.sistemas.domain;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "if_system_checks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IfSystemCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_member_id", nullable = false)
    private FamilyMember author;

    @Column(name = "window_start", nullable = false)
    private LocalDate windowStart;

    @Column(name = "window_end", nullable = false)
    private LocalDate windowEnd;

    @Column(name = "weekly_reviews_done", nullable = false)
    private int weeklyReviewsDone;

    @Column(name = "capture_emptied_every_week", nullable = false)
    private boolean captureEmptiedEveryWeek;

    @Column(name = "unregistered_commitment", nullable = false)
    private boolean unregisteredCommitment;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
