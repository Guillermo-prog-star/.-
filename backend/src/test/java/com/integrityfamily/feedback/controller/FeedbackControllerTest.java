package com.integrityfamily.feedback.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.domain.Feedback;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FeedbackRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = FeedbackController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FeedbackController — Feedback Familiar")
class FeedbackControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean FeedbackRepository feedbackRepository;
    @MockitoBean FamilyRepository   familyRepository;
    @MockitoBean com.integrityfamily.security.JwtAuthenticationFilter jwtAuthFilter;
    @MockitoBean com.integrityfamily.security.TenantInterceptor       tenantInterceptor;

    private final Family family = Family.builder().id(1L).name("Familia López").currentMilestone("W2").build();

    @BeforeEach
    void setUp() throws Exception {
        Mockito.lenient().when(tenantInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    // ─── POST /api/feedback/send ───────────────────────────────────────────────

    @Nested
    @DisplayName("submit()")
    class Submit {

        @Test
        @DisplayName("familia encontrada → feedback guardado con milestoneAtMoment del ICF actual")
        void familyFound_feedbackSavedWithMilestone() throws Exception {
            Feedback saved = Feedback.builder()
                    .id(1L).family(family).score(5)
                    .comment("Muy buena experiencia").type("SPRINT")
                    .milestoneAtMoment("W2").build();

            Mockito.when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            Mockito.when(feedbackRepository.save(any())).thenReturn(saved);

            String body = "{\"familyId\":1,\"score\":5,\"comment\":\"Muy buena experiencia\",\"type\":\"SPRINT\"}";

            mockMvc.perform(post("/api/feedback/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.score").value(5))
                    .andExpect(jsonPath("$.data.milestoneAtMoment").value("W2"));
        }

        @Test
        @DisplayName("familia no encontrada → 404")
        void familyNotFound_returns404() throws Exception {
            Mockito.when(familyRepository.findById(99L)).thenReturn(Optional.empty());

            mockMvc.perform(post("/api/feedback/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"familyId\":99,\"score\":3,\"comment\":\"ok\",\"type\":\"GENERAL\"}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("score negativo enviado → guardado (no hay validación de rango en controller)")
        void negativeScore_savedAsIs() throws Exception {
            Feedback saved = Feedback.builder().id(2L).family(family).score(-1).build();
            Mockito.when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            Mockito.when(feedbackRepository.save(any())).thenReturn(saved);

            mockMvc.perform(post("/api/feedback/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"familyId\":1,\"score\":-1}"))
                    .andExpect(status().isOk());
        }
    }

    // ─── GET /api/feedback/all ─────────────────────────────────────────────────

    @Nested
    @DisplayName("getAll()")
    class GetAll {

        @Test
        @DisplayName("sin registros → data es array vacío")
        void noFeedback_returnsEmptyArray() throws Exception {
            Mockito.when(feedbackRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

            mockMvc.perform(get("/api/feedback/all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("con registros → retorna lista ordenada por fecha desc")
        void withFeedback_returnsList() throws Exception {
            Feedback fb1 = Feedback.builder().id(1L).score(5).type("SPRINT").family(family).build();
            Feedback fb2 = Feedback.builder().id(2L).score(3).type("GENERAL").family(family).build();
            Mockito.when(feedbackRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(fb1, fb2));

            mockMvc.perform(get("/api/feedback/all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].id").value(1))
                    .andExpect(jsonPath("$.data[1].id").value(2));
        }
    }
}
