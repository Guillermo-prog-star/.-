package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "milestones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Milestone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "milestone_key", unique = true, nullable = false, length = 50)
    private String milestoneKey;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "bloque", length = 50)
    private String bloque;

    @Column(name = "phase", length = 50)
    private String phase;

    @Column(name = "months")
    private Integer months;

    @Column(name = "sort_order")
    private Integer sortOrder;

    public String getName() {
        return title;
    }
}
