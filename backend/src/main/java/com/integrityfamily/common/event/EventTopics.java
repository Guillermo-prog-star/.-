package com.integrityfamily.common.event;

/**
 * Contratos del Event Bus Familiar — IF Sistema Vivo.
 *
 * Cada constante representa un evento de dominio que atraviesa transversalmente
 * los módulos de Integrity Family. Ningún módulo opera de forma aislada:
 * cada evento puede modificar riesgo, planes, ICF, IA y estado longitudinal.
 *
 * Convención: family.<dominio>.<accion>
 */
public final class EventTopics {

    // ── Evaluación y Diagnóstico ─────────────────────────────────────────────
    /** Diagnóstico emocional completado → activa Motor Inferencial */
    public static final String EVALUATION_COMPLETED         = "family.assessment.completed";

    // ── Riesgo Familiar ──────────────────────────────────────────────────────
    /** ICF recalculado → notifica Dashboard, IA y Planes */
    public static final String ICF_RECALCULATED             = "family.icf.recalculated";
    /** Nivel de riesgo cambió (BAJO/MODERADO/ALTO/CRITICO) */
    public static final String RISK_CHANGED                 = "family.risk.changed";
    /** Riesgo aumentó de nivel → escala prioridad de IA */
    public static final String RISK_INCREASED               = "family.risk.increased";

    // ── Crisis ───────────────────────────────────────────────────────────────
    /** Crisis registrada → cascada sistémica: ICF + planes + IA + alertas */
    public static final String CRISIS_TRIGGERED             = "family.crisis.triggered";
    /** Crisis resuelta → reevaluación automática */
    public static final String CRISIS_RESOLVED              = "family.crisis.resolved";

    // ── Planes y Misiones ────────────────────────────────────────────────────
    /** Plan adaptativo generado o regenerado */
    public static final String PLAN_GENERATED               = "family.plan.generated";
    /** Plan ajustado por Motor Inferencial */
    public static final String PLAN_ADJUSTED                = "family.plan.adjusted";
    /** Misión completada → actualiza adherencia y evolución */
    public static final String MISSION_COMPLETED            = "family.mission.completed";
    /** Misión fallida → evalúa reducción de carga */
    public static final String MISSION_FAILED               = "family.mission.failed";

    // ── Bitácora ─────────────────────────────────────────────────────────────
    /** Entrada de bitácora agregada → modifica inferencias y riesgo */
    public static final String JOURNAL_ENTRY_ADDED          = "family.journal.entry.added";
    /** Deterioro comunicacional detectado en bitácora */
    public static final String COMMUNICATION_COLLAPSE       = "family.communication.collapse.detected";
    /** Mejora emocional detectada */
    public static final String EMOTIONAL_IMPROVEMENT        = "family.emotional-improvement.detected";

    // ── Consultor IA ─────────────────────────────────────────────────────────
    /** Contexto IA actualizado con nuevo estado familiar */
    public static final String AI_CONTEXT_UPDATED           = "family.ai.context.updated";
    /** Recomendación IA generada */
    public static final String AI_RECOMMENDATION_GENERATED  = "family.ai.recommendation.generated";

    // ── Evolución Longitudinal ───────────────────────────────────────────────
    /** Hito de evolución alcanzado (Reconocimiento → Amor → Entrega) */
    public static final String EVOLUTION_MILESTONE_REACHED  = "family.evolution.milestone.reached";
    /** Estado longitudinal familiar actualizado */
    public static final String LONGITUDINAL_STATE_UPDATED   = "family.longitudinal.state.updated";

    // ── Sistema ──────────────────────────────────────────────────────────────
    /** Reevaluación sistémica completa disparada (post-crisis o cambio brusco) */
    public static final String SYSTEM_REBALANCED            = "family.system.rebalanced";

    private EventTopics() {}
}


