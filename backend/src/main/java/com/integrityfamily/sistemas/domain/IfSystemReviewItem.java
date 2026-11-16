package com.integrityfamily.sistemas.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "if_system_review_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IfSystemReviewItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private IfSystemReview review;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_kind", nullable = false, length = 24)
    private ReviewItemKind itemKind;

    @Column(name = "text_value", columnDefinition = "TEXT")
    private String textValue;

    @Column(name = "decision", length = 16)
    private String decision;

    @Column(name = "achievement_criterion", columnDefinition = "TEXT")
    private String achievementCriterion;

    @Column(name = "review_on")
    private LocalDate reviewOn;
}
