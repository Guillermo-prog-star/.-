package com.integrityfamily.plan.service;

import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.ImprovementPlan;
import com.integrityfamily.domain.PlanTask;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.repository.ImprovementPlanRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContinuityEngine")
class ContinuityEngineTest {

    @Mock EvaluationRepository     evaluationRepository;
    @Mock ImprovementPlanRepository planRepository;

    @InjectMocks ContinuityEngine engine;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Evaluation eval(long id, double icf, boolean hasCrisis, LocalDateTime finalized) {
        return Evaluation.builder()
                .id(id)
                .icf(icf)
                .hasCrisis(hasCrisis)
                .finalizedAt(finalized)
                .build();
    }

    private PlanTask task(boolean completed) {
        PlanTask t = new PlanTask();
        t.setCompleted(completed);
        return t;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Primera evaluación (sin historial previo)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("primera evaluación (sin historial)")
    class FirstEvaluation {

        @Test
        @DisplayName("ICF >= 30 sin crisis → IMPROVED, priorIcf=0, delta=0")
        void firstEval_normalIcf_improved() {
            Evaluation current = eval(1L, 65.0, false, LocalDateTime.now());
            // Un solo elemento en historial → size = 1 → rama primera evaluación
            when(evaluationRepository.findByFamilyId(1L)).thenReturn(List.of(current));

            ContinuityEngine.ContinuityAnalysis r = engine.analyzeFamilyContinuity(1L, current);

            assertThat(r.status()).isEqualTo(ContinuityEngine.EvolutionStatus.IMPROVED);
            assertThat(r.priorIcf()).isEqualTo(0.0);
            assertThat(r.icfDelta()).isEqualTo(0.0);
            assertThat(r.currentIcf()).isEqualTo(65.0);
            assertThat(r.hasCrisis()).isFalse();
        }

        @Test
        @DisplayName("ICF < 30 → CRISIS en primera evaluación")
        void firstEval_icfBelow30_crisis() {
            Evaluation current = eval(1L, 25.0, false, LocalDateTime.now());
            when(evaluationRepository.findByFamilyId(1L)).thenReturn(List.of(current));

            ContinuityEngine.ContinuityAnalysis r = engine.analyzeFamilyContinuity(1L, current);

            assertThat(r.status()).isEqualTo(ContinuityEngine.EvolutionStatus.CRISIS);
            assertThat(r.recommendedPlanType()).contains("CONTENCIÓN");
        }

        @Test
        @DisplayName("hasCrisis=true → CRISIS en primera evaluación (sin importar el ICF)")
        void firstEval_hasCrisis_crisis() {
            Evaluation current = eval(1L, 70.0, true, LocalDateTime.now());
            when(evaluationRepository.findByFamilyId(1L)).thenReturn(List.of(current));

            ContinuityEngine.ContinuityAnalysis r = engine.analyzeFamilyContinuity(1L, current);

            assertThat(r.status()).isEqualTo(ContinuityEngine.EvolutionStatus.CRISIS);
            assertThat(r.hasCrisis()).isTrue();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Evaluación con historial — estados de evolución
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("evolución con historial previo")
    class WithHistory {

        @Test
        @DisplayName("delta > 5 → IMPROVED, plan de profundización")
        void delta6_improved() {
            LocalDateTime t1 = LocalDateTime.of(2025, 1, 1, 0, 0);
            LocalDateTime t2 = LocalDateTime.of(2025, 6, 1, 0, 0);
            Evaluation prior   = eval(1L, 60.0, false, t1);
            Evaluation current = eval(2L, 70.0, false, t2);
            // Historial incluye ambas (más reciente primero al ordenar DESC)
            when(evaluationRepository.findByFamilyId(10L)).thenReturn(List.of(prior, current));
            when(planRepository.findByFamilyId(10L)).thenReturn(List.of());

            ContinuityEngine.ContinuityAnalysis r = engine.analyzeFamilyContinuity(10L, current);

            assertThat(r.status()).isEqualTo(ContinuityEngine.EvolutionStatus.IMPROVED);
            assertThat(r.icfDelta()).isEqualTo(10.0);      // 70 - 60 = 10
            assertThat(r.priorIcf()).isEqualTo(60.0);
            assertThat(r.currentIcf()).isEqualTo(70.0);
            assertThat(r.recommendedPlanType()).contains("PROFUNDIZACIÓN");
        }

        @Test
        @DisplayName("delta < -5 → DETERIORATED, plan de intervención correctivo")
        void deltaMinus6_deteriorated() {
            LocalDateTime t1 = LocalDateTime.of(2025, 1, 1, 0, 0);
            LocalDateTime t2 = LocalDateTime.of(2025, 6, 1, 0, 0);
            Evaluation prior   = eval(1L, 70.0, false, t1);
            Evaluation current = eval(2L, 60.0, false, t2);
            when(evaluationRepository.findByFamilyId(11L)).thenReturn(List.of(prior, current));
            when(planRepository.findByFamilyId(11L)).thenReturn(List.of());

            ContinuityEngine.ContinuityAnalysis r = engine.analyzeFamilyContinuity(11L, current);

            assertThat(r.status()).isEqualTo(ContinuityEngine.EvolutionStatus.DETERIORATED);
            assertThat(r.icfDelta()).isEqualTo(-10.0);
            assertThat(r.recommendedPlanType()).contains("CORRECTIVO");
        }

        @Test
        @DisplayName("|delta| <= 5 → STAGNATED, plan de recalibración")
        void deltaSmall_stagnated() {
            LocalDateTime t1 = LocalDateTime.of(2025, 1, 1, 0, 0);
            LocalDateTime t2 = LocalDateTime.of(2025, 6, 1, 0, 0);
            Evaluation prior   = eval(1L, 65.0, false, t1);
            Evaluation current = eval(2L, 68.0, false, t2); // delta = 3
            when(evaluationRepository.findByFamilyId(12L)).thenReturn(List.of(prior, current));
            when(planRepository.findByFamilyId(12L)).thenReturn(List.of());

            ContinuityEngine.ContinuityAnalysis r = engine.analyzeFamilyContinuity(12L, current);

            assertThat(r.status()).isEqualTo(ContinuityEngine.EvolutionStatus.STAGNATED);
            assertThat(r.recommendedPlanType()).contains("RECALIBRACIÓN");
        }

        @Test
        @DisplayName("hasCrisis=true en evaluación actual → CRISIS independientemente del delta")
        void hasCrisis_forcedCrisis() {
            LocalDateTime t1 = LocalDateTime.of(2025, 1, 1, 0, 0);
            LocalDateTime t2 = LocalDateTime.of(2025, 6, 1, 0, 0);
            Evaluation prior   = eval(1L, 60.0, false, t1);
            Evaluation current = eval(2L, 75.0, true,  t2); // delta=+15 → sería IMPROVED sin crisis
            when(evaluationRepository.findByFamilyId(13L)).thenReturn(List.of(prior, current));
            when(planRepository.findByFamilyId(13L)).thenReturn(List.of());

            ContinuityEngine.ContinuityAnalysis r = engine.analyzeFamilyContinuity(13L, current);

            assertThat(r.status()).isEqualTo(ContinuityEngine.EvolutionStatus.CRISIS);
            assertThat(r.recommendedPlanType()).contains("CRISIS");
        }

        @Test
        @DisplayName("ICF actual < 30 con historial → CRISIS")
        void currentIcfBelow30_crisis() {
            LocalDateTime t1 = LocalDateTime.of(2025, 1, 1, 0, 0);
            LocalDateTime t2 = LocalDateTime.of(2025, 6, 1, 0, 0);
            Evaluation prior   = eval(1L, 50.0, false, t1);
            Evaluation current = eval(2L, 25.0, false, t2);
            when(evaluationRepository.findByFamilyId(14L)).thenReturn(List.of(prior, current));
            when(planRepository.findByFamilyId(14L)).thenReturn(List.of());

            ContinuityEngine.ContinuityAnalysis r = engine.analyzeFamilyContinuity(14L, current);

            assertThat(r.status()).isEqualTo(ContinuityEngine.EvolutionStatus.CRISIS);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tasa de cumplimiento de tareas del plan anterior
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("task completion rate del plan anterior")
    class TaskCompletionRate {

        @Test
        @DisplayName("3 tareas: 2 completadas, 1 pendiente → tasa = 66.67%")
        void threeTasksTwoCompleted_rate66() {
            LocalDateTime t1 = LocalDateTime.of(2025, 1, 1, 0, 0);
            LocalDateTime t2 = LocalDateTime.of(2025, 6, 1, 0, 0);
            Evaluation prior   = eval(1L, 60.0, false, t1);
            Evaluation current = eval(2L, 66.0, false, t2); // delta=+6 → IMPROVED

            List<PlanTask> tasks = List.of(task(true), task(true), task(false));
            ImprovementPlan plan = ImprovementPlan.builder()
                    .id(1L)
                    .evaluation(prior)
                    .tasks(tasks)
                    .build();

            when(evaluationRepository.findByFamilyId(20L)).thenReturn(List.of(prior, current));
            when(planRepository.findByFamilyId(20L)).thenReturn(List.of(plan));

            ContinuityEngine.ContinuityAnalysis r = engine.analyzeFamilyContinuity(20L, current);

            assertThat(r.totalPriorTasks()).isEqualTo(3);
            assertThat(r.completedPriorTasks()).isEqualTo(2);
            // 2/3 * 100 = 66.666... — guardamos como double, no redondeamos
            assertThat(r.taskCompletionRate()).isCloseTo(66.67, org.assertj.core.data.Offset.offset(0.01));
        }

        @Test
        @DisplayName("todas las tareas completadas → tasa = 100%")
        void allTasksCompleted_rate100() {
            LocalDateTime t1 = LocalDateTime.of(2025, 1, 1, 0, 0);
            LocalDateTime t2 = LocalDateTime.of(2025, 6, 1, 0, 0);
            Evaluation prior   = eval(1L, 55.0, false, t1);
            Evaluation current = eval(2L, 65.0, false, t2);

            List<PlanTask> tasks = List.of(task(true), task(true));
            ImprovementPlan plan = ImprovementPlan.builder()
                    .id(1L).evaluation(prior).tasks(tasks).build();

            when(evaluationRepository.findByFamilyId(21L)).thenReturn(List.of(prior, current));
            when(planRepository.findByFamilyId(21L)).thenReturn(List.of(plan));

            ContinuityEngine.ContinuityAnalysis r = engine.analyzeFamilyContinuity(21L, current);

            assertThat(r.taskCompletionRate()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("sin plan previo → tasa = 0, totalPriorTasks = 0")
        void noPriorPlan_rate0() {
            LocalDateTime t1 = LocalDateTime.of(2025, 1, 1, 0, 0);
            LocalDateTime t2 = LocalDateTime.of(2025, 6, 1, 0, 0);
            Evaluation prior   = eval(1L, 60.0, false, t1);
            Evaluation current = eval(2L, 66.0, false, t2);

            when(evaluationRepository.findByFamilyId(22L)).thenReturn(List.of(prior, current));
            when(planRepository.findByFamilyId(22L)).thenReturn(List.of());

            ContinuityEngine.ContinuityAnalysis r = engine.analyzeFamilyContinuity(22L, current);

            assertThat(r.totalPriorTasks()).isEqualTo(0);
            assertThat(r.taskCompletionRate()).isEqualTo(0.0);
        }
    }
}
