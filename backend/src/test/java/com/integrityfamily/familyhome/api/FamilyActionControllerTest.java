package com.integrityfamily.familyhome.api;

import com.integrityfamily.domain.repository.UserRepository;
import com.integrityfamily.dto.home.AcceptFirstSprintRequest;
import com.integrityfamily.dto.home.FamilyActionResult;
import com.integrityfamily.dto.home.JourneyStage;
import com.integrityfamily.familyhome.application.FamilyActionEngine;
import com.integrityfamily.familyhome.application.exception.FamilyHomeAccessDeniedException;
import com.integrityfamily.familyhome.application.exception.UnsupportedJourneyStageException;
import com.integrityfamily.security.CustomUserDetailsService;
import com.integrityfamily.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = FamilyActionController.class)
@Import(FamilyHomeExceptionHandler.class)
@ActiveProfiles("test")
@SuppressWarnings("deprecation")
class FamilyActionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private FamilyActionEngine actionEngine;
    @MockBean private AuthenticatedUserResolver authenticatedUserResolver;
    @MockBean private UserRepository userRepository;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private CustomUserDetailsService userDetailsService;
    @MockBean private jakarta.persistence.EntityManager entityManager;
    @MockBean private com.integrityfamily.security.TenantInterceptor tenantInterceptor;

    @org.junit.jupiter.api.BeforeEach
    void setUp() throws Exception {
        when(tenantInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    @WithMockUser
    void acceptFirstSprint_firstExecution_returns201() throws Exception {
        UUID familyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID sprintId = UUID.randomUUID();

        when(authenticatedUserResolver.requireAuthenticatedUserId()).thenReturn(userId);
        when(actionEngine.acceptFirstSprint(eq(familyId), eq(userId), any(), eq("idem-key-1")))
                .thenReturn(new FamilyActionResult("accept-first-sprint", "COMPLETED", sprintId, Instant.now(), false));

        mockMvc.perform(post("/api/v1/families/{familyId}/actions/accept-first-sprint", familyId)
                        .with(csrf())
                        .header("Idempotency-Key", "idem-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.action").value("accept-first-sprint"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.replayed").value(false));
    }

    @Test
    @WithMockUser
    void acceptFirstSprint_replayedWithSameIdempotencyKey_returns200() throws Exception {
        UUID familyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID sprintId = UUID.randomUUID();

        when(authenticatedUserResolver.requireAuthenticatedUserId()).thenReturn(userId);
        when(actionEngine.acceptFirstSprint(eq(familyId), eq(userId), any(), eq("idem-key-1")))
                .thenReturn(new FamilyActionResult("accept-first-sprint", "COMPLETED", sprintId, Instant.now(), true));

        mockMvc.perform(post("/api/v1/families/{familyId}/actions/accept-first-sprint", familyId)
                        .with(csrf())
                        .header("Idempotency-Key", "idem-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replayed").value(true));
    }

    @Test
    @WithMockUser
    void acceptFirstSprint_missingIdempotencyKey_returns400() throws Exception {
        UUID familyId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/families/{familyId}/actions/accept-first-sprint", familyId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void acceptFirstSprint_notAMember_returns404() throws Exception {
        UUID familyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(authenticatedUserResolver.requireAuthenticatedUserId()).thenReturn(userId);
        when(actionEngine.acceptFirstSprint(eq(familyId), eq(userId), any(), any()))
                .thenThrow(new FamilyHomeAccessDeniedException("not a member"));

        mockMvc.perform(post("/api/v1/families/{familyId}/actions/accept-first-sprint", familyId)
                        .with(csrf())
                        .header("Idempotency-Key", "idem-key-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FAMILY_HOME_NOT_FOUND"));
    }

    @Test
    @WithMockUser
    void acceptFirstSprint_wrongJourneyStage_returns409() throws Exception {
        UUID familyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(authenticatedUserResolver.requireAuthenticatedUserId()).thenReturn(userId);
        when(actionEngine.acceptFirstSprint(eq(familyId), eq(userId), any(), any()))
                .thenThrow(new UnsupportedJourneyStageException(JourneyStage.ACTIVE_HOME));

        mockMvc.perform(post("/api/v1/families/{familyId}/actions/accept-first-sprint", familyId)
                        .with(csrf())
                        .header("Idempotency-Key", "idem-key-3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FAMILY_HOME_STATE_CONFLICT"));
    }
}
