package com.integrityfamily.simulation.service;

import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.evaluation.service.EvaluationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SentinelSimulationService")
class SentinelSimulationServiceTest {

    @Mock FamilyRepository  familyRepository;
    @Mock EvaluationService evaluationService;

    @InjectMocks SentinelSimulationService service;

    @Test
    @DisplayName("ejecuta dos inyecciones de resultado cuando la familia existe y retorna exito")
    void runBurstSimulation_familyExists_callsTwoSimulationsAndReturnsSuccess() {
        when(familyRepository.existsById(42L)).thenReturn(true);

        String result = service.runBurstSimulation(42L);

        verify(evaluationService).processSimulatedResult(42L, 5.0, false);
        verify(evaluationService).processSimulatedResult(42L, 1.0, true);
        assertThat(result).contains("completada");
    }

    @Test
    @DisplayName("retorna mensaje de error SDD cuando la familia no existe")
    void runBurstSimulation_familyNotFound_returnsErrorMessage() {
        when(familyRepository.existsById(99L)).thenReturn(false);

        String result = service.runBurstSimulation(99L);

        assertThat(result).contains("ERROR");
    }
}
