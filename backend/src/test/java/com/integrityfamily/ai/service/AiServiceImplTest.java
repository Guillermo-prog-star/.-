package com.integrityfamily.ai.service;

import com.integrityfamily.ai.dto.AiContext;
import com.integrityfamily.ai.dto.LogbookCorrelationResult;
import com.integrityfamily.ai.dto.SentimentResult;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.chat.controller.ChatController;
import com.integrityfamily.domain.ChatMessage;
import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.EvaluationAnswer;
import com.integrityfamily.domain.EvaluationDimensionScore;
import com.integrityfamily.domain.EvaluationStatus;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.ChatMessageRepository;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.participation.service.ParticipationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiServiceImpl")
class AiServiceImplTest {

    @Mock AiProvider                 aiProvider;
    @Mock FamilyRepository           familyRepository;
    @Mock EvaluationRepository       evaluationRepository;
    @Mock ContextSynthesizer         contextSynthesizer;
    @Mock ChatMessageRepository      chatMessageRepository;
    @Mock PromptGenerator            promptGenerator;
    @Mock SentimentAnalysisService   sentimentAnalysisService;
    @Mock ParticipationService       participationService;
    @Mock ConversationSessionService conversationSessionService;
    @Mock ConversationGoalManager    conversationGoalManager;
    @Mock EmotionalStateTracker      emotionalStateTracker;
    @Mock PostSessionAnalyzer        postSessionAnalyzer;

    @InjectMocks AiServiceImpl service;

    private static final long FAM_ID = 1L;
    private final Family family = Family.builder().id(FAM_ID).name("Test").build();

    /** AiContext mínimo con activeMember=null → usa rama buildFamilyMentorPrompt. */
    private AiContext emptyContext() {
        return new AiContext(
                null, null, null, null, null, null, null, false, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }

    /** Stubs lenient mínimos para que processInteractiveChat no falle (memberId=null). */
    private void stubInteractiveChat() {
        lenient().when(sentimentAnalysisService.analyze(any()))
                .thenReturn(SentimentResult.builder().label("POSITIVE").score(0.5).build());
        lenient().when(contextSynthesizer.synthesize(
                any(Family.class), isNull(), isNull(), anyString()))
                .thenReturn(emptyContext());
        lenient().when(promptGenerator.buildFamilyMentorPrompt(any(), any())).thenReturn("full-p");
        lenient().when(aiProvider.generateWithFullPrompt("full-p")).thenReturn("respuesta");
        lenient().when(chatMessageRepository.save(any()))
                .thenReturn(ChatMessage.builder().id(1L).content("respuesta").ai(true).family(family).build());
    }

    // ─── generateSynthesis ────────────────────────────────────────────────────

    @Nested
    @DisplayName("generateSynthesis")
    class GenerateSynthesis {

        @Test
        @DisplayName("retorna constante deprecada sin invocar servicios externos")
        void returnsDeprecatedConstant() {
            String result = service.generateSynthesis(Map.of("k", "v"));

            assertThat(result).startsWith("SÍNTESIS_TÉCNICA_DEPRECATED");
            verifyNoInteractions(aiProvider, promptGenerator, contextSynthesizer);
        }
    }

    // ─── generateExecutiveSynthesis(Long) ─────────────────────────────────────

    @Nested
    @DisplayName("generateExecutiveSynthesis(Long)")
    class GenerateExecutiveSynthesisByFamilyId {

        @Test
        @DisplayName("sin evaluación FINALIZED → retorna 'Sin evaluaciones finalizadas.'")
        void noFinalizedEval_returnsNoEvalText() {
            when(evaluationRepository.findTopByFamilyIdAndStatusOrderByFinalizedAtDesc(
                    FAM_ID, EvaluationStatus.FINALIZED)).thenReturn(Optional.empty());

            assertThat(service.generateExecutiveSynthesis(FAM_ID))
                    .isEqualTo("Sin evaluaciones finalizadas.");
        }

        @Test
        @DisplayName("con evaluación FINALIZED → delega a promptGenerator y aiProvider")
        void withFinalizedEval_delegatesAndReturnsAiResponse() {
            Evaluation eval = Evaluation.builder().id(10L).family(family)
                    .dimensionScores(List.of()).answers(List.of()).build();
            when(evaluationRepository.findTopByFamilyIdAndStatusOrderByFinalizedAtDesc(
                    FAM_ID, EvaluationStatus.FINALIZED)).thenReturn(Optional.of(eval));
            when(promptGenerator.buildSpiritualSynthesisPrompt(any(), any(), any())).thenReturn("p");
            when(aiProvider.generateRawResponse("p")).thenReturn("síntesis");

            assertThat(service.generateExecutiveSynthesis(FAM_ID)).isEqualTo("síntesis");
        }
    }

    // ─── generateExecutiveSynthesis(Evaluation) ───────────────────────────────

    @Nested
    @DisplayName("generateExecutiveSynthesis(Evaluation)")
    class GenerateExecutiveSynthesisByEval {

        @Test
        @DisplayName("evaluación con datos → prompt incluye questionKey y dimensión")
        void fullData_buildsPromptWithAnswerData() {
            EvaluationDimensionScore ds = EvaluationDimensionScore.builder()
                    .dimensionName("emociones").score(80.0).build();
            EvaluationAnswer ans = EvaluationAnswer.builder()
                    .questionKey("Q1").score(4)
                    .diagnosticDimension("emociones").consciousnessLevel("Plena").build();
            Evaluation eval = Evaluation.builder().id(1L).family(family)
                    .dimensionScores(List.of(ds)).answers(List.of(ans)).build();

            when(promptGenerator.buildSpiritualSynthesisPrompt(eq(family), any(), contains("Q1")))
                    .thenReturn("prompt-ok");
            when(aiProvider.generateRawResponse("prompt-ok")).thenReturn("síntesis-ok");

            assertThat(service.generateExecutiveSynthesis(eval)).isEqualTo("síntesis-ok");
        }

        @Test
        @DisplayName("campos null en respuesta → usa valores por defecto 'comunicacion' y 'Consciente'")
        void nullAnswerFields_usesDefaults() {
            EvaluationAnswer ans = EvaluationAnswer.builder()
                    .questionKey("Q2").score(3)
                    .diagnosticDimension(null).consciousnessLevel(null).build();
            Evaluation eval = Evaluation.builder().id(2L).family(family)
                    .dimensionScores(List.of()).answers(List.of(ans)).build();

            when(promptGenerator.buildSpiritualSynthesisPrompt(any(), any(),
                    argThat(j -> j.contains("comunicacion") && j.contains("Consciente"))))
                    .thenReturn("p");
            when(aiProvider.generateRawResponse("p")).thenReturn("ok");

            assertThat(service.generateExecutiveSynthesis(eval)).isEqualTo("ok");
        }
    }

    // ─── processAnalyticInference ─────────────────────────────────────────────

    @Nested
    @DisplayName("processAnalyticInference")
    class ProcessAnalyticInference {

        @Test
        @DisplayName("familyId null → aiProvider recibe context=null directamente")
        void nullFamilyId_contextIsNull() {
            when(aiProvider.generateResponse("pregunta", null)).thenReturn("respuesta");

            assertThat(service.processAnalyticInference("pregunta", null)).isEqualTo("respuesta");
            verify(familyRepository, never()).findById(any());
        }

        @Test
        @DisplayName("familia encontrada → sintetiza contexto y lo pasa a aiProvider")
        void familyFound_contextSynthesized() {
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
            AiContext ctx = emptyContext();
            when(contextSynthesizer.synthesize(family, "NEUTRAL")).thenReturn(ctx);
            when(aiProvider.generateResponse("q", ctx)).thenReturn("r");

            assertThat(service.processAnalyticInference("q", FAM_ID)).isEqualTo("r");
        }

        @Test
        @DisplayName("familia no encontrada → aiProvider recibe context=null")
        void familyNotFound_contextIsNull() {
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.empty());
            when(aiProvider.generateResponse("q", null)).thenReturn("fallback");

            assertThat(service.processAnalyticInference("q", FAM_ID)).isEqualTo("fallback");
        }
    }

    // ─── generateDashboardInsight ─────────────────────────────────────────────

    @Nested
    @DisplayName("generateDashboardInsight")
    class GenerateDashboardInsight {

        @Test
        @DisplayName("delega al promptGenerator y al aiProvider")
        void delegatesAndReturnsAiResponse() {
            Map<String, Double> dims = Map.of("emociones", 70.0);
            when(promptGenerator.buildDashboardInsightPrompt(family, dims, "BAJO")).thenReturn("dp");
            when(aiProvider.generateRawResponse("dp")).thenReturn("insight");

            assertThat(service.generateDashboardInsight(family, dims, "BAJO")).isEqualTo("insight");
        }
    }

    // ─── generateEvolutionaryMissions ─────────────────────────────────────────

    @Nested
    @DisplayName("generateEvolutionaryMissions")
    class GenerateEvolutionaryMissions {

        @Test
        @DisplayName("delega al promptGenerator y al aiProvider")
        void delegatesAndReturnsAiResponse() {
            Map<String, Double> dims = Map.of("habitos", 55.0);
            when(promptGenerator.buildMissionGenerationPrompt(family, dims, "MODERADO")).thenReturn("ep");
            when(aiProvider.generateRawResponse("ep")).thenReturn("misiones");

            assertThat(service.generateEvolutionaryMissions(family, dims, "MODERADO")).isEqualTo("misiones");
        }
    }

    // ─── generateHybridPlan ───────────────────────────────────────────────────

    @Nested
    @DisplayName("generateHybridPlan")
    class GenerateHybridPlan {

        @Test
        @DisplayName("análisis de sentimiento lanza excepción → plan generado con correlation=null")
        void sentimentFails_planGeneratedWithNullCorrelation() {
            Map<String, Double> dims = Map.of();
            when(sentimentAnalysisService.correlateFamilySentiment(FAM_ID))
                    .thenThrow(new RuntimeException("no data"));
            when(promptGenerator.buildHybridPlanPrompt(eq(family), eq(dims), eq("BAJO"), isNull(), isNull()))
                    .thenReturn("plan-p");
            when(aiProvider.generateRawResponse("plan-p")).thenReturn("plan");

            assertThat(service.generateHybridPlan(family, dims, "BAJO")).isEqualTo("plan");
        }

        @Test
        @DisplayName("análisis de sentimiento exitoso → correlación propagada al promptGenerator")
        void sentimentSucceeds_correlationPropagated() {
            Map<String, Double> dims = Map.of();
            LogbookCorrelationResult corr = LogbookCorrelationResult.builder()
                    .generalLabel("CONSCIENTE").averageEmotionalScore(0.1).build();
            when(sentimentAnalysisService.correlateFamilySentiment(FAM_ID)).thenReturn(corr);
            when(promptGenerator.buildHybridPlanPrompt(eq(family), eq(dims), eq("ALTO"), eq(corr), isNull()))
                    .thenReturn("hybrid-p");
            when(aiProvider.generateRawResponse("hybrid-p")).thenReturn("plan-hibrido");

            assertThat(service.generateHybridPlan(family, dims, "ALTO")).isEqualTo("plan-hibrido");
        }
    }

    // ─── chatWithTransformation ───────────────────────────────────────────────

    @Nested
    @DisplayName("chatWithTransformation")
    class ChatWithTransformation {

        @Test
        @DisplayName("sin contexto de transformación → mensaje original llega sin prefijo")
        void noContext_originalMessagePassedDirectly() {
            stubInteractiveChat();
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

            service.chatWithTransformation("Hola familia", family, null, null);

            verify(sentimentAnalysisService).analyze(captor.capture());
            assertThat(captor.getValue()).isEqualTo("Hola familia");
        }

        @Test
        @DisplayName("con TransformationContextDto → mensaje lleva prefijo [CONTEXTO_TRANSFORMACIÓN]")
        void withTransformationContext_prefixAddedToMessage() {
            stubInteractiveChat();
            ChatController.ChatRequestV2.TransformationContextDto tc =
                    new ChatController.ChatRequestV2.TransformationContextDto();
            tc.setCurrentPillar("emociones");
            tc.setCurrentMonth(3);
            tc.setSprintNumber(2);
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

            service.chatWithTransformation("¿Cómo mejorar?", family, null, tc);

            verify(sentimentAnalysisService).analyze(captor.capture());
            assertThat(captor.getValue())
                    .contains("[CONTEXTO_TRANSFORMACIÓN")
                    .contains("emociones")
                    .contains("¿Cómo mejorar?");
        }
    }
}
