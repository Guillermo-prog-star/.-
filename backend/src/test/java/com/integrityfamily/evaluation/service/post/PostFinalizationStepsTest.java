package com.integrityfamily.evaluation.service.post;

import com.integrityfamily.analytics.service.FamilyProgressAnalyticsService;
import com.integrityfamily.cognitive.service.FamilyIdentityGraphService;
import com.integrityfamily.cognitive.service.FamilyMemoryService;
import com.integrityfamily.cognitive.service.FamilyReflectionService;
import com.integrityfamily.cognitive.service.FamilySkillEngine;
import com.integrityfamily.cognitive.service.NarrativeEvolutionEngine;
import com.integrityfamily.common.event.SystemEvent;
import com.integrityfamily.common.service.UserNotificationService;
import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.RiskSnapshot;
import com.integrityfamily.milestone.service.MilestoneService;
import com.integrityfamily.plan.service.PlanTaskService;
import com.integrityfamily.risk.service.LongitudinalStateService;
import com.integrityfamily.risk.service.RiskAlgoV1Engine;
import com.integrityfamily.risk.service.RiskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** ADR-020, corte B — un test por cada paso posterior a la finalización (AC5). */
@DisplayName("PostFinalizationSteps — Unit Tests")
class PostFinalizationStepsTest {

    private Family family;
    private Evaluation evaluation;

    @BeforeEach
    void setUp() {
        family = Family.builder().id(7L).name("Familia Lopez").build();
        evaluation = Evaluation.builder()
                .id(100L)
                .family(family)
                .icf(62.5)
                .riskLevel("MODERADO")
                .hasCrisis(false)
                .milestoneKey("W2")
                .build();
    }

    private static RiskAlgoV1Engine.AlgoResult algoWith(Map<String, Double> dims) {
        return new RiskAlgoV1Engine.AlgoResult(
                dims, 62.5, "MODERADO", "comunicacion", false, false,
                "COMUNICACION_CONSCIENTE", "Reactiva", 4, List.of(), List.of(),
                new RiskAlgoV1Engine.UncertaintyVector(0.05, 0.20, 0.10, 0.05, 0.05, 0.09));
    }

    // ═══════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("RiskAndLongitudinalSyncStep")
    class RiskAndLongitudinalSync {

        private final RiskService riskService = mock(RiskService.class);
        private final LongitudinalStateService longitudinal = mock(LongitudinalStateService.class);
        private final RiskAndLongitudinalSyncStep step = new RiskAndLongitudinalSyncStep(riskService, longitudinal);

        @Test
        @DisplayName("Con algo → sincroniza las 4 dimensiones y propaga el riskLevel del snapshot")
        void syncsDimensionsAndPropagatesSnapshotRisk() {
            RiskSnapshot snapshot = mock(RiskSnapshot.class);
            when(snapshot.getRiskLevel()).thenReturn("ALTO");
            when(riskService.calculateAndCreate(family, 62.5, false)).thenReturn(snapshot);
            PostFinalizationContext ctx = new PostFinalizationContext(evaluation,
                    algoWith(Map.of("emociones", 40.0, "comunicacion", 55.0, "habitos", 70.0, "tiempos", 80.0)));

            step.execute(ctx);

            assertThat(ctx.riskLevel()).isEqualTo("ALTO");
            verify(longitudinal).syncFromSnapshot(7L, 62.5, "ALTO", 40.0, 55.0, 70.0, 80.0);
        }

        @Test
        @DisplayName("Sin algo (simulación) → sincroniza solo ICF y riesgo, dimensiones en 50.0")
        void withoutAlgo_usesNeutralDimensions() {
            PostFinalizationContext ctx = new PostFinalizationContext(evaluation, null);

            step.execute(ctx);

            verify(longitudinal).syncFromSnapshot(7L, 62.5, "MODERADO", 50.0, 50.0, 50.0, 50.0);
        }

        @Test
        @DisplayName("Snapshot nulo → conserva el riskLevel de la evaluación")
        void nullSnapshot_keepsEvaluationRisk() {
            when(riskService.calculateAndCreate(family, 62.5, false)).thenReturn(null);
            PostFinalizationContext ctx = new PostFinalizationContext(evaluation, null);

            step.execute(ctx);

            assertThat(ctx.riskLevel()).isEqualTo("MODERADO");
        }

        @Test
        @DisplayName("Si riskService falla no sincroniza y el error sube al pipeline")
        void riskFailure_skipsSyncAndPropagates() {
            when(riskService.calculateAndCreate(any(), any(), org.mockito.ArgumentMatchers.anyBoolean())).thenThrow(new IllegalStateException("sin snapshot"));
            PostFinalizationContext ctx = new PostFinalizationContext(evaluation, null);

            assertThatThrownBy(() -> step.execute(ctx)).isInstanceOf(IllegalStateException.class);

            org.mockito.Mockito.verifyNoInteractions(longitudinal);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("MilestoneStep")
    class Milestone {
        @Test
        @DisplayName("Avanza el hito de la familia")
        void advancesMilestone() {
            MilestoneService milestoneService = mock(MilestoneService.class);

            new MilestoneStep(milestoneService).execute(new PostFinalizationContext(evaluation, null));

            verify(milestoneService).advanceMilestone(7L);
        }
    }

    @Nested
    @DisplayName("DiagnosisTasksStep")
    class DiagnosisTasks {
        @Test
        @DisplayName("Genera las misiones automáticas del diagnóstico (assertion movida desde EvaluationServiceConsciousTest)")
        void generatesTasksFromDiagnosis() {
            PlanTaskService planTaskService = mock(PlanTaskService.class);

            new DiagnosisTasksStep(planTaskService).execute(new PostFinalizationContext(evaluation, null));

            verify(planTaskService).generateTasksFromDiagnosis(evaluation);
        }
    }

    @Nested
    @DisplayName("CognitiveMemoryStep")
    class CognitiveMemory {
        @Test
        @DisplayName("Captura la memoria episódica de la evaluación")
        void capturesEvaluationMemory() {
            FamilyMemoryService memory = mock(FamilyMemoryService.class);

            new CognitiveMemoryStep(memory).execute(new PostFinalizationContext(evaluation, null));

            verify(memory).captureEvaluationMemory(evaluation);
        }
    }

    @Nested
    @DisplayName("SkillEngineStep")
    class SkillEngine {
        @Test
        @DisplayName("Analiza habilidades de la familia con la evaluación")
        void analyzesSkills() {
            FamilySkillEngine engine = mock(FamilySkillEngine.class);
            when(engine.analyze(7L, evaluation)).thenReturn(mock(FamilySkillEngine.SkillEngineResult.class));

            new SkillEngineStep(engine).execute(new PostFinalizationContext(evaluation, null));

            verify(engine).analyze(7L, evaluation);
        }
    }

    @Nested
    @DisplayName("IdentityGraphStep")
    class IdentityGraph {
        @Test
        @DisplayName("Actualiza el grafo de identidad de la familia")
        void updatesGraph() {
            FamilyIdentityGraphService graphService = mock(FamilyIdentityGraphService.class);
            when(graphService.updateGraph(7L, evaluation)).thenReturn(mock(FamilyIdentityGraphService.GraphSnapshot.class));

            new IdentityGraphStep(graphService).execute(new PostFinalizationContext(evaluation, null));

            verify(graphService).updateGraph(7L, evaluation);
        }
    }

    @Nested
    @DisplayName("NarrativeStep")
    class Narrative {
        @Test
        @DisplayName("Evoluciona la narrativa de la familia")
        void evolvesNarrative() {
            NarrativeEvolutionEngine engine = mock(NarrativeEvolutionEngine.class);
            when(engine.evolve(7L, evaluation)).thenReturn(mock(NarrativeEvolutionEngine.NarrativeSnapshot.class));

            new NarrativeStep(engine).execute(new PostFinalizationContext(evaluation, null));

            verify(engine).evolve(7L, evaluation);
        }
    }

    @Nested
    @DisplayName("ReflectionStep")
    class Reflection {
        @Test
        @DisplayName("Ejecuta la reflexión autónoma de la familia")
        void reflects() {
            FamilyReflectionService service = mock(FamilyReflectionService.class);
            when(service.reflect(7L)).thenReturn(mock(FamilyReflectionService.ReflectionReport.class, RETURNS_DEEP_STUBS));

            new ReflectionStep(service).execute(new PostFinalizationContext(evaluation, null));

            verify(service).reflect(7L);
        }
    }

    @Nested
    @DisplayName("ProgressAnalyticsStep")
    class ProgressAnalytics {
        @Test
        @DisplayName("Analiza el progreso a partir del id de la evaluación")
        void analyzesProgress() {
            FamilyProgressAnalyticsService analytics = mock(FamilyProgressAnalyticsService.class);

            new ProgressAnalyticsStep(analytics).execute(new PostFinalizationContext(evaluation, null));

            verify(analytics).analyzeProgress(100L);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("NotificationStep")
    class Notification {

        private final UserNotificationService notifications = mock(UserNotificationService.class);
        private final NotificationStep step = new NotificationStep(notifications);

        @ParameterizedTest(name = "riesgo {0} → etiqueta \"{1}\"")
        @CsvSource({
                "BAJO,Bajo ✅",
                "MODERADO,Moderado ⚠️",
                "ALTO,Alto 🔶",
                "CRITICO,Crítico 🔴",
                "DESCONOCIDO,DESCONOCIDO"
        })
        void labelsRiskLevel(String riskLevel, String expectedLabel) {
            PostFinalizationContext ctx = new PostFinalizationContext(evaluation, null);
            ctx.setRiskLevel(riskLevel);

            step.execute(ctx);

            ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
            verify(notifications).push(eq(family), isNull(), eq("EVALUATION_DONE"),
                    eq("Diagnóstico completado 📊"), message.capture());
            assertThat(message.getValue()).contains("Riesgo: " + expectedLabel).contains("Hito: W2");
        }

        @Test
        @DisplayName("ICF y hito ausentes → 0.0 y \"—\"")
        void missingIcfAndMilestone_useFallbacks() {
            evaluation.setIcf(null);
            evaluation.setMilestoneKey(null);

            step.execute(new PostFinalizationContext(evaluation, null));

            ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
            verify(notifications).push(any(), any(), anyString(), anyString(), message.capture());
            assertThat(message.getValue()).contains("ICF: 0.0").contains("Hito: —");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("CompletedEventStep")
    class CompletedEvent {

        private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
        private final CompletedEventStep step = new CompletedEventStep(rabbit);

        @Test
        @DisplayName("Sin transacción activa → publica de inmediato con el payload completo")
        void publishesPayload() {
            PostFinalizationContext ctx = new PostFinalizationContext(evaluation, null);
            ctx.setRiskLevel("ALTO");

            step.execute(ctx);

            ArgumentCaptor<Object> sent = ArgumentCaptor.forClass(Object.class);
            verify(rabbit).convertAndSend(anyString(), eq("evaluation.completed"), sent.capture());
            SystemEvent event = (SystemEvent) sent.getValue();
            assertThat(event.routingKey()).isEqualTo("evaluation.completed");
            assertThat(event.familyId()).isEqualTo(7L);
            assertThat(event.triggeredBy()).isEqualTo("SYSTEM");
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) event.payload();
            assertThat(payload)
                    .containsEntry("evaluationId", 100L)
                    .containsEntry("icf", 62.5)
                    .containsEntry("familyId", 7L)
                    .containsEntry("riskLevel", "ALTO");
        }

        @Test
        @DisplayName("Un fallo de RabbitMQ no se propaga")
        void rabbitFailure_isNotPropagated() {
            org.mockito.Mockito.doThrow(new RuntimeException("broker caído"))
                    .when(rabbit).convertAndSend(anyString(), anyString(), any(Object.class));

            assertThatCode(() -> step.execute(new PostFinalizationContext(evaluation, null)))
                    .doesNotThrowAnyException();
        }
    }
}
