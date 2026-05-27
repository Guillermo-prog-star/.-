package com.integrityfamily.analytics.service;

import com.integrityfamily.analytics.dto.ConvivenceAnalyticsDto.*;
import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link ConvivenceAnalyticsService}.
 *
 * Documenta:
 *   - getOperativeDashboard: FAMILY_NOT_FOUND, cálculo ICF básico, alertas operativas
 *   - getMetricsTimeline: mapeo de snapshots a DTOs
 *
 * No levanta contexto Spring — Mockito strict stubs.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConvivenceAnalyticsService — Unit Tests")
class ConvivenceAnalyticsServiceTest {

    @Mock FamilyRepository               familyRepository;
    @Mock EvaluationRepository           evaluationRepository;
    @Mock PlanTaskRepository             planTaskRepository;
    @Mock TaskEvidenceRepository         taskEvidenceRepository;
    @Mock ReflectionRepository           reflectionRepository;
    @Mock FamilyMetricsSnapshotRepository snapshotRepository;

    @InjectMocks ConvivenceAnalyticsService service;

    private Family family;

    @BeforeEach
    void setUp() {
        family = Family.builder()
                .id(1L).name("Los García")
                .members(List.of())
                .build();
    }

    // ─── Helper: configura el mínimo de stubs para que getOperativeDashboard no falle ──

    private void stubMinimalDashboard(Family f, List<Evaluation> evals,
                                      List<PlanTask> tasks, List<TaskEvidence> evidences,
                                      List<FamilyMetricsSnapshot> history) {
        when(familyRepository.findById(f.getId())).thenReturn(Optional.of(f));
        when(evaluationRepository.findWithScoresByFamilyId(f.getId())).thenReturn(evals);
        when(planTaskRepository.findAll()).thenReturn(tasks);
        when(taskEvidenceRepository.findAll()).thenReturn(evidences);
        when(reflectionRepository.findByFamilyId(f.getId())).thenReturn(List.of());
        when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(f.getId())).thenReturn(history);
        when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ───────────────────────────────────────────────────────────────────────
    //  getOperativeDashboard()
    // ───────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getOperativeDashboard()")
    class GetOperativeDashboard {

        @Test
        @DisplayName("familia no encontrada → BusinessException FAMILY_NOT_FOUND 404")
        void familyNotFound() {
            when(familyRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getOperativeDashboard(99L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo("FAMILY_NOT_FOUND");
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("sin evaluaciones ni tareas → ICF usa valor default (70.0), adherencia 100%")
        void noDependencies_defaultsApplied() {
            stubMinimalDashboard(family, List.of(), List.of(), List.of(), List.of());

            OperativeDashboardResponse result = service.getOperativeDashboard(1L);

            assertThat(result.familyId()).isEqualTo(1L);
            assertThat(result.convivenceIndex()).isGreaterThan(0);
            // Sin evaluaciones → ICF default 70, adherencia 100% → índice esperado alto
            assertThat(result.adherenceRate()).isEqualTo(100.0);
            assertThat(result.adherenceStatus()).isEqualTo("Alta");
            verify(snapshotRepository).save(any(FamilyMetricsSnapshot.class));
        }

        @Test
        @DisplayName("alerta ALERTA_CRITICAL_COMMUNICATION cuando communicationScore < 25")
        void alert_criticalCommunication() {
            // Construir evaluación con dimensionScore de comunicacion=10
            EvaluationDimensionScore commScore = new EvaluationDimensionScore();
            commScore.setDimensionName("comunicacion");
            commScore.setScore(10.0);

            Evaluation eval = Evaluation.builder()
                    .id(1L).family(family)
                    .status(EvaluationStatus.FINALIZED)
                    .icf(65.0).riskLevel("MODERADO")
                    .dimensionScores(List.of(commScore))
                    .finalizedAt(LocalDateTime.now())
                    .build();

            stubMinimalDashboard(family, List.of(eval), List.of(), List.of(), List.of());

            OperativeDashboardResponse result = service.getOperativeDashboard(1L);

            assertThat(result.activeAlerts())
                    .extracting(OperativeAlertDto::alertCode)
                    .contains("ALERTA_CRITICAL_COMMUNICATION");
            assertThat(result.activeAlerts())
                    .filteredOn(a -> "ALERTA_CRITICAL_COMMUNICATION".equals(a.alertCode()))
                    .extracting(OperativeAlertDto::severity)
                    .containsOnly("CRITICAL");
        }

        @Test
        @DisplayName("alerta ALERTA_LOW_ADHERENCE cuando adherencia < 40%")
        void alert_lowAdherence() {
            // 10 tareas de la familia, solo 3 completadas → 30% < 40%
            PlanTask completedTask   = PlanTask.builder().id(1L).completed(true)
                    .plan(mockPlanForFamily(1L)).build();
            PlanTask incompletedTask = PlanTask.builder().id(2L).completed(false)
                    .plan(mockPlanForFamily(1L)).build();

            List<PlanTask> tasks = List.of(
                    completedTask, completedTask, completedTask,
                    incompletedTask, incompletedTask, incompletedTask,
                    incompletedTask, incompletedTask, incompletedTask, incompletedTask
            );

            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(evaluationRepository.findWithScoresByFamilyId(1L)).thenReturn(List.of());
            when(planTaskRepository.findAll()).thenReturn(tasks);
            when(taskEvidenceRepository.findAll()).thenReturn(List.of());
            when(reflectionRepository.findByFamilyId(1L)).thenReturn(List.of());
            when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(1L)).thenReturn(List.of());
            when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            OperativeDashboardResponse result = service.getOperativeDashboard(1L);

            assertThat(result.adherenceRate()).isLessThan(40.0);
            assertThat(result.activeAlerts())
                    .extracting(OperativeAlertDto::alertCode)
                    .contains("ALERTA_LOW_ADHERENCE");
        }

        @Test
        @DisplayName("alerta ALERTA_INACTIVITY cuando no hay evidencias recientes (14 días)")
        void alert_inactivity_noEvidences() {
            stubMinimalDashboard(family, List.of(), List.of(), List.of(), List.of());

            OperativeDashboardResponse result = service.getOperativeDashboard(1L);

            assertThat(result.activeAlerts())
                    .extracting(OperativeAlertDto::alertCode)
                    .contains("ALERTA_INACTIVITY");
        }

        @Test
        @DisplayName("sin alertas activas cuando comunicación, adherencia y evidencias son saludables")
        void noAlerts_whenHealthy() {
            EvaluationDimensionScore commScore = new EvaluationDimensionScore();
            commScore.setDimensionName("comunicacion");
            commScore.setScore(75.0);

            Evaluation eval = Evaluation.builder()
                    .id(1L).family(family).status(EvaluationStatus.FINALIZED)
                    .icf(80.0).riskLevel("BAJO")
                    .dimensionScores(List.of(commScore))
                    .finalizedAt(LocalDateTime.now())
                    .build();

            // Evidencia reciente (hace 5 días)
            TaskEvidence recentEvidence = TaskEvidence.builder()
                    .id(1L).family(family)
                    .task(PlanTask.builder().id(1L).build())
                    .evidenceType(EvidenceType.DOCUMENT)
                    .status(EvidenceStatus.VALIDATED)
                    .validated(true)
                    .createdAt(LocalDateTime.now().minusDays(5))
                    .build();

            // 8/10 tareas completadas (80% > 40% umbral)
            PlanTask ct  = PlanTask.builder().id(1L).completed(true).plan(mockPlanForFamily(1L)).build();
            PlanTask ict = PlanTask.builder().id(2L).completed(false).plan(mockPlanForFamily(1L)).build();
            List<PlanTask> tasks = List.of(ct, ct, ct, ct, ct, ct, ct, ct, ict, ict);

            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(evaluationRepository.findWithScoresByFamilyId(1L)).thenReturn(List.of(eval));
            when(planTaskRepository.findAll()).thenReturn(tasks);
            when(taskEvidenceRepository.findAll()).thenReturn(List.of(recentEvidence));
            when(reflectionRepository.findByFamilyId(1L)).thenReturn(List.of());
            when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(1L)).thenReturn(List.of());
            when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            OperativeDashboardResponse result = service.getOperativeDashboard(1L);

            assertThat(result.activeAlerts())
                    .extracting(OperativeAlertDto::alertCode)
                    .doesNotContain(
                            "ALERTA_CRITICAL_COMMUNICATION",
                            "ALERTA_LOW_ADHERENCE",
                            "ALERTA_INACTIVITY"
                    );
        }
    }

    // ───────────────────────────────────────────────────────────────────────
    //  getMetricsTimeline()
    // ───────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMetricsTimeline() — historial de métricas")
    class GetMetricsTimeline {

        @Test
        @DisplayName("mapea correctamente los snapshots a MetricsTimelineDto")
        void mapsSnapshotsToDto() {
            LocalDate d1 = LocalDate.of(2026, 1, 1);
            LocalDate d2 = LocalDate.of(2026, 2, 1);

            FamilyMetricsSnapshot s1 = FamilyMetricsSnapshot.builder()
                    .id(1L).familyId(1L).snapshotDate(d1)
                    .convivenceIndex(65.0).emotionsScore(70.0).adherence(80.0).build();
            FamilyMetricsSnapshot s2 = FamilyMetricsSnapshot.builder()
                    .id(2L).familyId(1L).snapshotDate(d2)
                    .convivenceIndex(72.0).emotionsScore(75.0).adherence(85.0).build();

            when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(1L))
                    .thenReturn(List.of(s1, s2));

            List<MetricsTimelineDto> result = service.getMetricsTimeline(1L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).date()).isEqualTo(d1);
            assertThat(result.get(0).convivenceIndex()).isEqualTo(65.0);
            assertThat(result.get(0).adherenceRate()).isEqualTo(80.0);
            assertThat(result.get(1).date()).isEqualTo(d2);
            assertThat(result.get(1).convivenceIndex()).isEqualTo(72.0);
        }

        @Test
        @DisplayName("sin snapshots → lista vacía")
        void emptyHistory_returnsEmptyList() {
            when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(1L)).thenReturn(List.of());

            assertThat(service.getMetricsTimeline(1L)).isEmpty();
        }
    }

    // ─── helpers de fixture ──────────────────────────────────────────────────

    /** Crea un ImprovementPlan cuya familia tiene el ID indicado */
    private com.integrityfamily.domain.ImprovementPlan mockPlanForFamily(Long familyId) {
        Family f = Family.builder().id(familyId).name("F" + familyId).build();
        com.integrityfamily.domain.ImprovementPlan plan =
                com.integrityfamily.domain.ImprovementPlan.builder().id(familyId).family(f).build();
        return plan;
    }
}
