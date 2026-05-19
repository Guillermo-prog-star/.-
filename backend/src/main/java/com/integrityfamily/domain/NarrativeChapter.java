package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * SDD Fase 4 — Capítulo de la historia evolutiva familiar.
 *
 * Cada familia tiene una narrativa compuesta de capítulos que representan
 * fases distintas de su transformación. Los capítulos se abren cuando hay
 * un cambio de fase o un punto de inflexión (crisis, recuperación, breakthrough).
 * Solo puede haber un capítulo abierto (closedAt == null) por familia a la vez.
 */
@Entity
@Table(name = "narrative_chapters",
        indexes = {
            @Index(name = "idx_narrative_family", columnList = "family_id"),
            @Index(name = "idx_narrative_open", columnList = "family_id, closed_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NarrativeChapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @Column(name = "chapter_number", nullable = false)
    private Integer chapterNumber;

    /** Título narrativo del capítulo: "El Despertar", "La Tormenta del Tercer Mes" */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /** Párrafo narrativo descriptivo de lo que ocurrió en este capítulo */
    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    /**
     * Fase de evolución familiar en este capítulo.
     * AWAKENING → DISCOVERY → TRANSITION → CONSOLIDATION → AUTONOMY
     * CRISIS y RECOVERY pueden intercalarse en cualquier momento.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "phase", nullable = false, length = 20)
    private NarrativePhase phase;

    /** ICF al inicio del capítulo */
    @Column(name = "icf_at_open")
    private Double icfAtOpen;

    /** ICF al cierre del capítulo (null si aún abierto) */
    @Column(name = "icf_at_close")
    private Double icfAtClose;

    /** Descripción del evento que desencadenó este capítulo */
    @Column(name = "key_event", length = 400)
    private String keyEvent;

    /** true si este capítulo representa un punto de inflexión significativo */
    @Column(name = "turning_point")
    @Builder.Default
    private Boolean turningPoint = false;

    @Column(name = "opened_at", updatable = false)
    @Builder.Default
    private LocalDateTime openedAt = LocalDateTime.now();

    /** null = capítulo actualmente abierto */
    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean isOpen() {
        return closedAt == null;
    }

    public enum NarrativePhase {
        AWAKENING,      // Primera evaluación — la familia se ve por primera vez
        DISCOVERY,      // Patrones emergen, insights iniciales
        TRANSITION,     // Cambio activo en marcha
        CONSOLIDATION,  // Ganancias sostenidas ≥ 3 ciclos
        CRISIS,         // ICF cae > 15 pts o hasCrisis = true
        RECOVERY,       // ICF remonta desde una crisis
        AUTONOMY        // Familia opera sin intervención intensa (ICF ≥ 80, ciclos ≥ 12)
    }
}
