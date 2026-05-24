package com.integrityfamily.ai.dto;

import lombok.Builder;
import java.util.List;

public class CopilotDtos {

    @Builder
    public record CompactFamilyContext(
        Long familyId,
        String riskLevel,
        String criticalDimension,
        String trend,
        Double adherence,
        Integer inactiveDays,
        List<String> recentLearnings,
        List<String> alerts
    ) {}

    @Builder
    public record StructuredAiInferenceResponse(
        String summary,
        String priority, // HIGH, MEDIUM, LOW
        List<String> recommendedActions,
        String containmentSuggestion,
        Integer followUpDays
    ) {}

    public record CopilotInferRequest(
        Long familyId,
        String triggerEvent // NEW_ALERT, REGRESSION, INACTIVITY
    ) {}

    /**
     * SDD Fases 1–5: Enriquecimiento cognitivo para el prompt del copiloto.
     * Compacta la memoria, narrativa, grafo e identidad de la familia en un bloque
     * que Claude puede leer sin superar el límite de contexto.
     */
    @Builder
    public record CognitiveEnrichment(
        // Identidad
        String evolutionStage,           // INITIAL / RECOGNITION / ADJUSTMENT / CONSOLIDATION / AUTONOMOUS
        double adaptabilityIndex,        // 0.0–1.0
        String communicationStyle,       // RESERVED / EXPRESSIVE / CONFLICTIVE / COLLABORATIVE
        String conflictStyle,            // AVOIDANT / NEGOTIATING / EXPLOSIVE / etc.
        String identityNarrative,        // Párrafo narrativo compacto (max 300 chars)

        // Narrativa
        String currentChapterTitle,      // Ej: "Capítulo 3: El Cambio en Marcha"
        String currentChapterPhase,      // AWAKENING / DISCOVERY / TRANSITION / etc.
        boolean turningPointInLastEval,  // ¿Hubo punto de inflexión en la última evaluación?
        int totalChapters,

        // Grafo relacional
        int totalDyads,
        double graphCohesion,            // 0–100
        double graphTension,             // 0–100
        long conflictiveDyads,
        List<String> systemRoles,        // ["María: ANCHOR", "Pedro: ESCALATOR"]

        // Memoria semántica
        List<String> semanticPatterns,   // ["evaluation-trend-pattern: IMPROVING (avg ICF 68.5)"]
        String lastLessonLearned,        // Última lección aprendida por el motor de reflexión

        // Skills activas
        List<String> activeSkills,       // ["micro_missions_high_stress", "short_dialogues_low_communication"]

        // Riesgo de abandono
        String abandonmentRisk           // LOW / MODERATE / HIGH / CRITICAL
    ) {}
}
