package com.integrityfamily.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.cognitive.service.FamilyMemoryService;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMemory;
import com.integrityfamily.domain.ConversationSession;
import com.integrityfamily.domain.repository.ChatMessageRepository;
import com.integrityfamily.domain.repository.ConversationSessionRepository;
import com.integrityfamily.domain.repository.FamilyMemoryRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.ai.dto.AiContext;
import com.integrityfamily.ai.provider.AiProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionMemoryConsolidator")
class SessionMemoryConsolidatorTest {

    @Mock ConversationSessionRepository sessionRepository;
    @Mock ChatMessageRepository         chatMessageRepository;
    @Mock FamilyRepository              familyRepository;
    @Mock FamilyMemoryRepository        memoryRepository;
    @Mock FamilyMemoryService           familyMemoryService;
    @Mock AiProvider                    aiProvider;
    @Mock PromptGenerator               promptGenerator;
    @Spy  ObjectMapper                  objectMapper = new ObjectMapper();
    @InjectMocks SessionMemoryConsolidator consolidator;

    private static final long SESSION_ID = 1L;
    private static final long FAM_ID     = 2L;
    private static final long MEM_ID     = 10L;

    private static final String VALID_JSON = """
            {
              "themes": ["comunicación", "confianza"],
              "emotionalSummary": "Sesión constructiva",
              "memberState": "ENGAGED",
              "progressSignals": ["avance notable"],
              "recommendedFollowUp": "Continuar ejercicio de escucha",
              "importanceScore": 0.8
            }
            """;

    private final Family family = Family.builder().id(FAM_ID).name("García").build();

    private ConversationSession session() {
        return ConversationSession.builder()
                .id(SESSION_ID).familyId(FAM_ID).memberId(MEM_ID)
                .goal("GENERAL").emotionalState("STABLE")
                .build();
    }

    private void stubHappyPath(AiContext ctx) {
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session()));
        when(chatMessageRepository.findUserMessageContentsBySessionId(SESSION_ID))
                .thenReturn(List.of("msg1", "msg2"));
        when(promptGenerator.buildSessionSynthesisPrompt(anyList(), any(), any(), any(), any()))
                .thenReturn("prompt");
        when(aiProvider.generateRawResponse("prompt")).thenReturn(VALID_JSON);
        when(memoryRepository.findByFamilyIdAndSemanticKeyOrderByCreatedAtDesc(FAM_ID, SessionMemoryConsolidator.SEMANTIC_KEY))
                .thenReturn(List.of());
        when(familyRepository.getReferenceById(FAM_ID)).thenReturn(family);
        when(memoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(familyMemoryService).consolidateSemanticPattern(FAM_ID, SessionMemoryConsolidator.SEMANTIC_KEY);
    }

    // ── Guards ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("guards")
    class Guards {

        @Test
        @DisplayName("sessionId=null → noop")
        void nullSessionId_noop() {
            consolidator.consolidate(null, FAM_ID, null);

            verifyNoInteractions(sessionRepository, chatMessageRepository, memoryRepository);
        }

        @Test
        @DisplayName("familyId=null → noop")
        void nullFamilyId_noop() {
            consolidator.consolidate(SESSION_ID, null, null);

            verifyNoInteractions(sessionRepository, chatMessageRepository, memoryRepository);
        }

        @Test
        @DisplayName("sesión no encontrada → noop")
        void sessionNotFound_noop() {
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

            consolidator.consolidate(SESSION_ID, FAM_ID, null);

            verifyNoInteractions(chatMessageRepository, memoryRepository);
        }

        @Test
        @DisplayName("sin mensajes de usuario → noop")
        void noMessages_noop() {
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session()));
            when(chatMessageRepository.findUserMessageContentsBySessionId(SESSION_ID))
                    .thenReturn(List.of());

            consolidator.consolidate(SESSION_ID, FAM_ID, null);

            verifyNoInteractions(aiProvider, memoryRepository);
        }
    }

    // ── Consolidación exitosa ─────────────────────────────────────────────────

    @Nested
    @DisplayName("consolidación exitosa")
    class HappyPath {

        @Test
        @DisplayName("sin memoria previa → nueva FamilyMemory EPISODIC guardada")
        void noExistingMemory_newMemorySaved() {
            AiContext ctx = mock(AiContext.class);
            stubHappyPath(ctx);

            consolidator.consolidate(SESSION_ID, FAM_ID, ctx);

            verify(memoryRepository).save(argThat(m ->
                    m.getFamily().getId().equals(FAM_ID)
                    && FamilyMemory.MemoryType.EPISODIC.equals(m.getMemoryType())
                    && SessionMemoryConsolidator.SEMANTIC_KEY.equals(m.getSemanticKey())
                    && Long.valueOf(SESSION_ID).equals(m.getSourceId())));
        }

        @Test
        @DisplayName("sin memoria previa → consolidateSemanticPattern invocado")
        void noExistingMemory_semanticPatternConsolidated() {
            AiContext ctx = mock(AiContext.class);
            stubHappyPath(ctx);

            consolidator.consolidate(SESSION_ID, FAM_ID, ctx);

            verify(familyMemoryService).consolidateSemanticPattern(FAM_ID, SessionMemoryConsolidator.SEMANTIC_KEY);
        }

        @Test
        @DisplayName("context.activeMember()=null → usa 'el miembro'/'FAMILIA' como defaults")
        void nullActiveMember_defaultNames() {
            AiContext ctx = mock(AiContext.class); // activeMember() → null por defecto
            stubHappyPath(ctx);

            consolidator.consolidate(SESSION_ID, FAM_ID, ctx);

            verify(promptGenerator).buildSessionSynthesisPrompt(
                    anyList(), eq("FAMILIA"), eq("el miembro"), any(), any());
        }

        @Test
        @DisplayName("memoria existente para misma sesión → actualiza en lugar de crear nueva")
        void existingMemoryForSession_updatesInsteadOfCreating() {
            AiContext ctx = mock(AiContext.class);
            FamilyMemory existing = FamilyMemory.builder()
                    .id(99L).sourceId(SESSION_ID)
                    .content("{}").importanceScore(0.5)
                    .build();
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session()));
            when(chatMessageRepository.findUserMessageContentsBySessionId(SESSION_ID))
                    .thenReturn(List.of("msg1"));
            when(promptGenerator.buildSessionSynthesisPrompt(anyList(), any(), any(), any(), any()))
                    .thenReturn("p");
            when(aiProvider.generateRawResponse("p")).thenReturn(VALID_JSON);
            when(memoryRepository.findByFamilyIdAndSemanticKeyOrderByCreatedAtDesc(FAM_ID, SessionMemoryConsolidator.SEMANTIC_KEY))
                    .thenReturn(List.of(existing));
            when(memoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(familyMemoryService).consolidateSemanticPattern(any(), any());

            consolidator.consolidate(SESSION_ID, FAM_ID, ctx);

            // Saves the existing entity (not a new one) and importanceScore updated to 0.8
            verify(memoryRepository).save(argThat(m -> m.getId() == 99L));
            assertThat(existing.getImportanceScore()).isEqualTo(0.8);
        }
    }

    // ── Fallos silenciosos ────────────────────────────────────────────────────

    @Nested
    @DisplayName("fallos silenciosos")
    class SilentFailures {

        @Test
        @DisplayName("AI lanza excepción → no propaga, FamilyMemory no guardada")
        void aiThrows_noMemorySaved() {
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session()));
            when(chatMessageRepository.findUserMessageContentsBySessionId(SESSION_ID))
                    .thenReturn(List.of("msg1"));
            when(promptGenerator.buildSessionSynthesisPrompt(anyList(), any(), any(), any(), any()))
                    .thenReturn("p");
            when(aiProvider.generateRawResponse("p")).thenThrow(new RuntimeException("AI down"));

            consolidator.consolidate(SESSION_ID, FAM_ID, mock(AiContext.class));

            verifyNoInteractions(memoryRepository);
        }
    }
}
