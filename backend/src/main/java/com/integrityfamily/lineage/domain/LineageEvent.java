package com.integrityfamily.lineage.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lineage_events")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class LineageEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private LineageMember member;

    @Column(name = "event_year", length = 20)
    private String eventYear;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** birth | death | marriage | migration | trauma | achievement | milestone */
    @Column(name = "event_type", length = 50)
    private String eventType;

    @Column(name = "is_approximate")
    private Boolean isApproximate;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
