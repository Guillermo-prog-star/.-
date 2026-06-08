package com.integrityfamily.ai.dto;

import java.util.List;
import java.util.Map;

/**
 * SDD-AI-03.3 v3: High-Fidelity Relational Context Record.
 * Fase A: conecta memoria cognitiva, grafo relacional y nivel de intervención.
 */
public record AiContext(
    FamilyMetadata family,
    List<MemberNode> members,
    IntegrityMetrics metrics,
    TrendAnalysis trends,
    Map<String, Double> dimensionScores,
    List<ActiveMission> missions,
    List<MessageHistory> history,
    boolean sentinelActive,
    String currentSentiment,
    // ── Fase 1: Contexto Relacional ──────────────────
    ActiveMemberProfile activeMember,
    GuardianProfile guardian,
    CognitiveSnapshot cognitive,
    ActivePlanSnapshot activePlan,
    // ── Fase A: Motor Cognitivo Conectado ────────────
    String memoryContext,    // semántica + identidad familiar, null si sin datos
    String relationalGraph,  // resumen del grafo de relaciones, null si sin datos
    String interventionLevel, // NONE | ATTENTION | URGENT | CRISIS
    // ── Fase B: Perfil de Identidad del Miembro ──────
    MemberIdentitySnapshot memberIdentity, // null si sin memberId o sin perfil aún
    // ── Fase C: Arco Emocional + Objetivo Conversacional
    String emotionalArc,      // STABLE | MILD_TENSION | ESCALATING | ESCALATED | DE_ESCALATING | null
    String conversationGoal,  // GENERAL | SUPPORT | REFLECTION | PLANNING | CRISIS_CONTAINMENT | null
    // ── Arquitectura Epistemológica: Estado Longitudinal ─────────────────────
    LongitudinalContext longitudinal,  // memoria estructural de la familia a lo largo del tiempo
    // ── Sprint Activo: Misión en curso ───────────────────────────────────────
    ActiveSprintSnapshot activeSprint,  // null si no hay sprint activo
    // ── ADN Familiar: Identidad evolutiva persistente ────────────────────────
    String familyDna,    // bloque textual de valores, fortalezas, sombras y patrones; null si sin datos
    // ── Motor de Rituales: rituales activos ───────────────────────────────────
    String activeRituals,   // bloque textual de rituales pendientes; null si no hay rituales activos
    // ── Árbol Generacional ────────────────────────────────────────────────────
    String generationalTree,  // contexto del árbol: ancestros y descendientes; null si sin vínculos
    // ── Motor de Contexto: estado familiar unificado ──────────────────────────
    String familyContext,     // estado en tiempo real: conexión, estrés, mood, racha; null si sin datos
    // ── Gemelo Digital: firma conductual y predicciones ───────────────────────
    String digitalTwin        // firma conductual + predicciones activas; null si sin datos
) {
    /** Sprint activo: objetivo, dimensión de riesgo, misiones en curso y avance. */
    public record ActiveSprintSnapshot(
        Long sprintId,
        String objective,
        String riskDimension,
        int durationDays,
        String startDate,
        String endDate,
        int totalMissions,
        int completedMissions,
        double progressPercent,
        List<String> pendingMissions
    ) {}

    public record FamilyMetadata(String name, String milestone, String lastUpdate) {}
    public record MemberNode(String firstName, String role) {}
    public record IntegrityMetrics(Double icf, String riskLevel, String consciousnessLabel) {}
    public record TrendAnalysis(Double previousIcf, Double delta, Double velocity) {}
    public record ActiveMission(String title, String description) {}
    public record MessageHistory(String role, String content) {}

    /** Quién está hablando: rol, nivel de conciencia y si es el Guardián. */
    public record ActiveMemberProfile(
        Long memberId,
        String fullName,
        String role,
        boolean isGuardian,
        String consciousnessLevel,
        String lastParticipation
    ) {}

    /** Estado del Guardián Familiar y métricas de participación del núcleo. */
    public record GuardianProfile(
        Long memberId,
        String fullName,
        int familySize,
        int activeParticipants,
        int inactiveMembers,
        String fatigueSignal   // NONE | MILD | HIGH
    ) {}

    /** Resumen compacto de la memoria cognitiva, narrativa e identidad familiar. */
    public record CognitiveSnapshot(
        String evolutionStage,
        String communicationStyle,
        String currentChapterPhase,
        boolean turningPoint,
        String lastLesson,
        List<String> activeSkills,
        String abandonmentRisk
    ) {}

    /** Plan activo: hito actual, pilar, misiones pendientes y tasa de cumplimiento. */
    public record ActivePlanSnapshot(
        Long planId,
        String currentMilestone,
        String pillar,
        List<ActiveMission> nextMissions,
        double completionRate
    ) {}

    /** Perfil conversacional individual del miembro. Se construye progresivamente. */
    public record MemberIdentitySnapshot(
        String communicationStyle,   // DIRECT | REFLECTIVE | AVOIDANT | ASSERTIVE
        int reflexivityLevel,        // 1-5
        int emotionalSensitivity,    // 1-5
        String changeResistance,     // LOW | MED | HIGH
        String evasionPatterns,      // JSON array como String, puede ser null
        String motivators            // JSON array como String, puede ser null
    ) {}

    /**
     * Estado Longitudinal Familiar — Memoria estructural para el Consultor IA.
     *
     * Sin este contexto, la IA responde sin historial (FALLA 2 — IA sin estado longitudinal).
     * Con este contexto, cada respuesta tiene trayectoria real, tendencia y explicabilidad causal.
     *
     * Representa el "estado vivo" de la familia:
     *   - dónde estaban hace 30/90 días vs. hoy
     *   - cuántas crisis han tenido
     *   - si la tendencia es IMPROVING / DETERIORATING
     *   - en qué fase evolutiva están (inconsciente → reactivo → consciente → pleno)
     *   - si hay deterioro emocional sostenido (≥3 entradas negativas consecutivas)
     */
    public record LongitudinalContext(
        Double icfCurrent,                  // ICF hoy
        Double icfDelta30d,                 // delta vs 30 días atrás (positivo = mejora)
        String riskTrend,                   // IMPROVING | STABLE | DETERIORATING | CRITICAL
        String currentRiskLevel,            // BAJO | MODERADO | ALTO | CRITICO
        String evolutionPhase,              // inconsciente | reactivo | consciente | pleno
        String narrativeStage,              // RECONOCIMIENTO | AMOR | ENTREGA
        String consciousnessLabel,          // Plena | Madurando | Consciente | Reactiva | Inconsciente
        int crisisCount30d,                 // crisis en los últimos 30 días
        int consecutiveDeteriorations,      // entradas de bitácora negativas consecutivas
        int consecutiveImprovements,        // entradas de bitácora positivas consecutivas
        boolean communicationCollapseActive,// colapso comunicacional activo
        boolean inActiveCrisis,             // crisis en últimas 48h
        String criticalDimension            // dimensión más crítica: emociones|comunicacion|habitos|tiempos
    ) {
        /** Resumen en una línea para incluir en el prompt del Consultor IA */
        public String toPromptSummary() {
            return String.format(
                "ICF actual=%.1f | Delta 30d=%+.1f | Tendencia=%s | Riesgo=%s | Fase=%s | " +
                "Etapa narrativa=%s | Consciencia=%s | Crisis 30d=%d | " +
                "Deterioro sostenido=%d | Mejora sostenida=%d | " +
                "Colapso comunicacional=%s | Crisis activa=%s | Dim.crítica=%s",
                icfCurrent != null ? icfCurrent : 50.0,
                icfDelta30d != null ? icfDelta30d : 0.0,
                riskTrend, currentRiskLevel, evolutionPhase,
                narrativeStage, consciousnessLabel, crisisCount30d,
                consecutiveDeteriorations, consecutiveImprovements,
                communicationCollapseActive ? "SÍ ⚠️" : "No",
                inActiveCrisis ? "SÍ 🆘" : "No",
                criticalDimension
            );
        }
    }
}
