package com.integrityfamily.biography.domain;

import com.integrityfamily.domain.EntryVisibility;
import com.integrityfamily.domain.EpistemicSource;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Respuesta de un miembro, en primera persona, a una pregunta del catálogo
 * del diario guiado biográfico. A diferencia de {@code LineageMember}
 * (donde el narrador es un tercero construyendo el árbol sobre un
 * ancestro), aquí el autor es siempre el propio miembro autenticado —
 * resuelto del principal, nunca confiado del cliente (mismo principio que
 * {@code JournalEntry}, ADR-012). Una fila por {@code (member, question)},
 * editable (upsert), no append-only.
 */
@Entity
@Table(name = "biography_answers",
        uniqueConstraints = @UniqueConstraint(name = "uq_biography_answer_member_question",
                columnNames = {"member_id", "question_id"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class BiographyAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    /** Autor de la respuesta — siempre un miembro humano, nunca null. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private FamilyMember member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private BiographyQuestion question;

    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    /** Cómo lo sabe: lo vivió, se lo contaron, lo leyó en un documento, o no está seguro. Opcional. */
    @Enumerated(EnumType.STRING)
    @Column(name = "epistemic_source", length = 20)
    private EpistemicSource epistemicSource;

    /** Cuándo cree el autor que ocurrió — texto libre (año, edad aproximada, etapa de vida). No forzamos una fecha exacta. */
    @Column(name = "occurred_period", length = 100)
    private String occurredPeriod;

    /** "Lo que pensé entonces" — la explicación que daba a la experiencia mientras la vivía. */
    @Column(name = "meaning_then", columnDefinition = "TEXT")
    private String meaningThen;

    /** "Lo que pienso hoy" — separado explícitamente del recuerdo crudo en answerText y de meaningThen. */
    @Column(name = "current_meaning", columnDefinition = "TEXT")
    private String currentMeaning;

    /** Foto, carta, documento, objeto o testigo que podría ayudar a reconstruir o confirmar esta historia. Descripción libre, no un archivo adjunto. */
    @Column(name = "related_source_note", columnDefinition = "TEXT")
    private String relatedSourceNote;

    /**
     * Clave opaca asignada por el cliente para vincular esta respuesta con la de
     * otro miembro que narra la misma escena desde su propia perspectiva — mismo
     * patrón que {@code JournalEntry.perceivedEventKey} (ADR-012). Nunca inferida
     * automáticamente.
     */
    @Column(name = "perceived_event_key", length = 64)
    private String perceivedEventKey;

    @Column(name = "confidence_level")
    private Integer confidenceLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EntryVisibility visibility = EntryVisibility.PRIVATE;

    /** null = borrador/en progreso; no-null = el autor lo marcó completo. */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** Qué cambió el autor después de releer o escuchar esta respuesta. Nota ligera, no un historial de versiones. */
    @Column(name = "correction_note", columnDefinition = "TEXT")
    private String correctionNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
