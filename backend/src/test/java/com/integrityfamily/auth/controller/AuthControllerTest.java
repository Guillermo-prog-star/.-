package com.integrityfamily.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.auth.dto.*;
import com.integrityfamily.auth.service.AuthService;
import com.integrityfamily.plan.scheduler.PlanComplianceScheduler;
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

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Aislamos el controlador de los filtros de seguridad para pruebas unitarias limpias
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
        Mockito.lenient().when(tenantInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("Debe autenticar exitosamente y devolver JWT y Refresh Token")
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest request = new LoginRequest("william@integrityfamily.com", "Password123!");
        UserResponse userResponse = new UserResponse(1L, "william@integrityfamily.com", "William Lopez", "ROLE_ADMIN", 100L, "Familia Lopez Rivera");
        LoginResponse response = new LoginResponse("mock-jwt-token", "mock-refresh-token", 3600000L, userResponse);

        Mockito.when(authService.login(any(LoginRequest.class), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.refreshToken").value("mock-refresh-token"))
                .andExpect(jsonPath("$.user.email").value("william@integrityfamily.com"))
                .andExpect(jsonPath("$.user.role").value("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("Debe rechazar la petición de login cuando el email está vacío")
    void shouldFailLoginWhenEmailIsBlank() throws Exception {
        LoginRequest request = new LoginRequest("", "Password123!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Debe rechazar la petición de login cuando el email tiene formato inválido")
    void shouldFailLoginWhenEmailIsInvalid() throws Exception {
        LoginRequest request = new LoginRequest("correo-invalido", "Password123!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Debe refrescar exitosamente el token de acceso usando un Refresh Token válido")
    void shouldRefreshTokenSuccessfully() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
        UserResponse userResponse = new UserResponse(1L, "william@integrityfamily.com", "William Lopez", "ROLE_ADMIN", 100L, "Familia Lopez Rivera");
        LoginResponse response = new LoginResponse("new-jwt-token", "valid-refresh-token", 3600000L, userResponse);

        Mockito.when(authService.refreshToken(any(RefreshTokenRequest.class), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new-jwt-token"))
                .andExpect(jsonPath("$.refreshToken").value("valid-refresh-token"));
    }

    @Test
    @DisplayName("Debe rechazar la petición de refresco cuando el token está vacío")
    void shouldFailRefreshTokenWhenTokenIsBlank() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("");

        mockMvc.perform(post("/api/auth/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
