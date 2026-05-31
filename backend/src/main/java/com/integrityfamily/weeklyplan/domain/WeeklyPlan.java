package com.integrityfamily.weeklyplan.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "weekly_plans",
       uniqueConstraints = @UniqueConstraint(columnNames = {"family_id","sprint_number","phase"}))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class WeeklyPlan {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    /** Número del sprint familiar al que pertenece este plan */
    @Column(name = "sprint_number", nullable = false)
    private int sprintNumber;

    /**
     * Fase de la semana: PREPARE | EXECUTE | EVALUATE | CONSOLIDATE
     */
    @Column(nullable = false, length = 20)
    private String phase;

    @Column(name = "week_start_date")
    private LocalDate weekStartDate;

    @OneToMany(mappedBy = "weeklyPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WeeklyTask> tasks = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist  protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate   protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
