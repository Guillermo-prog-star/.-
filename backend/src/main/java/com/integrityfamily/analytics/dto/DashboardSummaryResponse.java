package com.integrityfamily.analytics.dto;

import com.integrityfamily.domain.RiskLevel;
import lombok.Builder;
import java.math.BigDecimal;
import java.util.Map;
import java.util.List;

/**
 * DashboardSummaryResponse: Contrato final de la Capa de Analítica.
 * Rediseñado con @Builder para sanar la orquestación en AnalyticsService.
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

                // -- GAP 1 RESUELTO: campos que el frontend usaba pero el backend no devolv?a --

                /** Tendencia de riesgo: UP | DOWN | STABLE (compatible con frontend Angular) */
                String riskTrend,

                /** N?mero de crisis activas (frontend usa count, backend ten?a solo booleano) */
                Long activeCrisesCount,

                /** Dimensi?n m?s cr?tica: emociones | comunicacion | habitos | tiempos */
                String criticalDimension,

                // -- GAP 2 RESUELTO: Estado Longitudinal en el dashboard ------------------

                /** Fase evolutiva: inconsciente | reactivo | consciente | pleno */
                String evolutionPhase,

                /** Etapa narrativa: RECONOCIMIENTO | AMOR | ENTREGA */
                String narrativeStage,

                /** Delta ICF vs 30 d?as atr?s (positivo = mejora) */
                Double icfDelta30d,

                /** Crisis en los ?ltimos 30 d?as */
                Integer crisisCount30d,

                /** Deterioraciones consecutivas en bit?cora (=3 = patr?n de deterioro) */
                Integer consecutiveDeteriorations,

                /** true si hay colapso comunicacional activo */
                Boolean communicationCollapseActive,

                /** true si la familia est? en crisis activa (= 48h) */
                Boolean inActiveCrisis,

                // -- GAP 3 RESUELTO: Motor Inferencial Causal en el dashboard -------------

                /** Reglas causales activas del Motor Inferencial (R1-R7) */
                List<String> activeCausalRules,

                /** Explicaciones humanas de cada regla activa */
                List<String> causalExplanations,

                /** true si requiere intervenci?n inmediata (CRITICO o colapso comunicacional) */
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


