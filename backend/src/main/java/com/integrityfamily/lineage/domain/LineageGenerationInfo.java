package com.integrityfamily.lineage.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "lineage_generation_info",
       uniqueConstraints = @UniqueConstraint(columnNames = {"lineage_id", "generation_level"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class LineageGenerationInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lineage_id", nullable = false)
    private FamilyLineage lineage;

    /**
     * Nivel de generación: -3=tatarabuelos ... 0=responsable ... +3=bisnietos
     */
    @Column(name = "generation_level", nullable = false)
    private Integer generationLevel;

    /**
     * founding | builder | responsible | current | future | projected
     */
    @Column(name = "generation_type", length = 30, nullable = false)
    @Builder.Default
    private String generationType = "responsible";

    @Column(length = 120)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    /** Contexto histórico / social que vivió esa generación */
    @Column(columnDefinition = "TEXT")
    private String context;

    /** Principal desafío que enfrentó */
    @Column(name = "key_challenge", columnDefinition = "TEXT")
    private String keyChallenge;

    /** Principal logro de la generación */
    @Column(name = "key_achievement", columnDefinition = "TEXT")
    private String keyAchievement;

    /** Año aproximado de inicio del ciclo (ej: "1920") */
    @Column(name = "period_start", length = 10)
    private String periodStart;

    /** Año aproximado de cierre del ciclo (ej: "1960") */
    @Column(name = "period_end", length = 10)
    private String periodEnd;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { this.createdAt = this.updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}
