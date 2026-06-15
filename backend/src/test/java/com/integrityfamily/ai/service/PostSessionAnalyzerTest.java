package com.integrityfamily.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.ai.dto.AiContext;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.cognitive.service.MemberIdentityProfileService;
import com.integrityfamily.domain.repository.ChatMessageRepository;
import com.integrityfamily.domain.repository.ConversationSessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostSessionAnalyzer")
class PostSessionAnalyzerTest {

    @Mock ChatMessageRepository          chatMessageRepository;
    @Mock ConversationSessionRepository  sessionRepository;
    @Mock MemberIdentityProfileService   identityProfileService;
    @Mock SessionMemoryConsolidator      sessionMemoryConsolidator;
    @Mock AiProvider                     aiProvider;
    @Mock PromptGenerator                promptGenerator;
    @Spy  ObjectMapper                   objectMapper = new ObjectMapper();
    @InjectMocks PostSessionAnalyzer analyzer;

    private static final long SESSION_ID = 1L;
    private static final long FAM_ID     = 2L;
    private static final long MEM_ID     = 10L;

    private static final String VALID_IDENTITY_JSON = """
            {
              "communicationStyle": "DIRECT",
              "reflexivityLevel": 4,
              "emotionalSensitivity": 3,
              "changeResistance": "LOW",
              "evasionPatterns": [],
              "motivators": ["logro", "familia"]
            }
            """;

    // ── analyzeIfThresholdReached ─────────────────────────────────────────────

    @Nested
    @DisplayName("analyzeIfThresholdReached — guards")
    class Guards {

        @Test
        @DisplayName("sessionId=null → no hace nada")
        void nullSessionId_noop() {
            analyzer.analyzeIfThresholdReached(null, FAM_ID, MEM_ID, 10, null);

            verifyNoInteractions(chatMessageRepository, identityProfileService, sessionMemoryConsolidator);
        }

        @Test
        @DisplayName("turnCount < 3 → no hace nada")
        void lowTurnCount_noop() {
            analyzer.analyzeIfThresholdReached(SESSION_ID, FAM_ID, MEM_ID, 2, null);

            verifyNoInteractions(chatMessageRepository, identityProfileService, sessionMemoryConsolidator);
        }

        @Test
        @DisplayName("turnCount=4 (no múltiplo de 5 ni de 10) → no hace nada")
        void nonThresholdTurn_noop() {
            analyzer.analyzeIfThresholdReached(SESSION_ID, FAM_ID, MEM_ID, 4, null);

            verifyNoInteractions(chatMessageRepository, identityProfileService, sessionMemoryConsolidator);
        }
    }

    @Nested
    @DisplayName("analyzeIfThresholdReached — identidad (cada 5 turnos)")
    class IdentityAnalysis {

        @Test
        @DisplayName("turno 5, memberId válido → dispara análisis de identidad")
        void turn5_triggersIdentityAnalysis() {
            AiContext ctx = mock(AiContext.class); // activeMember() → null por defecto → usa "el miembro"/"FAMILIA"
            when(chatMessageRepository.findUserMessageContentsBySessionId(SESSION_ID))
                    .thenReturn(List.of("msg1", "msg2", "msg3"));
            when(promptGenerator.buildIdentityAnalysisPrompt(anyList(), any(), any()))
                    .thenReturn("prompt");
            when(aiProvider.generateRawResponse("prompt")).thenReturn(VALID_IDENTITY_JSON);

            analyzer.analyzeIfThresholdReached(SESSION_ID, FAM_ID, MEM_ID, 5, ctx);

            verify(identityProfileService).update(eq(MEM_ID), eq("DIRECT"), eq(4), eq(3), eq("LOW"), any(), any());
        }

        @Test
        @DisplayName("turno 5, memberId=null → no dispara análisis de identidad")
        void turn5_nullMemberId_noIdentityAnalysis() {
            analyzer.analyzeIfThresholdReached(SESSION_ID, FAM_ID, null, 5, null);

            verifyNoInteractions(chatMessageRepository, identityProfileService);
        }

        @Test
        @DisplayName("turno 5, menos de 3 mensajes → no llama a AI")
        void turn5_fewMessages_noAiCall() {
            when(chatMessageRepository.findUserMessageContentsBySessionId(SESSION_ID))
                    .thenReturn(List.of("msg1", "msg2")); // solo 2

            analyzer.analyzeIfThresholdReached(SESSION_ID, FAM_ID, MEM_ID, 5, null);

            verifyNoInteractions(aiProvider, identityProfileService);
        }

        @Test
        @DisplayName("turno 5, AI falla → no propaga excepción")
        void turn5_aiThrows_silentFailure() {
            AiContext ctx = mock(AiContext.class);
            when(chatMessageRepository.findUserMessageContentsBySessionId(SESSION_ID))
                    .thenReturn(List.of("m1", "m2", "m3"));
            when(promptGenerator.buildIdentityAnalysisPrompt(anyList(), any(), any()))
                    .thenReturn("p");
            when(aiProvider.generateRawResponse("p")).thenThrow(new RuntimeException("AI error"));

            // No debe propagarse
            analyzer.analyzeIfThresholdReached(SESSION_ID, FAM_ID, MEM_ID, 5, ctx);

            verifyNoInteractions(identityProfileService);
        }
    }

    @Nested
    @DisplayName("analyzeIfThresholdReached — memoria (cada 10 turnos)")
    class MemoryConsolidation {

        @Test
        @DisplayName("turno 10, familyId válido → consolida memoria")
        void turn10_triggersMemoryConsolidation() {
            AiContext ctx = mock(AiContext.class);
            // turno 10: también es múltiplo de 5 → dispara identidad también
            when(chatMessageRepository.findUserMessageContentsBySessionId(SESSION_ID))
                    .thenReturn(List.of("m1", "m2", "m3"));
            when(promptGenerator.buildIdentityAnalysisPrompt(anyList(), any(), any()))
                    .thenReturn("p");
            when(aiProvider.generateRawResponse("p")).thenReturn(VALID_IDENTITY_JSON);

            analyzer.analyzeIfThresholdReached(SESSION_ID, FAM_ID, MEM_ID, 10, ctx);

            verify(sessionMemoryConsolidator).consolidate(SESSION_ID, FAM_ID, ctx);
        }

        @Test
        @DisplayName("turno 10, familyId=null → no consolida memoria")
        void turn10_nullFamilyId_noConsolidation() {
            AiContext ctx = mock(AiContext.class);
            // turnCount=10 es múltiplo de 5 → también dispara identidad
            when(chatMessageRepository.findUserMessageContentsBySessionId(SESSION_ID))
                    .thenReturn(List.of("m1", "m2", "m3"));
            when(promptGenerator.buildIdentityAnalysisPrompt(anyList(), any(), any()))
                    .thenReturn("p");
            when(aiProvider.generateRawResponse("p")).thenReturn(VALID_IDENTITY_JSON);

            analyzer.analyzeIfThresholdReached(SESSION_ID, null, MEM_ID, 10, ctx);

            verifyNoInteractions(sessionMemoryConsolidator);
        }
    }
}
