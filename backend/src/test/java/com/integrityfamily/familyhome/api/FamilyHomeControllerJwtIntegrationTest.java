package com.integrityfamily.familyhome.api;

import com.integrityfamily.domain.User;
import com.integrityfamily.domain.repository.UserRepository;
import com.integrityfamily.dto.home.*;
import com.integrityfamily.familyhome.application.FamilyHomeProjectionService;
import com.integrityfamily.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A diferencia de {@link FamilyHomeControllerIntegrationTest} (que usa {@code @WithMockUser},
 * inyectando la autenticación directo en el SecurityContext sin pasar por el filtro real),
 * esta suite ejercita la cadena HTTP completa: {@code SecurityFilterChain} real,
 * {@code JwtAuthenticationFilter} real parseando un JWT emitido por {@code JwtTokenProvider},
 * y {@code CustomUserDetailsService} + {@code SpringSecurityAuthenticatedUserResolver} reales
 * contra un usuario persistido en la BD de test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FamilyHomeControllerJwtIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @MockBean private FamilyHomeProjectionService projectionService;

    private final UUID familyId = UUID.randomUUID();

    @Test
    @DisplayName("JWT real válido + usuario existente → autentica por la cadena real y devuelve 200")
    void validJwt_authenticatesThroughRealChain_returns200() throws Exception {
        User user = userRepository.save(User.builder()
                .email("jwt-real@if.com")
                .passwordHash("irrelevant-hash")
                .fullName("JWT Real User")
                .build());
        String token = jwtTokenProvider.generate(user);

        when(projectionService.project(eq(familyId), any(), any())).thenReturn(sampleOnboardingView());

        mockMvc.perform(get("/api/v1/families/{familyId}/home", familyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewType").value("ONBOARDING"));
    }

    @Test
    @DisplayName("sin header Authorization → la cadena de seguridad real bloquea con 401/403")
    void noToken_isRejectedByRealSecurityChain() throws Exception {
        mockMvc.perform(get("/api/v1/families/{familyId}/home", familyId))
                .andExpect(result -> assertAuthRejected(result.getResponse().getStatus()));
    }

    @Test
    @DisplayName("JWT con firma inválida → el filtro real lo rechaza, no autentica")
    void tamperedJwt_isRejected() throws Exception {
        mockMvc.perform(get("/api/v1/families/{familyId}/home", familyId)
                        .header("Authorization", "Bearer not-a-real-jwt.tampered.value"))
                .andExpect(result -> assertAuthRejected(result.getResponse().getStatus()));
    }

    @Test
    @DisplayName("JWT válido pero de un usuario que ya no existe en BD → rechazado")
    void jwtForDeletedUser_isRejected() throws Exception {
        User user = userRepository.save(User.builder()
                .email("temp-user@if.com")
                .passwordHash("irrelevant-hash")
                .fullName("Temp User")
                .build());
        String token = jwtTokenProvider.generate(user);
        userRepository.delete(user);

        mockMvc.perform(get("/api/v1/families/{familyId}/home", familyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(result -> assertAuthRejected(result.getResponse().getStatus()));
    }

    private void assertAuthRejected(int status) {
        assertTrue(status == 401 || status == 403,
                "Se esperaba 401 o 403 de la cadena de seguridad real, fue: " + status);
    }

    private OnboardingHomeView sampleOnboardingView() {
        ViewerContext viewer = new ViewerContext(UUID.randomUUID(), ViewerRole.ADULT_MEMBER, List.of("READ"));
        FamilyContext family = new FamilyContext(familyId, "Familia JWT", null);
        JourneyContext journey = new JourneyContext(JourneyStage.NEW_FAMILY, new ProgressBlock(0, 4, 0), null);
        SafetyPresentation safety = new SafetyPresentation(SafetyMode.NONE, null, null, List.of());
        ResponseMetadata meta = new ResponseMetadata(
                Instant.now(), Instant.now().plusSeconds(3600), "0.9.0-Candidate",
                DataStatus.FRESH, UUID.randomUUID().toString());
        ModuleAvailability modules = new ModuleAvailability(
                ModuleStatus.AVAILABLE, ModuleStatus.NOT_APPLICABLE, ModuleStatus.NOT_APPLICABLE);

        return new OnboardingHomeView(
                "0.9.0-Candidate",
                ViewType.ONBOARDING,
                viewer,
                family,
                journey,
                safety,
                meta,
                modules
        );
    }
}
