package com.integrityfamily.evaluation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RiskEvaluationService")
class RiskEvaluationServiceTest {

    @Mock FamilyEvaluationRepository repository;

    RiskEvaluationService service() {
        return new RiskEvaluationService(repository);
    }

    // ── Validación de parámetros ──────────────────────────────────────────────

    @Test
    @DisplayName("familyId null → IllegalArgumentException")
    void calculateRisk_nullFamilyId_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service().calculateRisk(null, 50, 50, 50, 50))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("familyId");
    }

    // ── ICF — cálculo (división entera de 4 scores) ──────────────────────────

    @Test
    @DisplayName("scores iguales → ICF = ese valor exacto")
    void calculateRisk_equalScores_icfIsExact() {
        when(repository.countEvaluationsByFamilyId(anyLong())).thenReturn(2);

        FamilyRiskResult result = service().calculateRisk(1L, 60, 60, 60, 60);

        assertThat(result.icf()).isEqualTo(60);
    }

    @Test
    @DisplayName("scores diferentes → ICF es su media entera")
    void calculateRisk_mixedScores_icfIsIntegerAverage() {
        when(repository.countEvaluationsByFamilyId(anyLong())).thenReturn(1);
        // (80 + 60 + 70 + 50) / 4 = 260 / 4 = 65
        FamilyRiskResult result = service().calculateRisk(2L, 80, 60, 70, 50);

        assertThat(result.icf()).isEqualTo(65);
    }

    // ── Umbrales de riesgo ────────────────────────────────────────────────────

    @Test
    @DisplayName("ICF < 40 → riskLevel 'HIGH'")
    void calculateRisk_icfBelow40_isHigh() {
        when(repository.countEvaluationsByFamilyId(anyLong())).thenReturn(1);
        // (36 + 36 + 36 + 36) / 4 = 36 → HIGH
        FamilyRiskResult result = service().calculateRisk(3L, 36, 36, 36, 36);

        assertThat(result.icf()).isEqualTo(36);
        assertThat(result.riskLevel()).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("ICF == 40 → riskLevel 'MEDIUM' (umbral inclusivo superior del HIGH)")
    void calculateRisk_icfExactly40_isMedium() {
        when(repository.countEvaluationsByFamilyId(anyLong())).thenReturn(1);
        // (40 + 40 + 40 + 40) / 4 = 40 → 40 < 40 es false → MEDIUM
        FamilyRiskResult result = service().calculateRisk(4L, 40, 40, 40, 40);

        assertThat(result.icf()).isEqualTo(40);
        assertThat(result.riskLevel()).isEqualTo("MEDIUM");
    }

    @Test
    @DisplayName("ICF == 69 → riskLevel 'MEDIUM'")
    void calculateRisk_icf69_isMedium() {
        when(repository.countEvaluationsByFamilyId(anyLong())).thenReturn(1);
        // (68 + 70 + 68 + 70) / 4 = 276 / 4 = 69 → MEDIUM
        FamilyRiskResult result = service().calculateRisk(5L, 68, 70, 68, 70);

        assertThat(result.icf()).isEqualTo(69);
        assertThat(result.riskLevel()).isEqualTo("MEDIUM");
    }

    @Test
    @DisplayName("ICF == 70 → riskLevel 'LOW' (umbral inclusivo del LOW)")
    void calculateRisk_icfExactly70_isLow() {
        when(repository.countEvaluationsByFamilyId(anyLong())).thenReturn(1);
        // (70 + 70 + 70 + 70) / 4 = 70 → 70 < 70 es false → LOW
        FamilyRiskResult result = service().calculateRisk(6L, 70, 70, 70, 70);

        assertThat(result.icf()).isEqualTo(70);
        assertThat(result.riskLevel()).isEqualTo("LOW");
    }

    // ── Interacción con el repositorio ────────────────────────────────────────

    @Test
    @DisplayName("siempre llama a countEvaluationsByFamilyId con el familyId correcto")
    void calculateRisk_alwaysCallsRepositoryCount() {
        when(repository.countEvaluationsByFamilyId(99L)).thenReturn(5);

        service().calculateRisk(99L, 50, 50, 50, 50);

        verify(repository).countEvaluationsByFamilyId(99L);
    }
}
