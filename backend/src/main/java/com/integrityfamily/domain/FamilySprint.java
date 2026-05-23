package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "family_sprints")
@Filter(name = "familyFilter", condition = "family_id = :familyId")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class FamilySprint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @Column(nullable = false, length = 255)
    private String objective;

    @Column(name = "risk_dimension", nullable = false, length = 50)
    private String riskDimension; // emociones, comunicacion, habitos, tiempos

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays; // 7 or 15

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, length = 30)
    private String status; // ACTIVE, COMPLETED, CANCELLED

    @OneToMany(mappedBy = "sprint", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SprintMission> missions = new ArrayList<>();

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
