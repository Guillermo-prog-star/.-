package com.integrityfamily.lineage.domain;

import com.integrityfamily.domain.Family;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "family_lineages")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FamilyLineage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @Column(name = "lineage_code", unique = true, nullable = false)
    private String lineageCode;

    @Column
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Generación desde la que se construye el árbol.
     * 0 = Generación Responsable (quién usa la app).
     * Negativo = pasado (abuelos, bisabuelos...).
     * Positivo = futuro (hijos, nietos...).
     */
    @Column(name = "anchor_generation", nullable = false)
    @Builder.Default
    private Integer anchorGeneration = 0;

    /** Generación más antigua registrada (ej: -3 = tatarabuelos) */
    @Column(name = "max_past_gen", nullable = false)
    @Builder.Default
    private Integer maxPastGen = -2;

    /** Generación más futura proyectada (ej: +2 = nietos) */
    @Column(name = "max_future_gen", nullable = false)
    @Builder.Default
    private Integer maxFutureGen = 2;

    @Column(name = "vision_statement", columnDefinition = "TEXT")
    private String visionStatement;

    @Column(name = "founding_year", length = 10)
    private String foundingYear;

    @OneToMany(mappedBy = "lineage", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LineageMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "lineage", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LineageRelationship> relationships = new ArrayList<>();

    @OneToMany(mappedBy = "lineage", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LineageGenerationInfo> generationInfos = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { this.createdAt = this.updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}
