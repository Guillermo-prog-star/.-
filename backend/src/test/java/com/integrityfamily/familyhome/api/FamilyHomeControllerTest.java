package com.integrityfamily.familyhome.api;

import com.integrityfamily.domain.repository.UserRepository;
import com.integrityfamily.dto.home.*;
import com.integrityfamily.familyhome.application.FamilyHomeProjectionService;
import com.integrityfamily.familyhome.application.exception.FamilyHomeAccessDeniedException;
import com.integrityfamily.familyhome.application.exception.FamilyHomeDataIncompleteException;
import com.integrityfamily.security.CustomUserDetailsService;
import com.integrityfamily.security.JwtTokenProvider;
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
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = FamilyHomeController.class)
@Import({FamilyHomeExceptionHandler.class, CorrelationIdFilter.class})
@ActiveProfiles("test")
@SuppressWarnings("deprecation")
public class FamilyHomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FamilyHomeProjectionService projectionService;

    @MockBean
    private AuthenticatedUserResolver authenticatedUserResolver;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private jakarta.persistence.EntityManager entityManager;

    @MockBean
    private com.integrityfamily.security.TenantInterceptor tenantInterceptor;

    @org.junit.jupiter.api.BeforeEach
    public void setUp() throws Exception {
        when(tenantInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    @WithMockUser
    public void testGetFamilyHome_Success_Onboarding() throws Exception {
        UUID familyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(authenticatedUserResolver.requireAuthenticatedUserId()).thenReturn(userId);

        ViewerContext viewer = new ViewerContext(userId, ViewerRole.ADULT_MEMBER, List.of("READ"));
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

        when(projectionService.project(eq(familyId), eq(userId), any())).thenReturn(view);

        mockMvc.perform(get("/api/v1/families/{familyId}/home", familyId)
                .header("X-Family-Home-Contract-Version", "0.9.0-Candidate"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Family-Home-Contract-Version", "0.9.0-Candidate"))
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.viewType").value("ONBOARDING"))
                .andExpect(jsonPath("$.family.displayName").value("Lopez Family"));
    }

    @Test
    @WithMockUser
    public void testGetFamilyHome_AccessDenied_Returns404() throws Exception {
        UUID familyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(authenticatedUserResolver.requireAuthenticatedUserId()).thenReturn(userId);
        when(projectionService.project(eq(familyId), eq(userId), any()))
                .thenThrow(new FamilyHomeAccessDeniedException("Access denied"));

        mockMvc.perform(get("/api/v1/families/{familyId}/home", familyId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FAMILY_HOME_NOT_FOUND"))
                .andExpect(jsonPath("$.title").value("Hogar familiar no disponible"));
    }

    @Test
    @WithMockUser
    public void testGetFamilyHome_InvalidUUID_Returns400() throws Exception {
        mockMvc.perform(get("/api/v1/families/not-a-uuid/home"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FAMILY_ID"))
                .andExpect(jsonPath("$.title").value("Identificador de familia inválido"));
    }

    @Test
    @WithMockUser
    public void testGetFamilyHome_UnsupportedVersion_Returns406() throws Exception {
        UUID familyId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/families/{familyId}/home", familyId)
                .header("X-Family-Home-Contract-Version", "0.8.0"))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.code").value("FAMILY_HOME_CONTRACT_NOT_SUPPORTED"));
    }

    @Test
    @WithMockUser
    public void testGetFamilyHome_DataIncomplete_Returns503() throws Exception {
        UUID familyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(authenticatedUserResolver.requireAuthenticatedUserId()).thenReturn(userId);
        when(projectionService.project(eq(familyId), eq(userId), any()))
                .thenThrow(new FamilyHomeDataIncompleteException("Active sprint missing"));

        mockMvc.perform(get("/api/v1/families/{familyId}/home", familyId))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("FAMILY_HOME_TEMPORARILY_UNAVAILABLE"));
    }
}
