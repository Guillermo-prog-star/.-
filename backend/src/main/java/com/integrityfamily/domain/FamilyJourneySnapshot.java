package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "family_journey_snapshots")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FamilyJourneySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "journey_level", nullable = false)
    private int journeyLevel;

    @Column(name = "journey_progress", nullable = false)
    private int journeyProgress;

    @Column(name = "level_up", nullable = false)
    private boolean levelUp = false;

    @Column(name = "previous_level")
    private Integer previousLevel;

    @Column(name = "celebration_sent", nullable = false)
    private boolean celebrationSent = false;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
