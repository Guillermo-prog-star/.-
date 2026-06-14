package com.integrityfamily.plan.service;

import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.ImprovementPlan;
import com.integrityfamily.domain.PlanTask;
import com.integrityfamily.domain.repository.PlanTaskRepository;
import com.integrityfamily.domain.repository.QuestionRepository;
import com.integrityfamily.risk.service.RiskAlgoV1Engine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MilestoneAwarePlanEngine")
class MilestoneAwarePlanEngineTest {

    @Mock QuestionRepository questionRepository;
    @Mock PlanTaskRepository  planTaskRepository;

    @InjectMocks MilestoneAwarePlanEngine engine;

    private static final RiskAlgoV1Engine.UncertaintyVector NO_UNCERTAINTY =
            new RiskAlgoV1Engine.UncertaintyVector(0.05, 0.05, 0.05, 0.05, 0.05, 0.25);

    @BeforeEach
    void stubCommonMocks() {
        // Banco de preguntas devuelve vacío → el motor usa metadatos por defecto
        when(questionRepository.findByMilestoneCodeAndTypeAndActiveTrue(anyString(), anyString()))
                .thenReturn(List.of());
        // saveAll devuelve lo que recibe
        when(planTaskRepository.saveAll(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Evaluation evalWithMilestone(String milestone) {
        Family family = Family.builder()
                .id(1L).name("Familia Test").currentMilestone(milestone).build();
        return Evaluation.builder()
                .id(1L).milestoneKey(milestone).family(family).build();
    }

    private ImprovementPlan plan() {
        return ImprovementPlan.builder().id(1L).build();
    }

    private RiskAlgoV1Engine.AlgoResult algo(
            Map<String, Double> scores, String riskLevel, String criticalDim,
            boolean simulation, boolean relapse) {
        return new RiskAlgoV1Engine.AlgoResult(
                scores, 65.0, riskLevel, criticalDim,
                simulation, relapse,
                "ESTABILIZACION_EMOCIONAL", "Consciente", 3,
                List.of(), List.of(), NO_UNCERTAINTY);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Generación por dimensión
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generación de tareas por dimensión")
    class TasksByDimension {

        @Test
        @DisplayName("dimensión crítica con score < 70 → 2 tareas (principal + seguimiento)")
        void criticalDim_twoTasks() {
            Map<String, Double> scores = Map.of(
                    "emociones", 40.0, "comunicacion", 80.0, "habitos", 80.0, "tiempos", 80.0);
            var a = algo(scores, "MODERADO", "emociones", false, false);

            List<PlanTask> tasks = engine.generate(plan(), evalWithMilestone("W1"), a);

            assertThat(tasks).hasSize(2);
            assertThat(tasks.get(0).getTitle()).contains("★");                  // principal
            assertThat(tasks.get(1).getTitle()).contains("Seguimiento");         // follow-up
        }

        @Test
        @DisplayName("dimensión crítica + 1 no-crítica con score < 70 → 3 tareas")
        void criticalPlusOneSecondary_threeTasks() {
            Map<String, Double> scores = Map.of(
                    "emociones", 40.0, "comunicacion", 60.0, "habitos", 80.0, "tiempos", 80.0);
            var a = algo(scores, "MODERADO", "emociones", false, false);

            List<PlanTask> tasks = engine.generate(plan(), evalWithMilestone("W1"), a);

            assertThat(tasks).hasSize(3);
        }

        @Test
        @DisplayName("todas las dimensiones no-críticas con score >= 70 → solo 2 tareas para la crítica")
        void allNonCriticalHighScore_onlyCriticalTasks() {
            Map<String, Double> scores = Map.of(
                    "emociones", 30.0, "comunicacion", 75.0, "habitos", 90.0, "tiempos", 80.0);
            var a = algo(scores, "ALTO", "emociones", false, false);

            List<PlanTask> tasks = engine.generate(plan(), evalWithMilestone("W1"), a);

            assertThat(tasks).hasSize(2);
        }

        @Test
        @DisplayName("todas las dimensiones con score < 70 → 2 (crítica) + 3 (otras) = 5 tareas")
        void allDimsBelow70_fiveTasks() {
            Map<String, Double> scores = Map.of(
                    "emociones", 40.0, "comunicacion", 60.0, "habitos", 65.0, "tiempos", 55.0);
            var a = algo(scores, "ALTO", "emociones", false, false);

            List<PlanTask> tasks = engine.generate(plan(), evalWithMilestone("W1"), a);

            assertThat(tasks).hasSize(5);
        }

        @Test
        @DisplayName("dimensión con score exactamente 70 → NO genera tarea (umbral estricto)")
        void scoreExactly70_noTask() {
            Map<String, Double> scores = Map.of(
                    "emociones", 40.0, "comunicacion", 70.0, "habitos", 80.0, "tiempos", 80.0);
            var a = algo(scores, "MODERADO", "emociones", false, false);

            List<PlanTask> tasks = engine.generate(plan(), evalWithMilestone("W1"), a);

            // Solo la dim crítica genera 2 tareas; comunicacion=70 NO genera tarea
            assertThat(tasks).hasSize(2);
        }

        @Test
        @DisplayName("dimensión con score 69.9 → SÍ genera tarea secundaria")
        void scoreJustBelow70_generatesTask() {
            Map<String, Double> scores = Map.of(
                    "emociones", 40.0, "comunicacion", 69.9, "habitos", 80.0, "tiempos", 80.0);
            var a = algo(scores, "MODERADO", "emociones", false, false);

            List<PlanTask> tasks = engine.generate(plan(), evalWithMilestone("W1"), a);

            assertThat(tasks).hasSize(3); // 2 crítica + 1 comunicacion
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tarea centinela de recaída
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("tarea centinela de recaída (relapseDetected)")
    class RelapseTask {

        @Test
        @DisplayName("relapseDetected=true → +1 tarea de protocolo de recuperación")
        void relapse_addsRelapseTask() {
            Map<String, Double> scores = Map.of(
                    "emociones", 40.0, "comunicacion", 80.0, "habitos", 80.0, "tiempos", 80.0);
            var a = algo(scores, "ALTO", "emociones", false, true);

            List<PlanTask> tasks = engine.generate(plan(), evalWithMilestone("W1"), a);

            assertThat(tasks).hasSize(3); // 2 (crítica) + 1 (recaída)
            boolean hasRelapseTask = tasks.stream()
                    .anyMatch(t -> t.getTitle().contains("Recuperación") || t.getTitle().contains("Recaída"));
            assertThat(hasRelapseTask).isTrue();
        }

        @Test
        @DisplayName("tarea de recaída tiene dueDate = now + 3 días (urgente)")
        void relapseTask_dueDateIs3Days() {
            Map<String, Double> scores = Map.of(
                    "emociones", 40.0, "comunicacion", 80.0, "habitos", 80.0, "tiempos", 80.0);
            var a = algo(scores, "ALTO", "emociones", false, true);

            List<PlanTask> tasks = engine.generate(plan(), evalWithMilestone("W1"), a);

            PlanTask relapseTask = tasks.stream()
                    .filter(t -> t.getRiskType() != null && t.getRiskType().equals("recaida_detectada"))
                    .findFirst().orElseThrow();
            // dueDate debe ser aproximadamente ahora + 3 días
            assertThat(relapseTask.getDueDate()).isAfter(java.time.LocalDateTime.now().plusDays(2));
            assertThat(relapseTask.getDueDate()).isBefore(java.time.LocalDateTime.now().plusDays(4));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tarea de honestidad (simulationSuspected)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("tarea de honestidad (simulationSuspected)")
    class SimulationTask {

        @Test
        @DisplayName("simulationSuspected=true → +1 tarea de espacio de honestidad")
        void simulation_addsHonestyTask() {
            Map<String, Double> scores = Map.of(
                    "emociones", 40.0, "comunicacion", 80.0, "habitos", 80.0, "tiempos", 80.0);
            var a = algo(scores, "MODERADO", "emociones", true, false);

            List<PlanTask> tasks = engine.generate(plan(), evalWithMilestone("W1"), a);

            assertThat(tasks).hasSize(3); // 2 (crítica) + 1 (simulación)
            boolean hasSimTask = tasks.stream()
                    .anyMatch(t -> t.getRiskType() != null && t.getRiskType().equals("simulacion_detectada"));
            assertThat(hasSimTask).isTrue();
        }

        @Test
        @DisplayName("simulationSuspected=true y relapseDetected=true → +2 tareas especiales")
        void bothSimulationAndRelapse_twoExtraTasks() {
            Map<String, Double> scores = Map.of(
                    "emociones", 40.0, "comunicacion", 80.0, "habitos", 80.0, "tiempos", 80.0);
            var a = algo(scores, "CRITICO", "emociones", true, true);

            List<PlanTask> tasks = engine.generate(plan(), evalWithMilestone("W1"), a);

            assertThat(tasks).hasSize(4); // 2 (crítica) + 1 (relapse) + 1 (simulación)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Resolución de milestone
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("resolución de milestone")
    class MilestoneResolution {

        @Test
        @DisplayName("milestoneKey=null → usa currentMilestone de la familia → W1")
        void nullMilestoneKey_usesFamily() {
            Family family = Family.builder().id(1L).name("Test").currentMilestone("W1").build();
            Evaluation eval = Evaluation.builder().id(1L).milestoneKey(null).family(family).build();

            Map<String, Double> scores = Map.of(
                    "emociones", 40.0, "comunicacion", 80.0, "habitos", 80.0, "tiempos", 80.0);
            var a = algo(scores, "MODERADO", "emociones", false, false);

            List<PlanTask> tasks = engine.generate(plan(), eval, a);

            assertThat(tasks).isNotEmpty();
            assertThat(tasks.get(0).getMilestoneCode()).isEqualTo("W1");
        }

        @Test
        @DisplayName("milestoneKey=MES_00_DIAGNOSTICO_BASE → normaliza a W1")
        void diagnosticoBase_normalizesToW1() {
            Map<String, Double> scores = Map.of(
                    "emociones", 40.0, "comunicacion", 80.0, "habitos", 80.0, "tiempos", 80.0);
            var a = algo(scores, "MODERADO", "emociones", false, false);

            List<PlanTask> tasks = engine.generate(plan(), evalWithMilestone("MES_00_DIAGNOSTICO_BASE"), a);

            assertThat(tasks).isNotEmpty();
            assertThat(tasks.get(0).getMilestoneCode()).isEqualTo("W1");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pilares según milestone
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("pilar según milestone")
    class PillarMapping {

        @Test
        @DisplayName("W1 → pilar reconocimiento")
        void w1_reconocimiento() {
            Map<String, Double> scores = Map.of(
                    "emociones", 40.0, "comunicacion", 80.0, "habitos", 80.0, "tiempos", 80.0);
            var a = algo(scores, "MODERADO", "emociones", false, false);

            List<PlanTask> tasks = engine.generate(plan(), evalWithMilestone("W1"), a);

            assertThat(tasks.get(0).getPillarName()).isEqualTo("reconocimiento");
            assertThat(tasks.get(0).getFase()).isEqualTo("RECONOCIMIENTO");
        }

        @Test
        @DisplayName("M6 → pilar amor")
        void m6_amor() {
            Map<String, Double> scores = Map.of(
                    "emociones", 40.0, "comunicacion", 80.0, "habitos", 80.0, "tiempos", 80.0);
            var a = algo(scores, "MODERADO", "emociones", false, false);

            List<PlanTask> tasks = engine.generate(plan(), evalWithMilestone("M6"), a);

            assertThat(tasks.get(0).getPillarName()).isEqualTo("amor");
        }

        @Test
        @DisplayName("M24 → pilar entrega")
        void m24_entrega() {
            Map<String, Double> scores = Map.of(
                    "emociones", 40.0, "comunicacion", 80.0, "habitos", 80.0, "tiempos", 80.0);
            var a = algo(scores, "MODERADO", "emociones", false, false);

            List<PlanTask> tasks = engine.generate(plan(), evalWithMilestone("M24"), a);

            assertThat(tasks.get(0).getPillarName()).isEqualTo("entrega");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Campos de la tarea generada
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("campos de la tarea generada")
    class TaskFields {

        @Test
        @DisplayName("dimension en la tarea está en MAYÚSCULAS")
        void taskDimension_isUpperCase() {
            Map<String, Double> scores = Map.of(
                    "emociones", 40.0, "comunicacion", 80.0, "habitos", 80.0, "tiempos", 80.0);
            var a = algo(scores, "MODERADO", "emociones", false, false);

            List<PlanTask> tasks = engine.generate(plan(), evalWithMilestone("W1"), a);

            assertThat(tasks.get(0).getDimension()).isEqualTo("EMOCIONES");
        }

        @Test
        @DisplayName("impactoIcf para riesgo CRITICO → 20")
        void impactoIcf_critico() {
            Map<String, Double> scores = Map.of(
                    "emociones", 30.0, "comunicacion", 80.0, "habitos", 80.0, "tiempos", 80.0);
            var a = algo(scores, "CRITICO", "emociones", false, false);

            List<PlanTask> tasks = engine.generate(plan(), evalWithMilestone("W1"), a);

            assertThat(tasks.get(0).getImpactoIcf()).isEqualTo(20);
        }

        @Test
        @DisplayName("impactoIcf para riesgo BAJO → 5")
        void impactoIcf_bajo() {
            Map<String, Double> scores = Map.of(
                    "emociones", 30.0, "comunicacion", 80.0, "habitos", 80.0, "tiempos", 80.0);
            var a = algo(scores, "BAJO", "emociones", false, false);

            List<PlanTask> tasks = engine.generate(plan(), evalWithMilestone("W1"), a);

            assertThat(tasks.get(0).getImpactoIcf()).isEqualTo(5);
        }

        @Test
        @DisplayName("riskType por defecto para 'emociones' → 'desconexion_emocional'")
        void defaultRiskType_emociones() {
            Map<String, Double> scores = Map.of(
                    "emociones", 30.0, "comunicacion", 80.0, "habitos", 80.0, "tiempos", 80.0);
            var a = algo(scores, "MODERADO", "emociones", false, false);

            List<PlanTask> tasks = engine.generate(plan(), evalWithMilestone("W1"), a);

            assertThat(tasks.get(0).getRiskType()).isEqualTo("desconexion_emocional");
        }

        @Test
        @DisplayName("completed=false en todas las tareas nuevas")
        void newTasks_completedFalse() {
            Map<String, Double> scores = Map.of(
                    "emociones", 40.0, "comunicacion", 60.0, "habitos", 60.0, "tiempos", 60.0);
            var a = algo(scores, "MODERADO", "emociones", true, true);

            List<PlanTask> tasks = engine.generate(plan(), evalWithMilestone("W1"), a);

            assertThat(tasks).allMatch(t -> !t.isCompleted());
        }

        @Test
        @DisplayName("dueDate de la tarea W1 es aproximadamente ahora + 7 días")
        void dueDate_w1_7days() {
            Map<String, Double> scores = Map.of(
                    "emociones", 40.0, "comunicacion", 80.0, "habitos", 80.0, "tiempos", 80.0);
            var a = algo(scores, "MODERADO", "emociones", false, false);

            List<PlanTask> tasks = engine.generate(plan(), evalWithMilestone("W1"), a);

            assertThat(tasks.get(0).getDueDate())
                    .isAfter(java.time.LocalDateTime.now().plusDays(6))
                    .isBefore(java.time.LocalDateTime.now().plusDays(8));
        }
    }
}
