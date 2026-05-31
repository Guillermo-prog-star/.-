package com.integrityfamily.weeklyplan.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "weekly_tasks")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class WeeklyTask {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "weekly_plan_id", nullable = false)
    private WeeklyPlan weeklyPlan;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String responsible;

    @Column(length = 100)
    private String when;

    @Column(length = 200)
    private String indicator;

    @Column(nullable = false)
    @Builder.Default
    private boolean done = false;

    @Column(name = "sort_order")
    private int sortOrder;
}
