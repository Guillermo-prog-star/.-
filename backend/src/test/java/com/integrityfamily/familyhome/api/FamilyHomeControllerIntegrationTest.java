package com.integrityfamily.familyhome.api;

import com.integrityfamily.domain.User;
import com.integrityfamily.domain.repository.UserRepository;
import com.integrityfamily.dto.home.*;
import com.integrityfamily.familyhome.application.FamilyHomeProjectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class FamilyHomeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FamilyHomeProjectionService projectionService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private com.integrityfamily.familyhome.port.FamilyMembershipQueryPort membershipPort;

    @MockBean
    private com.integrityfamily.familyhome.port.FamilyJourneyQueryPort journeyPort;

    @MockBean
    private com.integrityfamily.familyhome.port.FamilyProfileQueryPort profilePort;

    @MockBean
    private com.integrityfamily.familyhome.port.FamilyAssessmentQueryPort assessmentPort;

    @MockBean
    private com.integrityfamily.familyhome.port.FamilyInterventionQueryPort interventionPort;

    @MockBean
    private com.integrityfamily.familyhome.port.FamilyNarrativeQueryPort narrativePort;

    @MockBean
    private com.integrityfamily.familyhome.port.FamilySafetyQueryPort safetyPort;

    @MockBean
    private com.integrityfamily.familyhome.port.FamilyMemoryQueryPort memoryPort;

    private User mockUser;
    private final UUID familyId = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @BeforeEach
    public void setUp() {
        mockUser = new User();
        mockUser.setId(12345L);
        mockUser.setEmail("user@example.com");
        mockUser.setFullName("John Doe");

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(mockUser));

        ViewerContext viewer = new ViewerContext(UUID.randomUUID(), ViewerRole.ADULT_MEMBER, List.of("READ"));
        FamilyContext family = new FamilyContext(familyId, "Lopez Family", null);
        JourneyContext journey = new JourneyContext(JourneyStage.NEW_FAMILY, new ProgressBlock(0, 4, 0), null);
        SafetyPresentation safety = new SafetyPresentation(SafetyMode.NONE, null, null, List.of());
        ResponseMetadata meta = new ResponseMetadata(Instant.now(), Instant.now().plusSeconds(3600), "1.0", DataStatus.FRESH, UUID.randomUUID().toString());
        ModuleAvailability modules = new ModuleAvailability(ModuleStatus.AVAILABLE, ModuleStatus.NOT_APPLICABLE, ModuleStatus.NOT_APPLICABLE);

        OnboardingHomeView view = new OnboardingHomeView(
            "0.9.0-Candidate",
            ViewType.ONBOARDING,
            viewer,
            family,
            journey,
            safety,
            meta,
            modules
        );

        when(projectionService.project(eq(familyId), any(), any())).thenReturn(view);
    }

    @Test
    @WithMockUser(username = "user@example.com")
    public void testGetFamilyHome_SuccessfulThroughSecurityChain() throws Exception {
        mockMvc.perform(get("/api/v1/families/{familyId}/home", familyId)
                .header("X-Family-Home-Contract-Version", "0.9.0-Candidate"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Family-Home-Contract-Version", "0.9.0-Candidate"))
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$.viewType").value("ONBOARDING"));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    public void testGetFamilyHome_InvalidCorrelationIdFormat_RegeneratesValidUUID() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/families/{familyId}/home", familyId)
                .header("X-Correlation-Id", "abc123"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-Id"))
                .andReturn();

        String returnedCorrelationId = result.getResponse().getHeader("X-Correlation-Id");
        assertNotNull(returnedCorrelationId);
        assertEquals(36, returnedCorrelationId.length());
        assertDoesNotThrow(() -> UUID.fromString(returnedCorrelationId));
        assertNotEquals("abc123", returnedCorrelationId);
    }

    @Test
    @WithMockUser(username = "user@example.com")
    public void testGetFamilyHome_ExtremelyLongCorrelationId_RegeneratesValidUUID() throws Exception {
        String longCorrelationId = "a".repeat(100);
        MvcResult result = mockMvc.perform(get("/api/v1/families/{familyId}/home", familyId)
                .header("X-Correlation-Id", longCorrelationId))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-Id"))
                .andReturn();

        String returnedCorrelationId = result.getResponse().getHeader("X-Correlation-Id");
        assertNotNull(returnedCorrelationId);
        assertEquals(36, returnedCorrelationId.length());
        assertDoesNotThrow(() -> UUID.fromString(returnedCorrelationId));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    public void testGetFamilyHome_CorrelationIdReplay_IsAccepted() throws Exception {
        UUID correlationId = UUID.randomUUID();

        // First request
        mockMvc.perform(get("/api/v1/families/{familyId}/home", familyId)
                .header("X-Correlation-Id", correlationId.toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", correlationId.toString()));

        // Replay (second request with same correlation ID)
        mockMvc.perform(get("/api/v1/families/{familyId}/home", familyId)
                .header("X-Correlation-Id", correlationId.toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", correlationId.toString()));
    }
}
