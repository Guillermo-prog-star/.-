package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * SDD Sprint 4: Entidad de Bitácora Estructurada (JournalEntry).
 * Posee arquitectura híbrida (metadatos estructurados + síntesis narrativa).
 */
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "journal_entries")
@Filter(name = "familyFilter", condition = "family_id = :familyId")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    /** Autor del relato (ADR-012). Null = entrada generada por sistema/IA, sin autor humano. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private FamilyMember member;

    /** Clave opaca asignada por el cliente para vincular relatos del mismo evento real (ADR-012, H2). */
    @Column(name = "perceived_event_key", length = 64)
    private String perceivedEventKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    @Builder.Default
    private EntryVisibility visibility = EntryVisibility.PRIVATE;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false, length = 30)
    private JournalOrigin origin; // RISK, PLAN, TASK, CRISIS

    @Column(name = "risk_dimension", length = 50)
    private String riskDimension; // emociones, comunicacion, habitos, tiempos

    @Column(length = 100)
    private String emotion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_task_id")
    private PlanTask relatedTask;

    @Column(name = "mood_after")
    private Integer moodAfter; // escala 1-5

    @Column(name = "compliance_status", length = 50)
    private String complianceStatus;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String reflection;

    @Column(columnDefinition = "TEXT")
    private String learning;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private JournalStatus status = JournalStatus.ACTIVE;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
