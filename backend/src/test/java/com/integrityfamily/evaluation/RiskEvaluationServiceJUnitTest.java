package com.integrityfamily.evaluation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RiskEvaluationServiceJUnitTest {

    @Test
    void shouldCalculateHighRiskWithJUnitOnly() {
        FamilyEvaluationRepository fakeRepository = familyId -> 1;

        RiskEvaluationService service = new RiskEvaluationService(fakeRepository);

        FamilyRiskResult result = service.calculateRisk(
                10L,
                30,
                35,
                40,
                25
        );

        assertEquals(32, result.icf());
        assertEquals("HIGH", result.riskLevel());
    }

    @Test
    void shouldThrowExceptionWhenFamilyIdIsNull() {
        FamilyEvaluationRepository fakeRepository = familyId -> 0;

        RiskEvaluationService service = new RiskEvaluationService(fakeRepository);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.calculateRisk(null, 80, 80, 80, 80)
        );
    }
}
