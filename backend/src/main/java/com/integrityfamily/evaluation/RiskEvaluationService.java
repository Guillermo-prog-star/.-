package com.integrityfamily.evaluation;

public class RiskEvaluationService {

    private final FamilyEvaluationRepository repository;

    public RiskEvaluationService(FamilyEvaluationRepository repository) {
        this.repository = repository;
    }

    public FamilyRiskResult calculateRisk(
            Long familyId,
            int emotions,
            int communication,
            int habits,
            int timeManagement
    ) {
        if (familyId == null) {
            throw new IllegalArgumentException("familyId is required");
        }

        int icf = (emotions + communication + habits + timeManagement) / 4;

        String riskLevel;
        if (icf < 40) {
            riskLevel = "HIGH";
        } else if (icf < 70) {
            riskLevel = "MEDIUM";
        } else {
            riskLevel = "LOW";
        }

        repository.countEvaluationsByFamilyId(familyId);

        return new FamilyRiskResult(icf, riskLevel);
    }
}
