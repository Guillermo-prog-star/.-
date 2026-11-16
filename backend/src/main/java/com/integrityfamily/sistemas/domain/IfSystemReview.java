package com.integrityfamily.sistemas.domain;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.FamilySprint;
import com.integrityfamily.domain.SprintDaily;
import com.integrityfamily.domain.SprintRetrospective;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "if_system_reviews")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IfSystemReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_member_id", nullable = false)
    private FamilyMember author;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_type", nullable = false, length = 16)
    private SystemReviewType reviewType;

    @Column(name = "review_date", nullable = false)
    private LocalDate reviewDate;

    @Column(name = "period_label", length = 120)
    private String periodLabel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private FamilySprint sprint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_daily_id")
    private SprintDaily sprintDaily;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retrospective_id")
    private SprintRetrospective retrospective;

    @Column(name = "capture_emptied")
    private Boolean captureEmptied;

    @Column(name = "agenda_checked")
    private Boolean agendaChecked;

    @Column(name = "calendar_reviewed")
    private Boolean calendarReviewed;

    @Column(name = "tasks_pruned")
    private Boolean tasksPruned;

    @Column(name = "commitments_transferred")
    private Boolean commitmentsTransferred;

    @Column(name = "stage_change_unnoticed")
    private Boolean stageChangeUnnoticed;

    @Column(name = "change_description", length = 240)
    private String changeDescription;

    @Column(name = "what_no_longer_fits", columnDefinition = "TEXT")
    private String whatNoLongerFits;

    @Builder.Default
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<IfSystemReviewItem> items = new ArrayList<>();

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public void addItem(IfSystemReviewItem item) {
        item.setReview(this);
        items.add(item);
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
