package com.integrityfamily.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.ai.service.AiService;
import com.integrityfamily.ai.service.ConversationSessionService;
import com.integrityfamily.common.security.SecurityValidator;
import com.integrityfamily.domain.ChatMessage;
import com.integrityfamily.domain.ConversationSession;
import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.EvaluationDimensionScore;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.ChatMessageRepository;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ChatController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ChatController — Contratos de Respuesta")
class ChatControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean ChatMessageRepository         chatMessageRepository;
    @MockitoBean FamilyRepository              familyRepository;
    @MockitoBean EvaluationRepository          evaluationRepository;
    @MockitoBean AiService                     aiService;
    @MockitoBean SecurityValidator             securityValidator;
    @MockitoBean ConversationSessionService    conversationSessionService;
    @MockitoBean com.integrityfamily.security.JwtAuthenticationFilter jwtAuthFilter;
    @MockitoBean com.integrityfamily.security.TenantInterceptor       tenantInterceptor;

    private final Family family = Family.builder().id(1L).name("Familia Test").build();

    @BeforeEach
    void setUp() throws Exception {
        Mockito.lenient().when(tenantInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    // ─── GET /api/chat/family/{familyId} ─────────────────────────────────────

    @Nested
    @DisplayName("getHistory()")
    class GetHistory {

        @Test
        @DisplayName("sin mensajes → data es array vacío")
        void emptyHistory_returnsEmptyArray() throws Exception {
            Mockito.when(chatMessageRepository.findProjectedByFamilyIdOrderByCreatedAtAsc(1L))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/chat/family/1").principal(() -> "user@test.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    // ─── POST /api/chat/send/legacy ────────────────────────────────────────────

    @Nested
    @DisplayName("sendMessageLegacy()")
    class SendMessageLegacy {

        @Test
        @DisplayName("familia encontrada → respuesta de IA en data.content")
        void familyFound_aiResponseReturned() throws Exception {
            ChatMessage aiMsg = ChatMessage.builder().id(1L).content("Bienvenidos").family(family).build();
            Mockito.when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            Mockito.when(aiService.chat(eq("Hola"), eq(family), isNull())).thenReturn(aiMsg);

            mockMvc.perform(post("/api/chat/send/legacy")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"familyId\":1,\"message\":\"Hola\"}")
                    .principal(() -> "user@test.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").value("Bienvenidos"))
                    .andExpect(jsonPath("$.data.id").value(1));
        }

        @Test
        @DisplayName("familia no encontrada → 404")
        void familyNotFound_returns404() throws Exception {
            Mockito.when(familyRepository.findById(99L)).thenReturn(Optional.empty());

            mockMvc.perform(post("/api/chat/send/legacy")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"familyId\":99,\"message\":\"test\"}")
                    .principal(() -> "user@test.com"))
                    .andExpect(status().isNotFound());
        }
    }

    // ─── POST /api/chat/report/{evaluationId} ─────────────────────────────────

    @Nested
    @DisplayName("generateAutoReport()")
    class GenerateAutoReport {

        @Test
        @DisplayName("evaluación encontrada con dimensiones → insight retornado")
        void evalFound_insightReturned() throws Exception {
            EvaluationDimensionScore ds = EvaluationDimensionScore.builder()
                    .dimensionName("emociones").score(70.0).build();
            Evaluation eval = Evaluation.builder()
                    .id(1L).family(family).riskLevel("BAJO")
                    .dimensionScores(List.of(ds)).build();

            Mockito.when(evaluationRepository.findById(1L)).thenReturn(Optional.of(eval));
            Mockito.when(aiService.generateDashboardInsight(eq(family), any(), eq("BAJO")))
                    .thenReturn("Buen progreso emocional");

            mockMvc.perform(post("/api/chat/report/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value("Buen progreso emocional"));
        }

        @Test
        @DisplayName("riskLevel null → usa fallback 'MEDIUM'")
        void nullRiskLevel_usesMediumFallback() throws Exception {
            Evaluation eval = Evaluation.builder()
                    .id(2L).family(family).riskLevel(null)
                    .dimensionScores(List.of()).build();

            Mockito.when(evaluationRepository.findById(2L)).thenReturn(Optional.of(eval));
            Mockito.when(aiService.generateDashboardInsight(eq(family), any(), eq("MEDIUM")))
                    .thenReturn("Insight con MEDIUM");

            mockMvc.perform(post("/api/chat/report/2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value("Insight con MEDIUM"));
        }

        @Test
        @DisplayName("evaluación no encontrada → 404")
        void evalNotFound_returns404() throws Exception {
            Mockito.when(evaluationRepository.findById(99L)).thenReturn(Optional.empty());

            mockMvc.perform(post("/api/chat/report/99"))
                    .andExpect(status().isNotFound());
        }
    }

    // ─── GET /api/chat/session/active ─────────────────────────────────────────

    @Nested
    @DisplayName("getActiveSession()")
    class GetActiveSession {

        @Test
        @DisplayName("sin sesión activa → data es null")
        void noActiveSession_returnsNull() throws Exception {
            Mockito.when(conversationSessionService.getActiveSession(1L, null)).thenReturn(null);

            mockMvc.perform(get("/api/chat/session/active")
                    .param("familyId", "1")
                    .principal(() -> "user@test.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").doesNotExist());
        }

        @Test
        @DisplayName("sesión activa → retorna sessionId, goal y turnCount")
        void activeSession_returnsContext() throws Exception {
            ConversationSession session = ConversationSession.builder()
                    .id(5L).goal("Mejorar comunicación")
                    .emotionalState("POSITIVO").turnCount(3).build();
            Mockito.when(conversationSessionService.getActiveSession(1L, null)).thenReturn(session);

            mockMvc.perform(get("/api/chat/session/active")
                    .param("familyId", "1")
                    .principal(() -> "user@test.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.sessionId").value(5))
                    .andExpect(jsonPath("$.data.goal").value("Mejorar comunicación"))
                    .andExpect(jsonPath("$.data.turnCount").value(3));
        }
    }

    // ─── POST /api/chat/send ───────────────────────────────────────────────────

    @Nested
    @DisplayName("sendMessageWithContext()")
    class SendMessageWithContext {

        @Test
        @DisplayName("familia encontrada → chatWithTransformation invocado, respuesta en data.content")
        void familyFound_chatWithTransformationCalled() throws Exception {
            ChatMessage aiMsg = ChatMessage.builder().id(2L).content("Respuesta contextual").family(family).build();
            Mockito.when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            Mockito.when(aiService.chatWithTransformation(eq("Pregunta"), eq(family), isNull(), isNull()))
                    .thenReturn(aiMsg);

            mockMvc.perform(post("/api/chat/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"familyId\":1,\"message\":\"Pregunta\"}")
                    .principal(() -> "user@test.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").value("Respuesta contextual"));
        }

        @Test
        @DisplayName("familia no encontrada → 404")
        void familyNotFound_returns404() throws Exception {
            Mockito.when(familyRepository.findById(42L)).thenReturn(Optional.empty());

            mockMvc.perform(post("/api/chat/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"familyId\":42,\"message\":\"test\"}")
                    .principal(() -> "user@test.com"))
                    .andExpect(status().isNotFound());
        }
    }
}
