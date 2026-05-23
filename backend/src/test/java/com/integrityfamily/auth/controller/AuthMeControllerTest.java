package com.integrityfamily.auth.controller;

import com.integrityfamily.auth.dto.UserResponse;
import com.integrityfamily.auth.service.AuthService;
import com.integrityfamily.plan.scheduler.PlanComplianceScheduler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de la capa controlador para GET /api/auth/me.
 *
 * Cubre:
 *   1. Usuario autenticado → 200 con todos los campos de UserResponse
 *   2. Respuesta cuando el usuario no pertenece a ninguna familia (familyId y familyName null)
 *   3. Verificación de que authService.me() recibe el email correcto del Principal
 *   4. authService.me() lanza RuntimeException → 500
 *
 * Nota técnica sobre el setup:
 *   Se usa webAppContextSetup + springSecurity() en lugar de @AutoConfigureMockMvc(addFilters=false)
 *   porque en Spring Security 6 el SecurityContextHolderFilter (parte de la cadena) es quien
 *   carga el SecurityContext en SecurityContextHolder. Sin él, Authentication es siempre null.
 *   El JwtAuthenticationFilter mock se configura para delegar al chain sin hacer validación JWT.
 */
@WebMvcTest(controllers = AuthController.class)
@DisplayName("AuthController — GET /api/auth/me")
class AuthMeControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private PlanComplianceScheduler planComplianceScheduler;

    @MockitoBean
    private com.integrityfamily.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private com.integrityfamily.security.TenantInterceptor tenantInterceptor;

    @BeforeEach
    void setUp() throws Exception {
        // JwtAuthenticationFilter mock: delegar al siguiente filtro sin validar JWT
        Mockito.doAnswer(inv -> {
            HttpServletRequest  req   = inv.getArgument(0);
            HttpServletResponse resp  = inv.getArgument(1);
            FilterChain         chain = inv.getArgument(2);
            chain.doFilter(req, resp);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(
                any(HttpServletRequest.class),
                any(HttpServletResponse.class),
                any(FilterChain.class));

        // TenantInterceptor (HandlerInterceptor): siempre permitir
        Mockito.lenient()
               .when(tenantInterceptor.preHandle(any(), any(), any()))
               .thenReturn(true);

        // Construir MockMvc con la cadena de seguridad real para que
        // SecurityContextHolderFilter cargue el contexto en SecurityContextHolder
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  1. Usuario con familia
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Usuario autenticado → 200 con todos los campos de UserResponse")
    void shouldReturnCurrentUserProfile_whenAuthenticated() throws Exception {
        UserResponse response = new UserResponse(
                1L,
                "william@integrityfamily.com",
                "William Lopez",
                "ROLE_USER",
                42L,
                "Familia Lopez Rivera"
        );
        Mockito.when(authService.me("william@integrityfamily.com")).thenReturn(response);

        mockMvc.perform(get("/api/auth/me")
                        .with(user("william@integrityfamily.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("william@integrityfamily.com"))
                .andExpect(jsonPath("$.fullName").value("William Lopez"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.familyId").value(42))
                .andExpect(jsonPath("$.familyName").value("Familia Lopez Rivera"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  2. Usuario ADMIN sin familia asociada (familyId y familyName null)
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Admin sin familia → 200 con familyId y familyName ausentes en JSON (@JsonInclude NON_NULL)")
    void shouldReturnNullFamilyFields_whenAdminHasNoFamily() throws Exception {
        UserResponse adminResponse = new UserResponse(
                99L,
                "admin@integrityfamily.com",
                "Administrador Global",
                "ROLE_ADMIN",
                null,
                null
        );
        Mockito.when(authService.me("admin@integrityfamily.com")).thenReturn(adminResponse);

        mockMvc.perform(get("/api/auth/me")
                        .with(user("admin@integrityfamily.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.familyId").doesNotExist())
                .andExpect(jsonPath("$.familyName").doesNotExist());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  3. Verificación de delegación: el email del Principal llega al servicio
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Debe delegar al authService.me() con el email del Principal autenticado")
    void shouldCallAuthServiceMeWithPrincipalEmail() throws Exception {
        UserResponse response = new UserResponse(
                5L,
                "consultor@integrityfamily.com",
                "Consultor Familiar",
                "ROLE_USER",
                10L,
                "Familia Gomez"
        );
        Mockito.when(authService.me("consultor@integrityfamily.com")).thenReturn(response);

        mockMvc.perform(get("/api/auth/me")
                        .with(user("consultor@integrityfamily.com")))
                .andExpect(status().isOk());

        Mockito.verify(authService, Mockito.times(1)).me("consultor@integrityfamily.com");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  4. Usuario no encontrado en BD → 500 (RuntimeException sin handler específico)
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("authService.me() lanza RuntimeException → 500")
    void shouldReturn500_whenUserNotFoundInDatabase() throws Exception {
        Mockito.when(authService.me("ghost@integrityfamily.com"))
               .thenThrow(new RuntimeException("Usuario no encontrado"));

        mockMvc.perform(get("/api/auth/me")
                        .with(user("ghost@integrityfamily.com")))
                .andExpect(status().is5xxServerError());
    }
}
