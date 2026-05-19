package com.integrityfamily.cognitive.dto;

import com.integrityfamily.domain.MemberRelationEdge.DynamicType;
import com.integrityfamily.domain.NarrativeChapter.NarrativePhase;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTOs del Sistema Cognitivo Familiar — SDD Fases 1–5.
 * Todas las respuestas son records inmutables, sin referencias a entidades JPA.
 */
public class CognitiveDtos {

    // ─── Snapshot principal ──────────────────────────────────────────────────

    /** Respuesta completa del sistema cognitivo para el dashboard */
    public record CognitiveSnapshotResponse(
            Long familyId,
            IdentityProfileDto identityProfile,
            CurrentChapterDto currentChapter,
            int totalChapters,
            long turningPoints,
            GraphSummaryDto graphSummary,
            List<MemoryDto> recentMemories,
            List<SkillDto> appliedSkills,
            String storyArcSummary,
            LocalDateTime generatedAt
    ) {}

    // ─── Identidad ───────────────────────────────────────────────────────────

    public record IdentityProfileDto(
            String evolutionStage,
            String communicationStyle,
            String conflictStyle,
            String emotionalExpression,
            double adaptabilityIndex,
            int completedCycles,
            String identityNarrative
    ) {}

    // ─── Narrativa ───────────────────────────────────────────────────────────

    public record NarrativeResponse(
            Long familyId,
            List<ChapterDto> chapters,
            String currentPhase,
            int totalChapters,
            long turningPoints,
            String storyArcSummary
    ) {}

    public record ChapterDto(
            int chapterNumber,
            String title,
            String body,
            NarrativePhase phase,
            Double icfAtOpen,
            Double icfAtClose,
            boolean turningPoint,
            boolean open,
            LocalDateTime openedAt,
            LocalDateTime closedAt
    ) {}

    public record CurrentChapterDto(
            int chapterNumber,
            String title,
            String body,
            NarrativePhase phase,
            Double icfAtOpen,
            boolean turningPoint
    ) {}

    // ─── Grafo de identidad ──────────────────────────────────────────────────

    public record GraphResponse(
            Long familyId,
            List<DyadDto> dyads,
            List<MemberRoleDto> systemRoles,
            double cohesionDensity,
            double tensionDensity,
            long conflictiveEdges,
            boolean healthy,
            String summary
    ) {}

    public record DyadDto(
            Long memberAId,
            String memberAName,
            Long memberBId,
            String memberBName,
            String relationshipType,
            DynamicType dynamicType,
            double cohesionScore,
            double tensionScore,
            double communicationScore,
            double healthScore,
            String evolutionTrend,
            String roleA,
            String roleB
    ) {}

    public record MemberRoleDto(
            Long memberId,
            String memberName,
            String systemRole
    ) {}

    public record GraphSummaryDto(
            int totalDyads,
            double cohesionDensity,
            double tensionDensity,
            long conflictiveEdges,
            boolean healthy,
            List<MemberRoleDto> systemRoles
    ) {}

    // ─── Memoria cognitiva ───────────────────────────────────────────────────

    public record MemoryResponse(
            Long familyId,
            List<MemoryDto> episodic,
            List<MemoryDto> semantic,
            List<MemoryDto> procedural
    ) {}

    public record MemoryDto(
            Long id,
            String memoryType,
            String semanticKey,
            String content,
            double importanceScore,
            String sourceType,
            LocalDateTime createdAt
    ) {}

    // ─── Skills ──────────────────────────────────────────────────────────────

    public record SkillDto(
            Long id,
            String skillName,
            String description,
            String dimension,
            double confidence,
            double successRate,
            int reuseCount
    ) {}

    // ─── Reflexión autónoma ──────────────────────────────────────────────────

    public record ReflectionResponse(
            Long familyId,
            String effectivenessLevel,
            int evaluationCount,
            double icfTrend,
            double avgAdherence,
            double reflectionRate,
            String effectivenessSummary,
            String abandonmentLevel,
            List<String> abandonmentSignals,
            int abandonmentScore,
            String lessonLearned,
            String updatedNarrative,
            boolean requiresUrgentAttention,
            LocalDateTime generatedAt
    ) {}
}
