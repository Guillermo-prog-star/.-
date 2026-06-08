package com.integrityfamily.movie.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "family_movies")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FamilyMovie {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "period_label", nullable = false, length = 100)
    private String periodLabel;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    // ── Estadísticas ─────────────────────────────────────────────────────────
    @Builder.Default @Column(name = "evidences_count")    private Integer evidencesCount   = 0;
    @Builder.Default @Column(name = "gratitudes_count")   private Integer gratitudesCount  = 0;
    @Builder.Default @Column(name = "missions_completed") private Integer missionsCompleted= 0;
    @Builder.Default @Column(name = "crises_count")       private Integer crisesCount      = 0;
    @Builder.Default @Column(name = "rituals_completed")  private Integer ritualsCompleted = 0;
    @Builder.Default @Column(name = "days_active")        private Integer daysActive       = 0;
    @Builder.Default @Column(name = "best_streak")        private Integer bestStreak       = 0;

    @Column(name = "icf_start")  private Double icfStart;
    @Column(name = "icf_end")    private Double icfEnd;
    @Column(name = "icf_delta")  private Double icfDelta;

    // ── Narrativa IA ─────────────────────────────────────────────────────────
    @Column(name = "opening_line",    columnDefinition = "TEXT") private String openingLine;
    @Column(name = "chapter_1",       columnDefinition = "TEXT") private String chapter1;
    @Column(name = "chapter_2",       columnDefinition = "TEXT") private String chapter2;
    @Column(name = "chapter_3",       columnDefinition = "TEXT") private String chapter3;
    @Column(name = "mentor_letter",   columnDefinition = "TEXT") private String mentorLetter;
    @Column(name = "highlight_quote", columnDefinition = "TEXT") private String highlightQuote;

    @Column(name = "generated_at", nullable = false) @Builder.Default
    private LocalDateTime generatedAt = LocalDateTime.now();

    @Column(name = "generation_model", length = 50)
    private String generationModel;

    @PrePersist public void prePersist() {
        if (generatedAt == null) generatedAt = LocalDateTime.now();
    }
}
