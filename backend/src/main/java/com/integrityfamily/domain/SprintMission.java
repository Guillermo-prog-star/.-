package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sprint_missions")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class SprintMission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sprint_id", nullable = false)
    private FamilySprint sprint;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, length = 30)
    private String status; // PENDING, COMPLETED

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
