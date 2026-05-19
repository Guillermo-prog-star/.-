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
                String latestFamilyAgreement) {
}


