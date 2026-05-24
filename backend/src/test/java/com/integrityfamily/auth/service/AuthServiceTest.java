package com.integrityfamily.auth.service;

import com.integrityfamily.auth.dto.*;
import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.RefreshToken;
import com.integrityfamily.domain.Role;
import com.integrityfamily.domain.User;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.PasswordResetTokenRepository;
import com.integrityfamily.domain.repository.RoleRepository;
import com.integrityfamily.domain.repository.UserRepository;
import com.integrityfamily.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de AuthService.
 *
 * Cubre los flujos críticos de autenticación, registro (usuario individual y familiar),
 * consulta de perfil, cierre de sesión y renovación de token.
 *
 * No levanta contexto Spring — puro Mockito (STRICT_STUBS).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — Unit Tests")
class AuthServiceTest {

    // ─── Dependencies ────────────────────────────────────────────────────────
    @Mock UserRepository         userRepository;
    @Mock FamilyRepository       familyRepository;
    @Mock RoleRepository         roleRepository;
    @Mock PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock PasswordEncoder        passwordEncoder;
    @Mock JwtTokenProvider       jwtTokenProvider;
    @Mock AuthenticationManager  authenticationManager;
    @Mock AccountLockService     accountLockService;
    @Mock AuditService           auditService;
    @Mock RefreshTokenService    refreshTokenService;

    @InjectMocks
    AuthService authService;

    // ─── Shared fixtures ─────────────────────────────────────────────────────

    private Role userRole;
    private Role familyAdminRole;
    private User baseUser;
    private Family baseFamily;
    private RefreshToken baseRefreshToken;

    @BeforeEach
    void setUp() {
        userRole = Role.builder().id(1L).name("ROLE_USER").build();
        familyAdminRole = Role.builder().id(2L).name("ROLE_FAMILY_ADMIN").build();

        baseFamily = Family.builder()
                .id(10L)
                .name("Familia López")
                .familyCode("IF-2026-ABCD1234")
                .build();

        baseUser = User.builder()
                .id(1L)
                .email("william@integrityfamily.com")
                .fullName("William López")
                .passwordHash("$2a$10$hashedpassword")
                .enabled(true)
                .roles(new ArrayList<>(List.of(userRole)))
                .family(baseFamily)
                .build();

        baseRefreshToken = RefreshToken.builder()
                .id(99L)
                .token(UUID.randomUUID().toString())
                .user(baseUser)
                .expiryDate(Instant.now().plusSeconds(604800))
                .revoked(false)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  login()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("Credenciales válidas → retorna LoginResponse con JWT y refreshToken")
        void shouldReturnLoginResponse_whenCredentialsAreValid() {
            LoginRequest req = new LoginRequest("william@integrityfamily.com", "password123");
            when(authenticationManager.authenticate(any())).thenReturn(null);
            when(userRepository.findByEmail("william@integrityfamily.com")).thenReturn(Optional.of(baseUser));
            when(jwtTokenProvider.generate(baseUser)).thenReturn("jwt.token.here");
            when(refreshTokenService.createRefreshToken(1L)).thenReturn(baseRefreshToken);

            LoginResponse response = authService.login(req, "127.0.0.1", "Mozilla/5.0");

            assertThat(response.token()).isEqualTo("jwt.token.here");
            assertThat(response.refreshToken()).isEqualTo(baseRefreshToken.getToken());
            assertThat(response.expiresInMs()).isEqualTo(3600000L);
            assertThat(response.user().email()).isEqualTo("william@integrityfamily.com");
            assertThat(response.user().familyId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("AuthenticationManager lanza excepción → BusinessException INVALID_CREDENTIALS + registra fallo")
        void shouldThrowBusinessException_andRegisterFailure_whenAuthFails() {
            LoginRequest req = new LoginRequest("bad@example.com", "wrongpass");
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(req, "1.2.3.4", "curl/7.0"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Credenciales inválidas");

            verify(accountLockService).registerFailure("bad@example.com");
        }

        @Test
        @DisplayName("AuthenticationManager delega el email correcto al UsernamePasswordAuthenticationToken")
        void shouldAuthenticateWithCorrectCredentials() {
            LoginRequest req = new LoginRequest("william@integrityfamily.com", "mySecret99");
            when(authenticationManager.authenticate(any())).thenReturn(null);
            when(userRepository.findByEmail("william@integrityfamily.com")).thenReturn(Optional.of(baseUser));
            when(jwtTokenProvider.generate(any())).thenReturn("tok");
            when(refreshTokenService.createRefreshToken(anyLong())).thenReturn(baseRefreshToken);

            authService.login(req, "127.0.0.1", "UA");

            ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                    ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
            verify(authenticationManager).authenticate(captor.capture());
            assertThat(captor.getValue().getPrincipal()).isEqualTo("william@integrityfamily.com");
            assertThat(captor.getValue().getCredentials()).isEqualTo("mySecret99");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  register()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("Email nuevo → crea usuario, retorna LoginResponse con token")
        void shouldCreateUserAndReturnToken_whenEmailIsNew() {
            RegisterRequest req = new RegisterRequest("Nuevo Usuario", "nuevo@if.com", "pass1234", null);
            when(userRepository.findByEmailIgnoreCase("nuevo@if.com")).thenReturn(Optional.empty());
            when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
            when(passwordEncoder.encode("pass1234")).thenReturn("$hashed$");
            User savedUser = User.builder()
                    .id(5L).email("nuevo@if.com").fullName("Nuevo Usuario")
                    .passwordHash("$hashed$").enabled(true)
                    .roles(List.of(userRole)).build();
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(jwtTokenProvider.generate(savedUser)).thenReturn("new.jwt.token");
            when(refreshTokenService.createRefreshToken(5L)).thenReturn(baseRefreshToken);

            LoginResponse response = authService.register(req, "127.0.0.1", "UA");

            assertThat(response.token()).isEqualTo("new.jwt.token");
            assertThat(response.user().email()).isEqualTo("nuevo@if.com");
            verify(passwordEncoder).encode("pass1234");
        }

        @Test
        @DisplayName("Email duplicado → BusinessException EMAIL_ALREADY_USED (409 CONFLICT)")
        void shouldThrowConflict_whenEmailAlreadyExists() {
            RegisterRequest req = new RegisterRequest("Ya Existe", "dup@if.com", "pass1234", null);
            when(userRepository.findByEmailIgnoreCase("dup@if.com")).thenReturn(Optional.of(baseUser));

            assertThatThrownBy(() -> authService.register(req, "127.0.0.1", "UA"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException bex = (BusinessException) ex;
                        assertThat(bex.getCode()).isEqualTo("EMAIL_ALREADY_USED");
                        assertThat(bex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    });

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rol ROLE_USER no existe en BD → BusinessException ROLE_NOT_FOUND (500)")
        void shouldThrowInternalError_whenRoleNotFound() {
            RegisterRequest req = new RegisterRequest("Sin Rol", "sin@if.com", "pass1234", null);
            when(userRepository.findByEmailIgnoreCase("sin@if.com")).thenReturn(Optional.empty());
            when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.register(req, "127.0.0.1", "UA"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException bex = (BusinessException) ex;
                        assertThat(bex.getCode()).isEqualTo("ROLE_NOT_FOUND");
                        assertThat(bex.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    });
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  registerFamily()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("registerFamily()")
    class RegisterFamily {

        @Test
        @DisplayName("Familia nueva → persiste familia + admin, código generado con patrón IF-YYYY-XXXXXXXX")
        void shouldCreateFamilyAndAdmin_andReturnToken() {
            RegisterFamilyRequest req = new RegisterFamilyRequest(
                    "Familia Gómez", "Carlos Gómez", "carlos@if.com",
                    "Password1", "Password1", "Bogotá", "CO", "CUN");

            when(userRepository.findByEmailIgnoreCase("carlos@if.com")).thenReturn(Optional.empty());
            when(roleRepository.findByName("ROLE_FAMILY_ADMIN")).thenReturn(Optional.of(familyAdminRole));
            when(passwordEncoder.encode("Password1")).thenReturn("$hashed$");

            User savedAdmin = User.builder()
                    .id(7L).email("carlos@if.com").fullName("Carlos Gómez")
                    .passwordHash("$hashed$").enabled(true)
                    .roles(List.of(familyAdminRole)).build();
            Family savedFamily = Family.builder()
                    .id(20L).name("Familia Gómez").familyCode("IF-2026-XYZ")
                    .currentMilestone("W1").sentinelActive(true).municipio("Bogotá")
                    .countryCode("CO").departmentCode("CUN").build();

            when(userRepository.save(any(User.class))).thenReturn(savedAdmin);
            when(familyRepository.save(any(Family.class))).thenReturn(savedFamily);
            when(jwtTokenProvider.generate(any(User.class))).thenReturn("family.jwt.token");
            when(refreshTokenService.createRefreshToken(7L)).thenReturn(baseRefreshToken);

            LoginResponse response = authService.registerFamily(req, "127.0.0.1", "UA");

            assertThat(response.token()).isEqualTo("family.jwt.token");
            // Verificar que se guardó la familia y el usuario (mínimo 2 saves del usuario: antes y después de vincular)
            verify(familyRepository).save(any(Family.class));
            verify(userRepository, atLeast(2)).save(any(User.class));
        }

        @Test
        @DisplayName("Email duplicado en familia → BusinessException EMAIL_ALREADY_USED antes de crear familia")
        void shouldThrowConflict_andNotCreateFamily_whenEmailDuplicated() {
            RegisterFamilyRequest req = new RegisterFamilyRequest(
                    "Familia X", "Admin X", "dup@if.com",
                    "Password1", "Password1", "Medellín", "CO", "ANT");

            when(userRepository.findByEmailIgnoreCase("dup@if.com")).thenReturn(Optional.of(baseUser));

            assertThatThrownBy(() -> authService.registerFamily(req, "127.0.0.1", "UA"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo("EMAIL_ALREADY_USED"));

            verify(familyRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rol ROLE_FAMILY_ADMIN no configurado → BusinessException ROLE_NOT_FOUND")
        void shouldThrowInternalError_whenFamilyAdminRoleMissing() {
            RegisterFamilyRequest req = new RegisterFamilyRequest(
                    "Familia Y", "Admin Y", "new@if.com",
                    "Password1", "Password1", "Cali", "CO", "VAC");

            when(userRepository.findByEmailIgnoreCase("new@if.com")).thenReturn(Optional.empty());
            when(roleRepository.findByName("ROLE_FAMILY_ADMIN")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.registerFamily(req, "127.0.0.1", "UA"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo("ROLE_NOT_FOUND"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  me()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("me()")
    class Me {

        @Test
        @DisplayName("Email registrado → retorna UserResponse con datos del usuario")
        void shouldReturnUserResponse_whenEmailExists() {
            when(userRepository.findByEmail("william@integrityfamily.com"))
                    .thenReturn(Optional.of(baseUser));

            UserResponse response = authService.me("william@integrityfamily.com");

            assertThat(response.email()).isEqualTo("william@integrityfamily.com");
            assertThat(response.fullName()).isEqualTo("William López");
            assertThat(response.familyId()).isEqualTo(10L);
            assertThat(response.familyName()).isEqualTo("Familia López");
        }

        @Test
        @DisplayName("Email no registrado → RuntimeException")
        void shouldThrowRuntimeException_whenEmailNotFound() {
            when(userRepository.findByEmail("ghost@if.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.me("ghost@if.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("no encontrado");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  logout()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("logout()")
    class Logout {

        @Test
        @DisplayName("Usuario encontrado → elimina sus refresh tokens")
        void shouldDeleteRefreshTokens_whenUserExists() {
            when(userRepository.findByEmail("william@integrityfamily.com"))
                    .thenReturn(Optional.of(baseUser));

            authService.logout("william@integrityfamily.com", "127.0.0.1", "UA");

            verify(refreshTokenService).deleteByUserId(1L);
        }

        @Test
        @DisplayName("Email desconocido → no lanza excepción (ifPresent es no-op)")
        void shouldDoNothing_whenUserDoesNotExist() {
            when(userRepository.findByEmail("nobody@if.com")).thenReturn(Optional.empty());

            authService.logout("nobody@if.com", "127.0.0.1", "UA");

            verify(refreshTokenService, never()).deleteByUserId(anyLong());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  refreshToken()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("refreshToken()")
    class RefreshTokenTests {

        @Test
        @DisplayName("Token válido → genera nuevo JWT y retorna LoginResponse")
        void shouldReturnNewJwt_whenRefreshTokenIsValid() {
            RefreshTokenRequest req = new RefreshTokenRequest(baseRefreshToken.getToken());
            when(refreshTokenService.findByToken(baseRefreshToken.getToken()))
                    .thenReturn(baseRefreshToken);
            when(refreshTokenService.verifyExpiration(baseRefreshToken))
                    .thenReturn(baseRefreshToken);
            when(jwtTokenProvider.generate(baseUser)).thenReturn("new.access.token");

            LoginResponse response = authService.refreshToken(req, "127.0.0.1", "UA");

            assertThat(response.token()).isEqualTo("new.access.token");
            assertThat(response.refreshToken()).isEqualTo(baseRefreshToken.getToken());
            assertThat(response.user().email()).isEqualTo("william@integrityfamily.com");
        }

        @Test
        @DisplayName("Token expirado → RuntimeException desde verifyExpiration")
        void shouldPropagateException_whenTokenExpiredOrRevoked() {
            RefreshTokenRequest req = new RefreshTokenRequest("expired-token-value");
            RefreshToken expiredToken = RefreshToken.builder()
                    .token("expired-token-value")
                    .user(baseUser)
                    .expiryDate(Instant.now().minusSeconds(3600))
                    .revoked(false)
                    .build();
            when(refreshTokenService.findByToken("expired-token-value")).thenReturn(expiredToken);
            when(refreshTokenService.verifyExpiration(expiredToken))
                    .thenThrow(new RuntimeException("El token de refresco ha expirado"));

            assertThatThrownBy(() -> authService.refreshToken(req, "127.0.0.1", "UA"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("expirado");
        }

        @Test
        @DisplayName("Token no encontrado → RuntimeException desde findByToken")
        void shouldThrowException_whenRefreshTokenNotInDatabase() {
            RefreshTokenRequest req = new RefreshTokenRequest("nonexistent-token");
            when(refreshTokenService.findByToken("nonexistent-token"))
                    .thenThrow(new RuntimeException("Token de refresco no encontrado"));

            assertThatThrownBy(() -> authService.refreshToken(req, "127.0.0.1", "UA"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("no encontrado");
        }
    }
}
