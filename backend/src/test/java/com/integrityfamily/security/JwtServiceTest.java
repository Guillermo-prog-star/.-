package com.integrityfamily.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas unitarias de JwtService.
 *
 * Cubre: generación de tokens, extracción de username, validación (usuario correcto,
 * expiración), CustomClaims.toMap(), y fallo por secret key vacía.
 *
 * No levanta contexto Spring — usa ReflectionTestUtils para inyectar @Value.
 */
@DisplayName("JwtService — Unit Tests")
class JwtServiceTest {

    /** 43 bytes (344 bits) → válido para HMAC-SHA-256 (mínimo 256 bits). */
    private static final String TEST_SECRET = Base64.getEncoder()
            .encodeToString("integrity-family-jwt-test-secret-2026-valid"
                    .getBytes(StandardCharsets.UTF_8));

    private static final long EXPIRATION_MS = 3_600_000L; // 1 hora

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey",    TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION_MS);

        userDetails = User.withUsername("william@integrityfamily.com")
                .password("{noop}password")
                .roles("USER")   // → authority ROLE_USER
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  generateToken() y extractUsername()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("generateToken() y extractUsername()")
    class GenerateAndExtract {

        @Test
        @DisplayName("Genera un token no vacío con el email como subject")
        void shouldGenerateNonBlankToken() {
            String token = jwtService.generateToken(userDetails);
            assertThat(token).isNotBlank();
        }

        @Test
        @DisplayName("extractUsername() devuelve el email correcto del subject")
        void shouldExtractCorrectUsername() {
            String token = jwtService.generateToken(userDetails);
            assertThat(jwtService.extractUsername(token))
                    .isEqualTo("william@integrityfamily.com");
        }

        @Test
        @DisplayName("generateToken(CustomClaims, UserDetails) incluye familyId y role")
        void shouldIncludeCustomClaimsInToken() {
            JwtService.CustomClaims claims = new JwtService.CustomClaims(42L, "ROLE_USER");
            String token = jwtService.generateToken(claims, userDetails);

            // Verificar que el token es parseable y el subject es correcto
            assertThat(jwtService.extractUsername(token))
                    .isEqualTo("william@integrityfamily.com");

            // Verificar claims extra
            String familyId = jwtService.extractClaim(token,
                    c -> c.get("familyId", Integer.class).toString());
            assertThat(familyId).isEqualTo("42");

            String role = jwtService.extractClaim(token,
                    c -> c.get("role", String.class));
            assertThat(role).isEqualTo("ROLE_USER");
        }

        @Test
        @DisplayName("generateToken(CustomClaims null fields, UserDetails) omite nulls del mapa")
        void shouldNotIncludeNullClaims() {
            JwtService.CustomClaims claims = new JwtService.CustomClaims(null, null);
            String token = jwtService.generateToken(claims, userDetails);

            // familyId y role no deben estar en el token
            Object familyId = jwtService.extractClaim(token,
                    c -> c.get("familyId"));
            assertThat(familyId).isNull();
        }

        @Test
        @DisplayName("Token generado tiene tres partes separadas por '.' (formato JWT)")
        void shouldHaveThreeParts_validJwtFormat() {
            String token = jwtService.generateToken(userDetails);
            assertThat(token.split("\\.")).hasSize(3);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  isTokenValid()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("isTokenValid()")
    class IsTokenValid {

        @Test
        @DisplayName("Token válido para el mismo usuario → true")
        void shouldReturnTrue_whenTokenMatchesUserAndIsNotExpired() {
            String token = jwtService.generateToken(userDetails);
            assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
        }

        @Test
        @DisplayName("Token de otro usuario → false (username diferente)")
        void shouldReturnFalse_whenTokenBelongsToDifferentUser() {
            String token = jwtService.generateToken(userDetails);

            UserDetails otherUser = User.withUsername("otro@integrityfamily.com")
                    .password("{noop}pass")
                    .roles("USER")
                    .build();

            assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
        }

        @Test
        @DisplayName("Token expirado → false")
        void shouldReturnFalse_whenTokenIsExpired() {
            // jwtExpiration negativa → expira inmediatamente en el pasado
            ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1_000L);
            String expiredToken = jwtService.generateToken(userDetails);

            // Restaurar para que isTokenValid pueda parsear el token
            ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION_MS);

            assertThat(jwtService.isTokenValid(expiredToken, userDetails)).isFalse();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CustomClaims.toMap()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("CustomClaims.toMap()")
    class CustomClaimsToMap {

        @Test
        @DisplayName("Ambos campos presentes → mapa contiene familyId y role")
        void shouldIncludeBothFields_whenNotNull() {
            var map = new JwtService.CustomClaims(10L, "ROLE_ADMIN").toMap();
            assertThat(map).containsEntry("familyId", 10L)
                           .containsEntry("role", "ROLE_ADMIN");
        }

        @Test
        @DisplayName("familyId null → no aparece en el mapa")
        void shouldExcludeFamilyId_whenNull() {
            var map = new JwtService.CustomClaims(null, "ROLE_USER").toMap();
            assertThat(map).doesNotContainKey("familyId")
                           .containsEntry("role", "ROLE_USER");
        }

        @Test
        @DisplayName("role null → no aparece en el mapa")
        void shouldExcludeRole_whenNull() {
            var map = new JwtService.CustomClaims(5L, null).toMap();
            assertThat(map).containsEntry("familyId", 5L)
                           .doesNotContainKey("role");
        }

        @Test
        @DisplayName("Ambos null → mapa vacío")
        void shouldReturnEmptyMap_whenBothNull() {
            var map = new JwtService.CustomClaims(null, null).toMap();
            assertThat(map).isEmpty();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  getSignInKey() — fail-fast
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Secret key vacía → IllegalStateException")
    class SecretKeyValidation {

        @Test
        @DisplayName("secretKey null → IllegalStateException al intentar generar token")
        void shouldThrowIllegalState_whenSecretKeyIsNull() {
            ReflectionTestUtils.setField(jwtService, "secretKey", null);
            assertThatThrownBy(() -> jwtService.generateToken(userDetails))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("JWT Secret Key");
        }

        @Test
        @DisplayName("secretKey blank → IllegalStateException al intentar generar token")
        void shouldThrowIllegalState_whenSecretKeyIsBlank() {
            ReflectionTestUtils.setField(jwtService, "secretKey", "   ");
            assertThatThrownBy(() -> jwtService.generateToken(userDetails))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("JWT Secret Key");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  getJwtExpiration()
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getJwtExpiration() devuelve el valor configurado")
    void shouldReturnConfiguredExpiration() {
        assertThat(jwtService.getJwtExpiration()).isEqualTo(EXPIRATION_MS);
    }
}
