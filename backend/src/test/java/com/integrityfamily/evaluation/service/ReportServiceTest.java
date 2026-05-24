package com.integrityfamily.evaluation.service;

import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.EvaluationDimensionScore;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.FamilySummary;
import com.integrityfamily.dto.TerritorialEvolutionReportDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de ReportService (evaluation package).
 *
 * Cubre: getTerritorialReport() — familia no encontrada (RuntimeException),
 * lista de evaluaciones vacía, filtrado de evaluaciones sin milestoneKey,
 * mapeo de dimensionScores (emociones, comunicacion, habitos, tiempos),
 * y encabezado del DTO con datos de la familia.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReportService (evaluation) — Unit Tests")
class ReportServiceTest {

    @Mock EvaluationRepository evaluationRepository;
    @Mock FamilyRepository     familyRepository;

    @InjectMocks
    ReportService reportService;

    private FamilySummary familySummary;

    @BeforeEach
    void setUp() {
        familySummary = mock(FamilySummary.class);
        lenient().when(familySummary.getId()).thenReturn(1L);
        lenient().when(familySummary.getName()).thenReturn("Familia López");
        lenient().when(familySummary.getFamilyCode()).thenReturn("IF-2026-TEST");
        lenient().when(familySummary.getCountryCode()).thenReturn("COL");
        lenient().when(familySummary.getDepartmentCode()).thenReturn("05");
        lenient().when(familySummary.getMunicipio()).thenReturn("Medellín");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Familia no encontrada
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("familia no encontrada")
    class FamilyNotFound {

        @Test
        @DisplayName("familyId inexistente → RuntimeException con mensaje 'Familia no encontrada'")
        void shouldThrow_whenFamilyNotFound() {
            when(familyRepository.findProjectedById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reportService.getTerritorialReport(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Familia no encontrada");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Sin evaluaciones
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("sin evaluaciones")
    class NoEvaluations {

        @Test
        @DisplayName("evaluaciones vacías → DTO con milestones vacío")
        void shouldReturnEmptyMilestones_whenNoEvaluations() {
            when(familyRepository.findProjectedById(1L)).thenReturn(Optional.of(familySummary));
            when(evaluationRepository.findWithScoresByFamilyId(1L)).thenReturn(List.of());

            TerritorialEvolutionReportDto report = reportService.getTerritorialReport(1L);

            assertThat(report.milestones()).isEmpty();
        }

        @Test
        @DisplayName("DTO contiene el familyCode y nombre de la familia")
        void shouldPopulateReportHeader() {
            when(familyRepository.findProjectedById(1L)).thenReturn(Optional.of(familySummary));
            when(evaluationRepository.findWithScoresByFamilyId(1L)).thenReturn(List.of());

            TerritorialEvolutionReportDto report = reportService.getTerritorialReport(1L);

            assertThat(report.familyCode()).isEqualTo("IF-2026-TEST");
            assertThat(report.referenceName()).isEqualTo("Familia López");
            assertThat(report.familyId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("DTO contiene datos territoriales de la familia")
        void shouldPopulateTerritorialData() {
            when(familyRepository.findProjectedById(1L)).thenReturn(Optional.of(familySummary));
            when(evaluationRepository.findWithScoresByFamilyId(1L)).thenReturn(List.of());

            TerritorialEvolutionReportDto report = reportService.getTerritorialReport(1L);

            assertThat(report.countryCode()).isEqualTo("COL");
            assertThat(report.departmentCode()).isEqualTo("05");
            assertThat(report.municipality()).isEqualTo("Medellín");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Filtrado por milestoneKey
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("filtrado por milestoneKey")
    class MilestoneKeyFilter {

        @Test
        @DisplayName("evaluación sin milestoneKey → NO aparece en el reporte")
        void shouldFilter_evaluationsWithoutMilestoneKey() {
            Family family = Family.builder().id(1L).build();
            Evaluation evalSinHito = Evaluation.builder()
                    .id(1L).family(family).milestoneKey(null).build();

            when(familyRepository.findProjectedById(1L)).thenReturn(Optional.of(familySummary));
            when(evaluationRepository.findWithScoresByFamilyId(1L)).thenReturn(List.of(evalSinHito));

            TerritorialEvolutionReportDto report = reportService.getTerritorialReport(1L);

            assertThat(report.milestones()).isEmpty();
        }

        @Test
        @DisplayName("evaluación con milestoneKey → SÍ aparece en el reporte")
        void shouldInclude_evaluationsWithMilestoneKey() {
            Family family = Family.builder().id(1L).build();
            Evaluation evalConHito = Evaluation.builder()
                    .id(2L).family(family)
                    .milestoneKey("M6")
                    .icf(75.0)
                    .riskLevel("MODERATE")
                    .criticalDimension("COMUNICACION")
                    .finalizedAt(LocalDateTime.of(2026, 1, 15, 10, 0))
                    .build();

            when(familyRepository.findProjectedById(1L)).thenReturn(Optional.of(familySummary));
            when(evaluationRepository.findWithScoresByFamilyId(1L)).thenReturn(List.of(evalConHito));

            TerritorialEvolutionReportDto report = reportService.getTerritorialReport(1L);

            assertThat(report.milestones()).hasSize(1);
            assertThat(report.milestones().get(0).hitoKey()).isEqualTo("M6");
            assertThat(report.milestones().get(0).icfPercent()).isEqualTo(75.0);
            assertThat(report.milestones().get(0).riskLevel()).isEqualTo("MODERATE");
            assertThat(report.milestones().get(0).criticalDimension()).isEqualTo("COMUNICACION");
        }

        @Test
        @DisplayName("mezcla con/sin milestoneKey → solo los que tienen hito pasan el filtro")
        void shouldFilterMixedEvaluations() {
            Family family = Family.builder().id(1L).build();
            Evaluation sinHito = Evaluation.builder()
                    .id(1L).family(family).milestoneKey(null).build();
            Evaluation conHito = Evaluation.builder()
                    .id(2L).family(family).milestoneKey("W1").icf(60.0).build();

            when(familyRepository.findProjectedById(1L)).thenReturn(Optional.of(familySummary));
            when(evaluationRepository.findWithScoresByFamilyId(1L)).thenReturn(List.of(sinHito, conHito));

            TerritorialEvolutionReportDto report = reportService.getTerritorialReport(1L);

            assertThat(report.milestones()).hasSize(1);
            assertThat(report.milestones().get(0).hitoKey()).isEqualTo("W1");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Mapeo de dimensionScores
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("mapeo de dimensionScores")
    class DimensionScoresMapping {

        private Evaluation buildEvalWithScores(
                Double emoScore, Double comScore, Double habScore, Double timScore) {

            Family family = Family.builder().id(1L).build();
            Evaluation eval = Evaluation.builder()
                    .id(10L).family(family).milestoneKey("M12")
                    .icf(80.0)
                    .build();

            if (emoScore != null) {
                eval.getDimensionScores().add(
                        EvaluationDimensionScore.builder()
                                .dimensionName("emociones").score(emoScore).evaluation(eval).build());
            }
            if (comScore != null) {
                eval.getDimensionScores().add(
                        EvaluationDimensionScore.builder()
                                .dimensionName("comunicacion").score(comScore).evaluation(eval).build());
            }
            if (habScore != null) {
                eval.getDimensionScores().add(
                        EvaluationDimensionScore.builder()
                                .dimensionName("habitos").score(habScore).evaluation(eval).build());
            }
            if (timScore != null) {
                eval.getDimensionScores().add(
                        EvaluationDimensionScore.builder()
                                .dimensionName("tiempos").score(timScore).evaluation(eval).build());
            }
            return eval;
        }

        @Test
        @DisplayName("dimensionScores con 4 dimensiones → se mapean correctamente al DTO")
        void shouldMapAllFourDimensions() {
            Evaluation eval = buildEvalWithScores(85.0, 70.0, 90.0, 65.0);

            when(familyRepository.findProjectedById(1L)).thenReturn(Optional.of(familySummary));
            when(evaluationRepository.findWithScoresByFamilyId(1L)).thenReturn(List.of(eval));

            TerritorialEvolutionReportDto report = reportService.getTerritorialReport(1L);

            TerritorialEvolutionReportDto.DimensionScoresDto scores = report.milestones().get(0).scores();
            assertThat(scores.emotions()).isEqualTo(85.0);
            assertThat(scores.communication()).isEqualTo(70.0);
            assertThat(scores.habits()).isEqualTo(90.0);
            assertThat(scores.time()).isEqualTo(65.0);
        }

        @Test
        @DisplayName("dimensionScores vacíos → scores del DTO son null")
        void shouldReturnNullScores_whenNoDimensionData() {
            Evaluation eval = buildEvalWithScores(null, null, null, null);

            when(familyRepository.findProjectedById(1L)).thenReturn(Optional.of(familySummary));
            when(evaluationRepository.findWithScoresByFamilyId(1L)).thenReturn(List.of(eval));

            TerritorialEvolutionReportDto report = reportService.getTerritorialReport(1L);

            TerritorialEvolutionReportDto.DimensionScoresDto scores = report.milestones().get(0).scores();
            assertThat(scores.emotions()).isNull();
            assertThat(scores.communication()).isNull();
            assertThat(scores.habits()).isNull();
            assertThat(scores.time()).isNull();
        }

        @Test
        @DisplayName("checklist simulado → totalItems=10, completionPercent=80, trendVsPrev=UP")
        void shouldReturnSimulatedChecklist() {
            Evaluation eval = buildEvalWithScores(null, null, null, null);

            when(familyRepository.findProjectedById(1L)).thenReturn(Optional.of(familySummary));
            when(evaluationRepository.findWithScoresByFamilyId(1L)).thenReturn(List.of(eval));

            TerritorialEvolutionReportDto report = reportService.getTerritorialReport(1L);

            TerritorialEvolutionReportDto.ChecklistSummaryDto checklist =
                    report.milestones().get(0).checklist();
            assertThat(checklist.totalItems()).isEqualTo(10);
            assertThat(checklist.completionPercent()).isEqualTo(80.0);
            assertThat(checklist.trendVsPrev()).isEqualTo("UP");
        }
    }
}
