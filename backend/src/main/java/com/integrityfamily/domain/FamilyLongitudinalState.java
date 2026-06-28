package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Estado Longitudinal Familiar — La memoria estructural del sistema vivo.
 *
 * Este entity es el corazón de la arquitectura event-driven de Integrity Family.
 * Se actualiza automáticamente con cada evento del bus familiar:
 *   - assessment.completed → recalcula dimensiones
 *   - crisis.triggered     → incrementa crisisCount, escala riesgo
 *   - journal.entry.added  → ajusta tendencia y señales emocionales
 *   - plan.adjusted        → actualiza adherencia y fase
 *   - icf.recalculated     → sincroniza icfCurrent y trend
 *
 * Mantiene la trayectoria familiar completa:
 *   RECONOCIMIENTO → AMOR → ENTREGA
 *   inconsciente  → reactivo → consciente → pleno
 *
 * Sin este entity, el Consultor IA responde sin memoria estructural (FALLA 2).
 * Con este entity, cada respuesta IA tiene contexto longitudinal real.
 */
@Entity
@Table(name = "family_longitudinal_state",
       uniqueConstraints = @UniqueConstraint(columnNames = "family_id"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FamilyLongitudinalState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false, unique = true)
    private Family family;

    // ── ICF y Riesgo ─────────────────────────────────────────────────────────

    /** ICF actual (0-100) — último valor calculado */
    @Column(name = "icf_current")
    private Double icfCurrent;

    /** ICF hace 30 días — para calcular tendencia */
    @Column(name = "icf_30d_ago")
    private Double icf30dAgo;

    /** ICF hace 90 días — para evolución a largo plazo */
    @Column(name = "icf_90d_ago")
    private Double icf90dAgo;

    /** Nivel de riesgo actual: BAJO | MODERADO | ALTO | CRITICO */
    @Column(name = "current_risk_level")
    private String currentRiskLevel;

    /** Tendencia: IMPROVING | STABLE | DETERIORATING | CRITICAL */
    @Column(name = "risk_trend")
    private String riskTrend;

    // ── Dimensiones ICF ───────────────────────────────────────────────────────

    /** Dimensión emociones (0-100) */
    @Column(name = "dim_emociones")
    private Double dimEmociones;

    /** Dimensión comunicación (0-100) */
    @Column(name = "dim_comunicacion")
    private Double dimComunicacion;

    /** Dimensión hábitos (0-100) */
    @Column(name = "dim_habitos")
    private Double dimHabitos;

    /** Dimensión tiempos compartidos (0-100) */
    @Column(name = "dim_tiempos")
    private Double dimTiempos;

    /** Dimensión más crítica (la más baja) */
    @Column(name = "critical_dimension")
    private String criticalDimension;

    // ── Crisis y Señales ──────────────────────────────────────────────────────

    /** Número de crisis en los últimos 30 días */
    @Builder.Default
    @Column(name = "crisis_count_30d")
    private Integer crisisCount30d = 0;

    /** Número de crisis total en el historial */
    @Builder.Default
    @Column(name = "crisis_count_total")
    private Integer crisisCountTotal = 0;

    /** Fecha de la última crisis registrada */
    @Column(name = "last_crisis_at")
    private LocalDateTime lastCrisisAt;

    /** Entradas de bitácora con deterioro consecutivas (moodAfter ≤ 2) */
    @Builder.Default
    @Column(name = "consecutive_deteriorations")
    private Integer consecutiveDeteriorations = 0;

    /** Entradas de bitácora con mejora consecutivas (moodAfter ≥ 4) */
    @Builder.Default
    @Column(name = "consecutive_improvements")
    private Integer consecutiveImprovements = 0;

    /** true si hay colapso comunicacional activo (≥ 3 entradas neg. en comunicación / 7 días) */
    @Builder.Default
    @Column(name = "communication_collapse_active")
    private Boolean communicationCollapseActive = false;

    // ── Evolución Longitudinal ────────────────────────────────────────────────

    /** Fase de consciencia: inconsciente | reactivo | consciente | pleno */
    @Column(name = "evolution_phase")
    private String evolutionPhase;

    /** Etapa narrativa: RECONOCIMIENTO | AMOR | ENTREGA */
    @Column(name = "narrative_stage")
    private String narrativeStage;

    /** Nivel de consciencia 1-5 (1=Plena, 5=Inconsciente) */
    @Column(name = "consciousness_level")
    private Integer consciousnessLevel;

    /** Etiqueta del nivel: Plena | Madurando | Consciente | Reactiva | Inconsciente */
    @Column(name = "consciousness_label")
    private String consciousnessLabel;

    // ── ICaF — Capital Familiar ───────────────────────────────────────────────

    /** ICaF actual (0-100) — último valor calculado */
    @Column(name = "icaf_current")
    private Double icafCurrent;

    /** ICaF hace ~6 meses */
    @Column(name = "icaf_6m_ago")
    private Double icaf6mAgo;

    /** ICaF hace ~12 meses */
    @Column(name = "icaf_12m_ago")
    private Double icaf12mAgo;

    /** ICaF hace ~36 meses */
    @Column(name = "icaf_36m_ago")
    private Double icaf36mAgo;

    /** Nivel de Madurez Familiar 1-5 */
    @Column(name = "icaf_madurez")
    private Integer icafMadurez;

    /** Tendencia ICaF: IMPROVING | STABLE | DECLINING */
    @Column(name = "icaf_trend", length = 20)
    private String icafTrend;

    /** Timestamp del último cálculo ICaF */
    @Column(name = "icaf_last_calculated")
    private java.time.LocalDateTime icafLastCalculated;

    // ── Adherencia y Planes ───────────────────────────────────────────────────

    /** Porcentaje de adherencia al plan actual (0-100) */
    @Column(name = "plan_adherence_percent")
    private Double planAdherencePercent;

    /** Días de inactividad consecutivos (sin misiones ni bitácora) */
    @Builder.Default
    @Column(name = "inactivity_days")
    private Integer inactivityDays = 0;

    // ── Control temporal ──────────────────────────────────────────────────────

    /** Fecha de la última evaluación diagnóstica completada */
    @Column(name = "last_assessment_at")
    private LocalDateTime lastAssessmentAt;

    /** Fecha de la última entrada de bitácora */
    @Column(name = "last_journal_at")
    private LocalDateTime lastJournalAt;

    /** Última vez que se actualizó este estado */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Primera vez que se creó */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── Métodos de dominio ────────────────────────────────────────────────────

    /** true si la familia está en crisis activa (≤ 48h) */
    public boolean isInActiveCrisis() {
        return lastCrisisAt != null &&
               lastCrisisAt.isAfter(LocalDateTime.now().minusHours(48));
    }

    /** true si hay señal de deterioro emocional sostenido */
    public boolean hasEmotionalDeterioration() {
        return consecutiveDeteriorations != null && consecutiveDeteriorations >= 3;
    }

    /** true si la familia está en trayectoria de mejora */
    public boolean isImprovingTrend() {
        return "IMPROVING".equals(riskTrend);
    }

    /** Delta ICF vs 30 días atrás (positivo = mejora) */
    public double icfDelta30d() {
        if (icfCurrent == null || icf30dAgo == null) return 0.0;
        return icfCurrent - icf30dAgo;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
