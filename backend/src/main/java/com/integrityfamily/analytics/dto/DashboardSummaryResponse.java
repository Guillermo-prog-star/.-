package com.integrityfamily.analytics.dto;
import com.integrityfamily.risk.domain.RiskLevel;
import java.math.BigDecimal;

public record DashboardSummaryResponse(
        Long familyId, String familyName, String familyCode, String currentMilestone,
        Long totalMembers, Long totalEvaluations, Long totalPlans,
        Long totalChecklistItems, Long completedChecklistItems,
        Long totalPlanTasks, Long completedPlanTasks,
        RiskLevel latestRiskLevel, BigDecimal latestGlobalScore,
        Integer latestConsciousnessLevel, String latestConsciousnessLabel,
        Boolean hasCrisis) {}
