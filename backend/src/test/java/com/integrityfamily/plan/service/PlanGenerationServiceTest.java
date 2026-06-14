package com.integrityfamily.plan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.ai.service.AiService;
import com.integrityfamily.checklist.service.ChecklistService;
import com.integrityfamily.common.service.UserNotificationService;
import com.integrityfamily.common.service.WhatsAppService;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import com.integrityfamily.risk.service.RiskAlgoV1Engine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlanGenerationService")
class PlanGenerationServiceTest {

    @Mock PlanService                 planService;
    @Mock EvaluationRepository        evaluationRepository;
    @Mock AiService                   aiService;
    @Mock WhatsAppService             whatsappService;
    @Mock ChecklistService            checklistService;
    @Mock MilestoneRepository         milestoneRepository;
    @Mock ContinuityEngine            continuityEngine;
    @Mock PlanValidator               planValidator;
    @Mock ImprovementPlanRepository   planRepository;
    @Mock MilestoneAwarePlanEngine    milestoneAwarePlanEngine;
    @Mock RiskAlgoV1Engine            riskAlgoV1Engine;
    @Mock UserNotificationService     userNotificationService;
    @Spy  ObjectMapper                objectMapper = new ObjectMapper();

    @InjectMocks PlanGenerationService service;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Evaluation evalWithMilestone(String milestone) {
        Family family = Family.builder()
                .id(1L).name("Familia Test").currentMilestone(milestone).build();
        return Evaluation.builder()
                .id(1L).icf(65.0).riskLevel("MODERADO")
                .criticalDimension("comunicacion")
                .milestoneKey(milestone)
                .family(family)
                .dimensionScores(List.of())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // generateMilestoneAwareFallback
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generateMilestoneAwareFallback")
    class MilestoneAwareFallback {

        @Test
        @DisplayName("persiste plan con título correcto y llama a milestoneAwarePlanEngine.generate()")
        void createsAndPersistsPlan() {
            Evaluation eval = evalWithMilestone("W1");
            when(planRepository.findByFamilyId(1L)).thenReturn(List.of());
            when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(milestoneAwarePlanEngine.generate(any(), any(), any())).thenReturn(List.of());

            service.generateMilestoneAwareFallback(eval);

            ArgumentCaptor<ImprovementPlan> planCaptor = ArgumentCaptor.forClass(ImprovementPlan.class);
            verify(planRepository).save(planCaptor.capture());
            ImprovementPlan saved = planCaptor.getValue();
            assertThat(saved.getTitle()).contains("Familia Test");
            assertThat(saved.getVision3y()).contains("36 meses");
            assertThat(saved.getFamily().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("elimina planes existentes antes de crear uno nuevo (deduplicación)")
        void deduplicatesExistingPlans() {
            Evaluation eval = evalWithMilestone("W1");
            ImprovementPlan existing = ImprovementPlan.builder().id(99L).build();
            when(planRepository.findByFamilyId(1L)).thenReturn(List.of(existing));
            when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(milestoneAwarePlanEngine.generate(any(), any(), any())).thenReturn(List.of());

            service.generateMilestoneAwareFallback(eval);

            verify(planRepository).deleteAll(List.of(existing));
            verify(planRepository).flush();
        }

        @Test
        @DisplayName("milestoneAwarePlanEngine.generate() recibe AlgoResult con datos de la evaluación")
        void passesSyntheticAlgoToEngine() {
            Evaluation eval = evalWithMilestone("M6");
            when(planRepository.findByFamilyId(1L)).thenReturn(List.of());
            when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(milestoneAwarePlanEngine.generate(any(), eq(eval), any())).thenReturn(List.of());

            service.generateMilestoneAwareFallback(eval);

            ArgumentCaptor<RiskAlgoV1Engine.AlgoResult> algoCaptor =
                    ArgumentCaptor.forClass(RiskAlgoV1Engine.AlgoResult.class);
            verify(milestoneAwarePlanEngine).generate(any(), any(), algoCaptor.capture());
            RiskAlgoV1Engine.AlgoResult algo = algoCaptor.getValue();
            assertThat(algo.healthyIndex()).isEqualTo(65.0);
            assertThat(algo.riskLevel()).isEqualTo("MODERADO");
            assertThat(algo.criticalDimension()).isEqualTo("comunicacion");
        }

        @Test
        @DisplayName("evaluation sin icf → usa 60.0 por defecto")
        void missingIcf_usesDefault() {
            Family family = Family.builder().id(1L).name("Test").currentMilestone("W1").build();
            Evaluation eval = Evaluation.builder()
                    .id(1L).icf(null).riskLevel("ALTO").criticalDimension("emociones")
                    .family(family).dimensionScores(List.of()).build();
            when(planRepository.findByFamilyId(1L)).thenReturn(List.of());
            when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(milestoneAwarePlanEngine.generate(any(), any(), any())).thenReturn(List.of());

            service.generateMilestoneAwareFallback(eval);

            ArgumentCaptor<RiskAlgoV1Engine.AlgoResult> cap =
                    ArgumentCaptor.forClass(RiskAlgoV1Engine.AlgoResult.class);
            verify(milestoneAwarePlanEngine).generate(any(), any(), cap.capture());
            assertThat(cap.getValue().healthyIndex()).isEqualTo(60.0);
        }

        @Test
        @DisplayName("evaluation sin riskLevel → usa 'MODERADO' por defecto")
        void missingRiskLevel_usesDefault() {
            Family family = Family.builder().id(1L).name("Test").currentMilestone("W1").build();
            Evaluation eval = Evaluation.builder()
                    .id(1L).icf(70.0).riskLevel(null).criticalDimension("habitos")
                    .family(family).dimensionScores(List.of()).build();
            when(planRepository.findByFamilyId(1L)).thenReturn(List.of());
            when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(milestoneAwarePlanEngine.generate(any(), any(), any())).thenReturn(List.of());

            service.generateMilestoneAwareFallback(eval);

            ArgumentCaptor<RiskAlgoV1Engine.AlgoResult> cap =
                    ArgumentCaptor.forClass(RiskAlgoV1Engine.AlgoResult.class);
            verify(milestoneAwarePlanEngine).generate(any(), any(), cap.capture());
            assertThat(cap.getValue().riskLevel()).isEqualTo("MODERADO");
        }

        @Test
        @DisplayName("dimensionScores en evaluación → se incluyen en el AlgoResult sintético")
        void dimensionScores_included() {
            Family family = Family.builder().id(1L).name("Test").currentMilestone("W1").build();
            EvaluationDimensionScore ds = EvaluationDimensionScore.builder()
                    .dimensionName("emociones").score(75.0).build();
            Evaluation eval = Evaluation.builder()
                    .id(1L).icf(65.0).riskLevel("BAJO").criticalDimension("comunicacion")
                    .family(family).dimensionScores(List.of(ds)).build();
            when(planRepository.findByFamilyId(1L)).thenReturn(List.of());
            when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(milestoneAwarePlanEngine.generate(any(), any(), any())).thenReturn(List.of());

            service.generateMilestoneAwareFallback(eval);

            ArgumentCaptor<RiskAlgoV1Engine.AlgoResult> cap =
                    ArgumentCaptor.forClass(RiskAlgoV1Engine.AlgoResult.class);
            verify(milestoneAwarePlanEngine).generate(any(), any(), cap.capture());
            assertThat(cap.getValue().dimensionScores()).containsEntry("emociones", 75.0);
        }

        @Test
        @DisplayName("error en userNotificationService → NO propaga excepción (swallowed)")
        void notificationError_swallowed() {
            Evaluation eval = evalWithMilestone("W1");
            when(planRepository.findByFamilyId(1L)).thenReturn(List.of());
            when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(milestoneAwarePlanEngine.generate(any(), any(), any())).thenReturn(List.of());
            doThrow(new RuntimeException("WhatsApp down"))
                    .when(userNotificationService).push(any(), any(), any(), any(), any());

            // No debe lanzar excepción
            org.assertj.core.api.Assertions.assertThatNoException()
                    .isThrownBy(() -> service.generateMilestoneAwareFallback(eval));
        }

        @Test
        @DisplayName("plan generado tiene aiReport que menciona MilestoneAwarePlanEngine")
        void aiReport_mentionsDeterministicEngine() {
            Evaluation eval = evalWithMilestone("W1");
            when(planRepository.findByFamilyId(1L)).thenReturn(List.of());
            when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(milestoneAwarePlanEngine.generate(any(), any(), any())).thenReturn(List.of());

            service.generateMilestoneAwareFallback(eval);

            ArgumentCaptor<ImprovementPlan> cap = ArgumentCaptor.forClass(ImprovementPlan.class);
            verify(planRepository).save(cap.capture());
            assertThat(cap.getValue().getAiReport()).contains("determinístico");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // generatePlanFromEvaluation — event parsing (via reflection on extractJson behavior)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generatePlanFromEvaluation — validación de evento")
    class EventParsing {

        @Test
        @DisplayName("evento sin evaluationId → retorna sin hacer nada (log + return)")
        void noEvaluationId_noSideEffects() {
            // No debe llamar a evaluationRepository.findById() ni a ningún otro servicio
            service.generatePlanFromEvaluation(Map.of("otherField", "value"));

            verifyNoInteractions(evaluationRepository, planRepository,
                    milestoneAwarePlanEngine, planService);
        }

        @Test
        @DisplayName("evento null → retorna sin hacer nada")
        void nullEvent_noSideEffects() {
            service.generatePlanFromEvaluation(null);

            verifyNoInteractions(evaluationRepository);
        }

        @Test
        @DisplayName("payload embebido con evaluationId → invoca evaluationRepository.findById()")
        void embeddedPayload_findsEvaluation() {
            Family family = Family.builder().id(1L).name("T").currentMilestone("W1").build();
            Evaluation eval = Evaluation.builder()
                    .id(42L).icf(65.0).family(family)
                    .dimensionScores(List.of()).build();
            when(evaluationRepository.findById(42L)).thenReturn(java.util.Optional.of(eval));
            // Síntesis espiritual falla → swallowed (no propaga)
            when(aiService.generateExecutiveSynthesis((Evaluation) any())).thenThrow(new RuntimeException("IA off"));
            // generateHybridPlan retorna null → extractJson("{}")  → HybridPlanDto con milestones=null
            // → p.getTasks().isEmpty() → RuntimeException → catches → generateMilestoneAwareFallback
            when(aiService.generateHybridPlan(any(), any(), any(), any())).thenReturn(null);
            when(continuityEngine.analyzeFamilyContinuity(any(), any()))
                    .thenReturn(new ContinuityEngine.ContinuityAnalysis(
                            ContinuityEngine.EvolutionStatus.IMPROVED, 65.0, 65.0, 0.0,
                            0, 0, 0.0, false, "PROFUNDIZACIÓN", "Resumen"));
            when(planRepository.findByFamilyId(1L)).thenReturn(List.of());
            when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(milestoneAwarePlanEngine.generate(any(), any(), any())).thenReturn(List.of());

            Map<String, Object> payload = Map.of("evaluationId", 42);
            Map<String, Object> event = Map.of("payload", payload);

            service.generatePlanFromEvaluation(event);

            verify(evaluationRepository).findById(42L);
        }
    }
}
