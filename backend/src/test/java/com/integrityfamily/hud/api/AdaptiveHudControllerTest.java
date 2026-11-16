package com.integrityfamily.hud.api;

import com.integrityfamily.domain.repository.UserRepository;
import com.integrityfamily.dto.home.*;
import com.integrityfamily.familyhome.api.AuthenticatedUserResolver;
import com.integrityfamily.familyhome.api.FamilyHomeExceptionHandler;
import com.integrityfamily.familyhome.api.CorrelationIdFilter;
import com.integrityfamily.familyhome.application.exception.FamilyHomeAccessDeniedException;
import com.integrityfamily.hud.application.AdaptiveHudProjectionService;
import com.integrityfamily.hud.dto.FamilyHudView;
import com.integrityfamily.hud.dto.ProfessionalHudView;
import com.integrityfamily.hud.dto.HudType;
import com.integrityfamily.security.CustomUserDetailsService;
import com.integrityfamily.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
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

@WebMvcTest(controllers = AdaptiveHudController.class)
@Import({FamilyHomeExceptionHandler.class, CorrelationIdFilter.class})
@ActiveProfiles("test")
@SuppressWarnings("deprecation")
public class AdaptiveHudControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdaptiveHudProjectionService hudProjectionService;

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
    public void testGetFamilyHud_Success() throws Exception {
        UUID familyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(authenticatedUserResolver.requireAuthenticatedUserId()).thenReturn(userId);

        ViewerContext viewer = new ViewerContext(userId, ViewerRole.ADULT_MEMBER, List.of("VIEW_FAMILY_HUD"));
        ResponseMetadata meta = new ResponseMetadata(Instant.now(), Instant.now().plusSeconds(3600), "1.0", DataStatus.FRESH, UUID.randomUUID().toString());

        FamilyHudView view = new FamilyHudView(
            "1.0.0-AdaptiveHUD",
            HudType.FAMILY,
            viewer,
            familyId,
            List.of("Hoy", "Somos"),
            "Todo en orden",
            "Tranquilo",
            "Fuerte",
            null,
            null,
            Map.of(),
            null,
            meta
        );

        when(hudProjectionService.project(eq(familyId), eq(userId), any(), eq(HudType.FAMILY))).thenReturn(view);

        mockMvc.perform(get("/api/v1/families/{familyId}/hud/family", familyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hudType").value("FAMILY"))
                .andExpect(jsonPath("$.statusMessage").value("Todo en orden"));
    }

    @Test
    @WithMockUser
    public void testGetProfessionalHud_Success() throws Exception {
        UUID familyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(authenticatedUserResolver.requireAuthenticatedUserId()).thenReturn(userId);

        ViewerContext viewer = new ViewerContext(userId, ViewerRole.SUPPORT_PERSON, List.of("VIEW_PROFESSIONAL_HUD"));
        ResponseMetadata meta = new ResponseMetadata(Instant.now(), Instant.now().plusSeconds(3600), "1.0", DataStatus.FRESH, UUID.randomUUID().toString());

        ProfessionalHudView view = new ProfessionalHudView(
            "1.0.0-AdaptiveHUD",
            HudType.PROFESSIONAL,
            viewer,
            familyId,
            List.of("Resumen", "Intervención"),
            "ICaF: 63",
            "Capacidad: 0.61",
            "Riesgo: Moderado",
            null,
            null,
            Map.of(),
            List.of("Nota 1"),
            List.of("Intervención 1"),
            null,
            meta
        );

        when(hudProjectionService.project(eq(familyId), eq(userId), any(), eq(HudType.PROFESSIONAL))).thenReturn(view);

        mockMvc.perform(get("/api/v1/families/{familyId}/hud/professional", familyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hudType").value("PROFESSIONAL"))
                .andExpect(jsonPath("$.icafStatus").value("ICaF: 63"));
    }

    @Test
    @WithMockUser
    public void testGetFamilyHud_AccessDenied() throws Exception {
        UUID familyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(authenticatedUserResolver.requireAuthenticatedUserId()).thenReturn(userId);
        when(hudProjectionService.project(eq(familyId), eq(userId), any(), eq(HudType.FAMILY)))
                .thenThrow(new FamilyHomeAccessDeniedException("Access denied"));

        mockMvc.perform(get("/api/v1/families/{familyId}/hud/family", familyId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    public void testGetProfessionalHud_AccessDenied() throws Exception {
        UUID familyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(authenticatedUserResolver.requireAuthenticatedUserId()).thenReturn(userId);
        when(hudProjectionService.project(eq(familyId), eq(userId), any(), eq(HudType.PROFESSIONAL)))
                .thenThrow(new FamilyHomeAccessDeniedException("Access denied"));

        mockMvc.perform(get("/api/v1/families/{familyId}/hud/professional", familyId))
                .andExpect(status().isNotFound());
     }

     @Test
     @WithMockUser
     public void testGetFamilyHud_InfrastructureFailure() throws Exception {
         UUID familyId = UUID.randomUUID();
         UUID userId = UUID.randomUUID();

         when(authenticatedUserResolver.requireAuthenticatedUserId()).thenReturn(userId);
         when(hudProjectionService.project(eq(familyId), eq(userId), any(), eq(HudType.FAMILY)))
                 .thenThrow(new RuntimeException("Database connection failure"));

         mockMvc.perform(get("/api/v1/families/{familyId}/hud/family", familyId))
                 .andExpect(status().isInternalServerError());
     }
}
