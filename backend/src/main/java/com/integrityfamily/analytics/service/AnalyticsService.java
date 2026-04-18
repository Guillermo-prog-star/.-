package com.integrityfamily.analytics.service;

import com.integrityfamily.analytics.dto.DashboardSummaryResponse;
import com.integrityfamily.checklist.repository.ChecklistItemRepository;
import com.integrityfamily.evaluation.repository.EvaluationRepository;
import com.integrityfamily.family.domain.Family;
import com.integrityfamily.family.repository.FamilyRepository;
import com.integrityfamily.member.repository.MemberRepository;
import com.integrityfamily.plan.repository.PlanRepository;
import com.integrityfamily.plan.domain.Plan;
import com.integrityfamily.risk.domain.RiskSnapshot;
import com.integrityfamily.risk.repository.RiskSnapshotRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final FamilyRepository familyRepository;
    private final MemberRepository memberRepository;
    private final EvaluationRepository evaluationRepository;
    private final PlanRepository planRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final RiskSnapshotRepository riskSnapshotRepository;

    public AnalyticsService(FamilyRepository familyRepository,
                            MemberRepository memberRepository,
                            EvaluationRepository evaluationRepository,
                            PlanRepository planRepository,
                            ChecklistItemRepository checklistItemRepository,
                            RiskSnapshotRepository riskSnapshotRepository) {
        this.familyRepository = familyRepository;
        this.memberRepository = memberRepository;
        this.evaluationRepository = evaluationRepository;
        this.planRepository = planRepository;
        this.checklistItemRepository = checklistItemRepository;
        this.riskSnapshotRepository = riskSnapshotRepository;
    }

    public Map<String, Object> getFamilySummary(Long familyId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new RuntimeException("Familia no encontrada: " + familyId));

        long totalMembers = memberRepository.findByFamilyId(familyId).size();
        long totalEvaluations = evaluationRepository.findByFamilyIdOrderByStartedAtDesc(familyId).size();
        List<Plan> plans = planRepository.findByFamilyIdOrderByCreatedAtDesc(familyId);
        
        long totalPlanTasks = plans.stream().mapToLong(p -> p.getTasks().size()).sum();
        long completedPlanTasks = plans.stream().flatMap(p -> p.getTasks().stream())
                                        .filter(t -> Boolean.TRUE.equals(t.getCompleted())).count();

        List<RiskSnapshot> riskHistory = riskSnapshotRepository.findByFamilyIdOrderByCreatedAtDesc(familyId);
        
        Double latestIcf = 0.0;
        String latestRisk = "PENDIENTE";
        Integer consciousnessLevel = 1;
        String consciousnessLabel = "Inconsciente";
        Boolean hasCrisis = false;
        Double baselineScore = 0.0;

        if (!riskHistory.isEmpty()) {
            RiskSnapshot latest = riskHistory.get(0);
            latestIcf = latest.getIcf();
            latestRisk = latest.getRiskLevel();
            consciousnessLevel = latest.getConsciousnessLevel();
            consciousnessLabel = latest.getConsciousnessLabel();
            hasCrisis = latest.getHasCrisis();
            
            baselineScore = riskHistory.get(riskHistory.size() - 1).getIcf();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("familyId", familyId);
        data.put("familyName", family.getName());
        data.put("familyCode", family.getFamilyCode());
        data.put("currentMilestone", family.getCurrentMilestone());
        data.put("latestRiskLevel", latestRisk);
        data.put("latestGlobalScore", latestIcf);
        data.put("latestConsciousnessLevel", consciousnessLevel);
        data.put("latestConsciousnessLabel", consciousnessLabel);
        data.put("hasCrisis", hasCrisis);
        data.put("awarenessGrowth", (latestIcf - baselineScore));
        data.put("totalMembers", totalMembers);
        data.put("totalEvaluations", totalEvaluations);
        data.put("totalPlans", (long) plans.size());
        data.put("totalPlanTasks", totalPlanTasks);
        data.put("completedPlanTasks", completedPlanTasks);
        data.put("nextEvaluationAt", family.getNextEvaluationAt());

        data.put("riskHistory", riskHistory.stream().limit(10).map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("createdAt", s.getCreatedAt());
            m.put("score", s.getIcf());
            m.put("level", s.getRiskLevel());
            m.put("consciousnessLevel", s.getConsciousnessLevel());
            return m;
        }).collect(Collectors.toList()));

        return data;
    }

    public DashboardSummaryResponse getFamilySummaryTyped(Long familyId) {
        Map<String, Object> map = getFamilySummary(familyId);
        return new DashboardSummaryResponse(
            (Long) map.get("familyId"),
            (String) map.get("familyName"),
            (String) map.get("familyCode"),
            (String) map.get("currentMilestone"),
            (Long) map.get("totalMembers"),
            (Long) map.get("totalEvaluations"),
            (Long) map.get("totalPlans"),
            0L, 0L, // Checklist mapping omitted for brevity or handled in separate call
            (Long) map.get("totalPlanTasks"),
            (Long) map.get("completedPlanTasks"),
            com.integrityfamily.risk.domain.RiskLevel.valueOf((String) map.get("latestRiskLevel")),
            BigDecimal.valueOf((Double) map.get("latestGlobalScore")),
            (Integer) map.get("latestConsciousnessLevel"),
            (String) map.get("latestConsciousnessLabel"),
            (Boolean) map.get("hasCrisis")
        );
    }
}