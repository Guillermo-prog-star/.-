package com.integrityfamily.adaptive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.security.CustomUserDetailsService;
import com.integrityfamily.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdaptiveController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AdaptiveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private AdaptivePlanService adaptivePlanService;

    @MockitoBean
    private jakarta.persistence.EntityManager entityManager;

    private AdaptivePlanContext validContext;
    private AdaptiveAdjustment sampleAdjustment;

    @BeforeEach
    void setUp() {
        validContext = new AdaptivePlanContext(1L, 35.0, 0, 80, 80, 0.0);
        sampleAdjustment = new AdaptiveAdjustment(1L, AdaptiveRuleType.REDUCE_LOAD, "Adherencia baja");
    }

    @Test
    @DisplayName("QA API Contrato: POST /api/v1/adaptive/evaluate debe devolver lista de propuestas JSON")
    void shouldEvaluateAndReturnProposals() throws Exception {
        Mockito.when(adaptivePlanService.evaluate(any(AdaptivePlanContext.class)))
                .thenReturn(List.of(sampleAdjustment));

        mockMvc.perform(post("/api/v1/adaptive/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validContext)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].ruleType").value("REDUCE_LOAD"))
                .andExpect(jsonPath("$.data[0].status").value("PROPOSED"))
                .andExpect(jsonPath("$.data[0].reason").value("Adherencia baja"));
    }

    @Test
    @DisplayName("QA API Contrato: POST /api/v1/adaptive/approve debe cambiar estado a APPROVED")
    void shouldApproveAdjustment() throws Exception {
        sampleAdjustment.approve();
        Mockito.when(adaptivePlanService.approve(any(AdaptiveAdjustment.class)))
                .thenReturn(sampleAdjustment);

        mockMvc.perform(post("/api/v1/adaptive/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleAdjustment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    @DisplayName("QA API Contrato: POST /api/v1/adaptive/apply debe cambiar estado a APPLIED")
    void shouldApplyAdjustment() throws Exception {
        sampleAdjustment.approve();
        sampleAdjustment.apply();
        Mockito.when(adaptivePlanService.apply(any(AdaptiveAdjustment.class)))
                .thenReturn(sampleAdjustment);

        mockMvc.perform(post("/api/v1/adaptive/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleAdjustment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPLIED"));
    }
}
