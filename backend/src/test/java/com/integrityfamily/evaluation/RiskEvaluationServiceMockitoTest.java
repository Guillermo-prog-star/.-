package com.integrityfamily.evaluation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class RiskEvaluationServiceMockitoTest {

    @Test
    void shouldCalculateMediumRiskAndCallRepositoryWithMockito() {
        FamilyEvaluationRepository repository = mock(FamilyEvaluationRepository.class);

        when(repository.countEvaluationsByFamilyId(20L)).thenReturn(3);

        RiskEvaluationService service = new RiskEvaluationService(repository);

        FamilyRiskResult result = service.calculateRisk(
                20L,
                60,
                55,
                70,
                65
        );

        assertEquals(62, result.icf());
        assertEquals("MEDIUM", result.riskLevel());

        verify(repository, times(1)).countEvaluationsByFamilyId(20L);
    }
}
