package com.integrityfamily.alexa.service;

import com.integrityfamily.alexa.config.AlexaProperties;
import com.integrityfamily.alexa.domain.AlexaOAuthCode;
import com.integrityfamily.alexa.domain.AlexaOAuthToken;
import com.integrityfamily.alexa.repository.AlexaOAuthCodeRepository;
import com.integrityfamily.alexa.repository.AlexaOAuthTokenRepository;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.User;
import com.integrityfamily.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlexaOAuthService — Unit Tests")
class AlexaOAuthServiceTest {

    @Mock AlexaProperties alexaProperties;
    @Mock AlexaOAuthCodeRepository codeRepository;
    @Mock AlexaOAuthTokenRepository tokenRepository;
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks AlexaOAuthService service;

    private static final String EMAIL = "user@example.com";
    private static final String PASSWORD = "secret123";
    private static final String REDIRECT_URI = "https://alexa.amazon.com/auth/callback";
    private static final String CODE_VERIFIER = "test-code-verifier-abc123";

    private static String sha256Base64Url(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(input.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    private User buildUser(Long familyId) {
        Family family = familyId != null ? Family.builder().id(familyId).build() : null;
        return User.builder()
                .id(1L)
                .email(EMAIL)
                .passwordHash("hashed-secret")
                .family(family)
                .build();
    }

    private AlexaOAuthCode storedCode(boolean expired, String redirectUri, String challenge) {
        long now = System.currentTimeMillis() / 1000L;
        return AlexaOAuthCode.builder()
                .code("valid-code")
                .userId(1L)
                .familyId(42L)
                .redirectUri(redirectUri)
                .codeChallenge(challenge)
                .expiresAt(expired ? now - 1 : now + 300)
                .used(false)
                .createdAt(now)
                .build();
    }

    @Nested
    @DisplayName("createAuthorizationCode()")
    class CreateAuthorizationCode {

        @Test
        @DisplayName("emite código cuando credenciales y familia son válidos")
        void happyPath() {
            when(alexaProperties.getCodeTtlSeconds()).thenReturn(300L);
            when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(buildUser(42L)));
            when(passwordEncoder.matches(PASSWORD, "hashed-secret")).thenReturn(true);

            String code = service.createAuthorizationCode(EMAIL, PASSWORD, REDIRECT_URI, "challenge-abc");

            assertThat(code).isNotBlank();
            ArgumentCaptor<AlexaOAuthCode> captor = ArgumentCaptor.forClass(AlexaOAuthCode.class);
            verify(codeRepository).save(captor.capture());
            AlexaOAuthCode saved = captor.getValue();
            assertThat(saved.getCode()).isEqualTo(code);
            assertThat(saved.getFamilyId()).isEqualTo(42L);
            assertThat(saved.isUsed()).isFalse();
            assertThat(saved.getCodeChallenge()).isEqualTo("challenge-abc");
            assertThat(saved.getRedirectUri()).isEqualTo(REDIRECT_URI);
        }

        @Test
        @DisplayName("lanza IllegalArgumentException si usuario no existe")
        void userNotFound() {
            when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.createAuthorizationCode(EMAIL, PASSWORD, REDIRECT_URI, "ch"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Credenciales inválidas");
        }

        @Test
        @DisplayName("lanza IllegalArgumentException si contraseña incorrecta")
        void wrongPassword() {
            when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(buildUser(1L)));
            when(passwordEncoder.matches(PASSWORD, "hashed-secret")).thenReturn(false);

            assertThatThrownBy(() ->
                    service.createAuthorizationCode(EMAIL, PASSWORD, REDIRECT_URI, "ch"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Credenciales inválidas");
        }

        @Test
        @DisplayName("lanza IllegalStateException si usuario sin familia")
        void userWithoutFamily() {
            when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(buildUser(null)));
            when(passwordEncoder.matches(PASSWORD, "hashed-secret")).thenReturn(true);

            assertThatThrownBy(() ->
                    service.createAuthorizationCode(EMAIL, PASSWORD, REDIRECT_URI, "ch"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("familia");
        }
    }

    @Nested
    @DisplayName("exchangeCode()")
    class ExchangeCode {

        @Test
        @DisplayName("intercambia código por access y refresh token cuando todo es válido")
        void happyPath() throws Exception {
            String challenge = sha256Base64Url(CODE_VERIFIER);
            AlexaOAuthCode code = storedCode(false, REDIRECT_URI, challenge);
            when(codeRepository.findByCodeAndUsedFalse("valid-code")).thenReturn(Optional.of(code));
            when(alexaProperties.getTokenTtlSeconds()).thenReturn(3600L);

            Map<String, Object> response = service.exchangeCode("valid-code", REDIRECT_URI, CODE_VERIFIER);

            assertThat(response).containsKeys("access_token", "refresh_token", "token_type", "expires_in");
            assertThat(response.get("token_type")).isEqualTo("Bearer");
            assertThat(response.get("expires_in")).isEqualTo(3600L);
            assertThat(code.isUsed()).isTrue();
            verify(tokenRepository).save(any(AlexaOAuthToken.class));
        }

        @Test
        @DisplayName("lanza IllegalArgumentException si código no existe o ya fue usado")
        void codeNotFound() {
            when(codeRepository.findByCodeAndUsedFalse("bad")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.exchangeCode("bad", REDIRECT_URI, CODE_VERIFIER))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Código inválido");
        }

        @Test
        @DisplayName("lanza IllegalArgumentException si código expirado")
        void codeExpired() throws Exception {
            String challenge = sha256Base64Url(CODE_VERIFIER);
            when(codeRepository.findByCodeAndUsedFalse("exp"))
                    .thenReturn(Optional.of(storedCode(true, REDIRECT_URI, challenge)));

            assertThatThrownBy(() -> service.exchangeCode("exp", REDIRECT_URI, CODE_VERIFIER))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("expirado");
        }

        @Test
        @DisplayName("lanza IllegalArgumentException si redirect_uri no coincide")
        void redirectMismatch() throws Exception {
            String challenge = sha256Base64Url(CODE_VERIFIER);
            when(codeRepository.findByCodeAndUsedFalse("ok"))
                    .thenReturn(Optional.of(storedCode(false, "https://other.com/cb", challenge)));

            assertThatThrownBy(() -> service.exchangeCode("ok", REDIRECT_URI, CODE_VERIFIER))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("redirect_uri");
        }

        @Test
        @DisplayName("lanza IllegalArgumentException si PKCE verifier inválido")
        void pkceInvalid() {
            when(codeRepository.findByCodeAndUsedFalse("ok"))
                    .thenReturn(Optional.of(storedCode(false, REDIRECT_URI, "correct-challenge-xyz")));

            assertThatThrownBy(() -> service.exchangeCode("ok", REDIRECT_URI, "wrong-verifier"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("PKCE");
        }
    }

    @Nested
    @DisplayName("refreshToken()")
    class RefreshToken {

        private AlexaOAuthToken existingToken() {
            return AlexaOAuthToken.builder()
                    .accessToken("old-access")
                    .refreshToken("valid-refresh")
                    .userId(1L)
                    .familyId(42L)
                    .expiresAt(System.currentTimeMillis() / 1000L + 3600)
                    .revoked(false)
                    .build();
        }

        @Test
        @DisplayName("rota el access token con refresh válido, no incluye nuevo refresh")
        void happyPath() {
            when(tokenRepository.findByRefreshTokenAndRevokedFalse("valid-refresh"))
                    .thenReturn(Optional.of(existingToken()));
            when(tokenRepository.rotateAccessToken(eq("valid-refresh"), anyString(), anyLong()))
                    .thenReturn(1);
            when(alexaProperties.getTokenTtlSeconds()).thenReturn(3600L);

            Map<String, Object> response = service.refreshToken("valid-refresh");

            assertThat(response).containsKey("access_token");
            assertThat(response).doesNotContainKey("refresh_token");
            assertThat(response.get("token_type")).isEqualTo("Bearer");
        }

        @Test
        @DisplayName("lanza IllegalArgumentException si refresh token no existe o está revocado")
        void tokenNotFound() {
            when(tokenRepository.findByRefreshTokenAndRevokedFalse("bad")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.refreshToken("bad"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Refresh token inválido");
        }

        @Test
        @DisplayName("lanza IllegalStateException si la rotación no actualiza ninguna fila")
        void rotateFails() {
            AlexaOAuthToken token = AlexaOAuthToken.builder()
                    .accessToken("old-access").refreshToken("rf")
                    .userId(1L).familyId(42L)
                    .expiresAt(System.currentTimeMillis() / 1000L + 3600)
                    .revoked(false).build();
            when(tokenRepository.findByRefreshTokenAndRevokedFalse("rf"))
                    .thenReturn(Optional.of(token));
            when(tokenRepository.rotateAccessToken(eq("rf"), anyString(), anyLong())).thenReturn(0);
            when(alexaProperties.getTokenTtlSeconds()).thenReturn(3600L);

            assertThatThrownBy(() -> service.refreshToken("rf"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("rotar");
        }
    }

    @Nested
    @DisplayName("resolveToken()")
    class ResolveToken {

        @Test
        @DisplayName("resuelve token válido y no expirado")
        void validToken() {
            AlexaOAuthToken token = AlexaOAuthToken.builder()
                    .accessToken("access-xyz")
                    .refreshToken("refresh-abc")
                    .userId(1L).familyId(42L)
                    .expiresAt(System.currentTimeMillis() / 1000L + 3600)
                    .revoked(false)
                    .build();
            when(tokenRepository.findByAccessTokenAndRevokedFalse("access-xyz"))
                    .thenReturn(Optional.of(token));

            Optional<AlexaOAuthToken> result = service.resolveToken("access-xyz");

            assertThat(result).isPresent().contains(token);
        }

        @Test
        @DisplayName("devuelve vacío para token expirado")
        void expiredToken() {
            AlexaOAuthToken token = AlexaOAuthToken.builder()
                    .accessToken("old-token")
                    .refreshToken("rf")
                    .userId(1L).familyId(1L)
                    .expiresAt(System.currentTimeMillis() / 1000L - 1)
                    .revoked(false)
                    .build();
            when(tokenRepository.findByAccessTokenAndRevokedFalse("old-token"))
                    .thenReturn(Optional.of(token));

            assertThat(service.resolveToken("old-token")).isEmpty();
        }

        @Test
        @DisplayName("devuelve vacío si token no existe")
        void tokenMissing() {
            when(tokenRepository.findByAccessTokenAndRevokedFalse("miss"))
                    .thenReturn(Optional.empty());

            assertThat(service.resolveToken("miss")).isEmpty();
        }
    }
}
