package com.integrityfamily.analytics.dto;

import com.integrityfamily.domain.RiskLevel;
import lombok.Builder;
import java.math.BigDecimal;
import java.util.Map;
import java.util.List;

/**
 * DashboardSummaryResponse: Contrato final de la Capa de AnalÃƒÂ­tica.
 * RediseÃƒÂ±ado con @Builder para sanar la orquestaciÃƒÂ³n en AnalyticsService.
 */
@Builder
public record DashboardSummaryResponse(
                Long familyId,
                String familyName,
                String familyCode,
                String currentMilestone,
                Long totalMembers,
                Long totalEvaluations,
                Long totalPlans,
                Long totalChecklistItems,
                Long completedChecklistItems,
                Long totalPlanTasks,
                Long completedPlanTasks,
                RiskLevel latestRiskLevel,
                BigDecimal latestGlobalScore,
                Integer latestConsciousnessLevel,
                String latestConsciousnessLabel,
                Boolean hasCrisis,
                Boolean isSentinelActive,
                Double pillarProgress,
                Double awarenessGrowth,
                Map<String, Double> dimensionScores,
                List<SuggestedActionDto> suggestedActions,
                String aiRecommendation,
                String planAiReport,
                Long openLogbookEntriesCount,
                String latestFamilyAgreement,

                // ── GAP 1 RESUELTO: campos que el frontend usaba pero el backend no devolvía ──

                /** Tendencia de riesgo: UP | DOWN | STABLE (compatible con frontend Angular) */
                String riskTrend,

                /** Número de crisis activas (frontend usa count, backend tenía solo booleano) */
                Long activeCrisesCount,

                /** Dimensión más crítica: emociones | comunicacion | habitos | tiempos */
                String criticalDimension,

                // ── GAP 2 RESUELTO: Estado Longitudinal en el dashboard ──────────────────

                /** Fase evolutiva: inconsciente | reactivo | consciente | pleno */
                String evolutionPhase,

                /** Etapa narrativa: RECONOCIMIENTO | AMOR | ENTREGA */
                String narrativeStage,

                /** Delta ICF vs 30 días atrás (positivo = mejora) */
                Double icfDelta30d,

                /** Crisis en los últimos 30 días */
                Integer crisisCount30d,

                /** Deterioraciones consecutivas en bitácora (≥3 = patrón de deterioro) */
                Integer consecutiveDeteriorations,

                /** true si hay colapso comunicacional activo */
                Boolean communicationCollapseActive,

                /** true si la familia está en crisis activa (≤ 48h) */
                Boolean inActiveCrisis,

                // ── GAP 3 RESUELTO: Motor Inferencial Causal en el dashboard ─────────────

                /** Reglas causales activas del Motor Inferencial (R1-R7) */
                List<String> activeCausalRules,

                /** Explicaciones humanas de cada regla activa */
                List<String> causalExplanations,

                /** true si requiere intervención inmediata (CRITICO o colapso comunicacional) */
                Boolean requiresImmediateIntervention
) {
    /** Compatibilidad: convierte tendencia longitudinal al formato esperado por el frontend */
    public String riskTrendForFrontend() {
        if (riskTrend == null) return "STABLE";
        return switch (riskTrend) {
            case "IMPROVING"     -> "UP";
            case "DETERIORATING" -> "DOWN";
            case "CRITICAL"      -> "DOWN";
            default               -> "STABLE";
        };
    }
}


