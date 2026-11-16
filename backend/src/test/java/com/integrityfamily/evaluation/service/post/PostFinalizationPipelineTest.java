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
import com.integrityfamily.risk.service.RiskService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** ADR-020, corte B — AC3 (orden y propagación de riskLevel) y AC6 (los pasos se descubren solos). */
@DisplayName("PostFinalizationPipeline — Unit Tests")
class PostFinalizationPipelineTest {

    private static PostFinalizationContext contextFor(Family family, String risk) {
        Evaluation evaluation = Evaluation.builder()
                .id(100L).family(family).icf(62.5).riskLevel(risk).hasCrisis(false).milestoneKey("W2").build();
        return new PostFinalizationContext(evaluation, null);
    }

    @Test
    @DisplayName("AC3 — @Order deja los 11 pasos en el mismo orden en que se ejecutaban en EvaluationService")
    void stepsAreOrderedAsBefore() {
        List<PostFinalizationStep> steps = new ArrayList<>(List.of(
                new RiskAndLongitudinalSyncStep(mock(RiskService.class), mock(LongitudinalStateService.class)),
                new MilestoneStep(mock(MilestoneService.class)),
                new DiagnosisTasksStep(mock(PlanTaskService.class)),
                new CognitiveMemoryStep(mock(FamilyMemoryService.class)),
                new SkillEngineStep(mock(FamilySkillEngine.class)),
                new IdentityGraphStep(mock(FamilyIdentityGraphService.class)),
                new NarrativeStep(mock(NarrativeEvolutionEngine.class)),
                new ReflectionStep(mock(FamilyReflectionService.class)),
                new ProgressAnalyticsStep(mock(FamilyProgressAnalyticsService.class)),
                new NotificationStep(mock(UserNotificationService.class)),
                new CompletedEventStep(mock(RabbitTemplate.class))));
        Collections.shuffle(steps);

        AnnotationAwareOrderComparator.sort(steps);   // lo mismo que hace Spring al inyectar List<...>

        assertThat(steps).extracting(s -> s.getClass().getSimpleName()).containsExactly(
                "RiskAndLongitudinalSyncStep", "MilestoneStep", "DiagnosisTasksStep", "CognitiveMemoryStep",
                "SkillEngineStep", "IdentityGraphStep", "NarrativeStep", "ReflectionStep",
                "ProgressAnalyticsStep", "NotificationStep", "CompletedEventStep");
    }


    @Test
    @DisplayName("AC3 — el contenedor de Spring inyecta los 11 pasos en el orden correcto (cableado real)")
    @SuppressWarnings("unchecked")
    void springInjectsAllStepsInOrder() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.registerBean(RiskService.class, () -> mock(RiskService.class));
            ctx.registerBean(LongitudinalStateService.class, () -> mock(LongitudinalStateService.class));
            ctx.registerBean(MilestoneService.class, () -> mock(MilestoneService.class));
            ctx.registerBean(PlanTaskService.class, () -> mock(PlanTaskService.class));
            ctx.registerBean(FamilyMemoryService.class, () -> mock(FamilyMemoryService.class));
            ctx.registerBean(FamilySkillEngine.class, () -> mock(FamilySkillEngine.class));
            ctx.registerBean(FamilyIdentityGraphService.class, () -> mock(FamilyIdentityGraphService.class));
            ctx.registerBean(NarrativeEvolutionEngine.class, () -> mock(NarrativeEvolutionEngine.class));
            ctx.registerBean(FamilyReflectionService.class, () -> mock(FamilyReflectionService.class));
            ctx.registerBean(FamilyProgressAnalyticsService.class, () -> mock(FamilyProgressAnalyticsService.class));
            ctx.registerBean(UserNotificationService.class, () -> mock(UserNotificationService.class));
            ctx.registerBean(RabbitTemplate.class, () -> mock(RabbitTemplate.class));
            // Registrados en desorden a propósito: el orden debe salir de @Order, no del registro
            ctx.register(CompletedEventStep.class, NotificationStep.class, ProgressAnalyticsStep.class,
                    ReflectionStep.class, NarrativeStep.class, IdentityGraphStep.class, SkillEngineStep.class,
                    CognitiveMemoryStep.class, DiagnosisTasksStep.class, MilestoneStep.class,
                    RiskAndLongitudinalSyncStep.class, PostFinalizationPipeline.class);
            ctx.refresh();

            List<PostFinalizationStep> injected =
                    (List<PostFinalizationStep>) ReflectionTestUtils.getField(
                            ctx.getBean(PostFinalizationPipeline.class), "steps");

            assertThat(injected).extracting(s -> s.getClass().getSimpleName()).containsExactly(
                    "RiskAndLongitudinalSyncStep", "MilestoneStep", "DiagnosisTasksStep", "CognitiveMemoryStep",
                    "SkillEngineStep", "IdentityGraphStep", "NarrativeStep", "ReflectionStep",
                    "ProgressAnalyticsStep", "NotificationStep", "CompletedEventStep");
        }
    }

    @Test
    @DisplayName("Un paso que falla no impide que se ejecuten los siguientes")
    void aFailingStepDoesNotStopTheRest() {
        PostFinalizationStep failing = mock(PostFinalizationStep.class);
        PostFinalizationStep healthy = mock(PostFinalizationStep.class);
        when(failing.name()).thenReturn("FAILING");
        PostFinalizationContext ctx = contextFor(Family.builder().id(7L).build(), "MODERADO");
        try {
            org.mockito.Mockito.doThrow(new IllegalStateException("boom")).when(failing).execute(ctx);
        } catch (Exception impossible) {
            throw new AssertionError(impossible);
        }

        new PostFinalizationPipeline(List.of(failing, healthy)).run(ctx);

        try {
            verify(healthy).execute(ctx);
        } catch (Exception impossible) {
            throw new AssertionError(impossible);
        }
    }

    @Test
    @DisplayName("AC6 — un paso nuevo se ejecuta sin modificar el pipeline ni EvaluationService")
    void aNewStepIsPickedUpWithoutChangingAnythingElse() throws Exception {
        PostFinalizationStep custom = mock(PostFinalizationStep.class);
        PostFinalizationContext ctx = contextFor(Family.builder().id(7L).build(), "MODERADO");

        new PostFinalizationPipeline(List.of(custom)).run(ctx);

        verify(custom).execute(ctx);
    }

    @Test
    @DisplayName("AC3 — el riskLevel del snapshot llega a la notificación y al evento, y el evento sale al final")
    void riskLevelFlowsFromSnapshotToNotificationAndEvent() throws Exception {
        Family family = Family.builder().id(7L).name("Familia Lopez").build();
        RiskService riskService = mock(RiskService.class);
        RiskSnapshot snapshot = mock(RiskSnapshot.class);
        when(snapshot.getRiskLevel()).thenReturn("CRITICO");
        when(riskService.calculateAndCreate(family, 62.5, false)).thenReturn(snapshot);
        LongitudinalStateService longitudinal = mock(LongitudinalStateService.class);
        MilestoneService milestone = mock(MilestoneService.class);
        UserNotificationService notifications = mock(UserNotificationService.class);
        RabbitTemplate rabbit = mock(RabbitTemplate.class);

        PostFinalizationPipeline pipeline = new PostFinalizationPipeline(List.of(
                new RiskAndLongitudinalSyncStep(riskService, longitudinal),
                new MilestoneStep(milestone),
                new NotificationStep(notifications),
                new CompletedEventStep(rabbit)));

        pipeline.run(contextFor(family, "MODERADO"));

        // Notificación con la etiqueta del riesgo del snapshot, no la de la evaluación
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(notifications).push(eq(family), isNull(), eq("EVALUATION_DONE"), anyString(), message.capture());
        assertThat(message.getValue()).contains("Riesgo: Crítico 🔴");

        // Evento con el mismo riskLevel
        ArgumentCaptor<Object> sent = ArgumentCaptor.forClass(Object.class);
        verify(rabbit).convertAndSend(anyString(), eq("evaluation.completed"), sent.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) ((SystemEvent) sent.getValue()).payload();
        assertThat(payload).containsEntry("riskLevel", "CRITICO");

        // Orden de ejecución
        InOrder order = inOrder(longitudinal, milestone, notifications, rabbit);
        order.verify(longitudinal).syncFromSnapshot(eq(7L), org.mockito.ArgumentMatchers.anyDouble(), eq("CRITICO"),
                org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyDouble());
        order.verify(milestone).advanceMilestone(7L);
        order.verify(notifications).push(eq(family), isNull(), anyString(), anyString(), anyString());
        order.verify(rabbit).convertAndSend(anyString(), eq("evaluation.completed"), org.mockito.ArgumentMatchers.any(Object.class));
    }
}
