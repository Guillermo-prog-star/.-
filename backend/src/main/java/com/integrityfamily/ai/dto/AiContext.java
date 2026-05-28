package com.integrityfamily.ai.dto;

import java.util.List;
import java.util.Map;

/**
 * SDD-AI-03.3 v2: High-Fidelity Relational Context Record.
 * Añade identidad del miembro activo, perfil del Guardián,
 * snapshot cognitivo y estado del plan activo.
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
    ActivePlanSnapshot activePlan
) {
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
}
