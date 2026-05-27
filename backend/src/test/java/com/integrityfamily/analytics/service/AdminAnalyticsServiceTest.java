package com.integrityfamily.analytics.service;

import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.EvaluationStatus;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.RiskSnapshotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link AdminAnalyticsService}.
 *
 * Documenta el filtro de familias ALFA-, conteo de sentinels, promedio ICF
 * y distribución de hitos para la fase alpha del producto.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAnalyticsService — Unit Tests")
class AdminAnalyticsServiceTest {

    @Mock FamilyRepository       familyRepository;
    @Mock EvaluationRepository   evaluationRepository;
    @Mock RiskSnapshotRepository riskSnapshotRepository;

    @InjectMocks AdminAnalyticsService service;

    // ───────────────────────────────────────────────────────────────────────
    //  getAlphaPhaseStats()
    // ───────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAlphaPhaseStats()")
    class GetAlphaPhaseStats {

        @Test
        @DisplayName("sin familias en BD → stats completamente en cero / vacío")
        void noFamilies_returnsZeroStats() {
            when(familyRepository.findAll()).thenReturn(List.of());

            AdminAnalyticsService.GlobalStats stats = service.getAlphaPhaseStats();

            assertThat(stats.getTotalFamilies()).isZero();
            assertThat(stats.getAverageIcf()).isZero();
            assertThat(stats.getActiveSentinels()).isZero();
            assertThat(stats.getMilestoneDistribution()).isEmpty();
            verifyNoInteractions(evaluationRepository);
        }

        @Test
        @DisplayName("familias sin código ALFA- → filtradas, retorna stats vacías")
        void onlyNonAlfaFamilies_returnsZeroStats() {
            Family f1 = Family.builder().id(1L).name("Beta").familyCode("BETA-001").build();
            Family f2 = Family.builder().id(2L).name("Sin código").familyCode(null).build();

            when(familyRepository.findAll()).thenReturn(List.of(f1, f2));

            AdminAnalyticsService.GlobalStats stats = service.getAlphaPhaseStats();

            assertThat(stats.getTotalFamilies()).isZero();
            verifyNoInteractions(evaluationRepository);
        }

        @Test
        @DisplayName("familias ALFA- → totalFamilies y milestoneDistribution correctos")
        void alfaFamilies_correctTotalsAndMilestoneDistribution() {
            Family f1 = Family.builder().id(1L).name("Alfa1").familyCode("ALFA-001").currentMilestone("W1").sentinelActive(false).build();
            Family f2 = Family.builder().id(2L).name("Alfa2").familyCode("ALFA-002").currentMilestone("M1").sentinelActive(false).build();
            Family f3 = Family.builder().id(3L).name("Alfa3").familyCode("ALFA-003").currentMilestone("W1").sentinelActive(false).build();

            when(familyRepository.findAll()).thenReturn(List.of(f1, f2, f3));
            // Sin evaluaciones para simplificar
            when(evaluationRepository.findFirstByFamilyIdAndStatusOrderByFinalizedAtDesc(
                    any(), eq(EvaluationStatus.FINALIZED)))
                    .thenReturn(Optional.empty());

            AdminAnalyticsService.GlobalStats stats = service.getAlphaPhaseStats();

            assertThat(stats.getTotalFamilies()).isEqualTo(3);
            assertThat(stats.getMilestoneDistribution())
                    .containsEntry("W1", 2L)
                    .containsEntry("M1", 1L);
        }

        @Test
        @DisplayName("familias ALFA- con sentinel activo → activeSentinels cuenta correctamente")
        void alfaFamilies_sentinelCounting() {
            Family f1 = Family.builder().id(1L).familyCode("ALFA-001").sentinelActive(true).currentMilestone("W1").build();
            Family f2 = Family.builder().id(2L).familyCode("ALFA-002").sentinelActive(false).currentMilestone("W1").build();
            Family f3 = Family.builder().id(3L).familyCode("ALFA-003").sentinelActive(true).currentMilestone("M1").build();
            Family f4 = Family.builder().id(4L).familyCode("ALFA-004").sentinelActive(null).currentMilestone("M1").build();

            when(familyRepository.findAll()).thenReturn(List.of(f1, f2, f3, f4));
            when(evaluationRepository.findFirstByFamilyIdAndStatusOrderByFinalizedAtDesc(
                    any(), eq(EvaluationStatus.FINALIZED)))
                    .thenReturn(Optional.empty());

            AdminAnalyticsService.GlobalStats stats = service.getAlphaPhaseStats();

            // Solo f1 y f3 tienen sentinelActive=true
            assertThat(stats.getActiveSentinels()).isEqualTo(2L);
        }

        @Test
        @DisplayName("familia sin currentMilestone → agrupada bajo 'SIN_HITO'")
        void alfaFamilies_nullMilestoneGroupedAsSinHito() {
            Family f1 = Family.builder().id(1L).familyCode("ALFA-001").sentinelActive(false).currentMilestone(null).build();

            when(familyRepository.findAll()).thenReturn(List.of(f1));
            when(evaluationRepository.findFirstByFamilyIdAndStatusOrderByFinalizedAtDesc(
                    any(), eq(EvaluationStatus.FINALIZED)))
                    .thenReturn(Optional.empty());

            AdminAnalyticsService.GlobalStats stats = service.getAlphaPhaseStats();

            assertThat(stats.getMilestoneDistribution()).containsEntry("SIN_HITO", 1L);
        }

        @Test
        @DisplayName("evaluación con ICF disponible → averageIcf calculado correctamente")
        void alfaFamilies_icfAverageFromEvaluations() {
            Family f1 = Family.builder().id(1L).familyCode("ALFA-001").sentinelActive(false).currentMilestone("W1").build();
            Family f2 = Family.builder().id(2L).familyCode("ALFA-002").sentinelActive(false).currentMilestone("W1").build();

            Evaluation eval1 = Evaluation.builder().id(1L).icf(60.0).status(EvaluationStatus.FINALIZED).build();
            Evaluation eval2 = Evaluation.builder().id(2L).icf(80.0).status(EvaluationStatus.FINALIZED).build();

            when(familyRepository.findAll()).thenReturn(List.of(f1, f2));
            when(evaluationRepository.findFirstByFamilyIdAndStatusOrderByFinalizedAtDesc(
                    eq(1L), eq(EvaluationStatus.FINALIZED)))
                    .thenReturn(Optional.of(eval1));
            when(evaluationRepository.findFirstByFamilyIdAndStatusOrderByFinalizedAtDesc(
                    eq(2L), eq(EvaluationStatus.FINALIZED)))
                    .thenReturn(Optional.of(eval2));

            AdminAnalyticsService.GlobalStats stats = service.getAlphaPhaseStats();

            // Promedio de 60 y 80 = 70
            assertThat(stats.getAverageIcf()).isEqualTo(70.0);
        }
    }
}
