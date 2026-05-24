package com.integrityfamily.auth.service;

import com.integrityfamily.domain.RefreshToken;
import com.integrityfamily.domain.User;
import com.integrityfamily.domain.repository.RefreshTokenRepository;
import com.integrityfamily.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService")
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User buildUser(Long id) {
        return User.builder()
                .id(id)
                .email("test@family.com")
                .passwordHash("hash")
                .fullName("Test User")
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("createRefreshToken")
    class CreateRefreshToken {

        @Test
        @DisplayName("usuario encontrado → crea token con UUID no vacío, expiración futura y revoked=false")
        void usuarioEncontrado_creaToken() {
            User user = buildUser(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            when(refreshTokenRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            RefreshToken result = refreshTokenService.createRefreshToken(1L);

            verify(refreshTokenRepository).deleteByUser(user);
            verify(refreshTokenRepository).save(any(RefreshToken.class));

            RefreshToken saved = captor.getValue();
            assertThat(saved.getToken()).isNotBlank();
            assertThat(saved.getToken()).hasSize(36); // UUID canónico
            assertThat(saved.getExpiryDate()).isAfter(Instant.now());
            assertThat(saved.isRevoked()).isFalse();
            assertThat(saved.getUser()).isSameAs(user);

            assertThat(result).isSameAs(saved);
        }

        @Test
        @DisplayName("usuario no encontrado → lanza RuntimeException")
        void usuarioNoEncontrado_lanzaExcepcion() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> refreshTokenService.createRefreshToken(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Usuario no encontrado");

            verifyNoInteractions(refreshTokenRepository);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("verifyExpiration")
    class VerifyExpiration {

        @Test
        @DisplayName("token válido (expiry futuro, no revocado) → retorna el mismo token")
        void tokenValido_retornaToken() {
            RefreshToken token = RefreshToken.builder()
                    .token("abc-123")
                    .expiryDate(Instant.now().plusSeconds(3600))
                    .revoked(false)
                    .build();

            RefreshToken result = refreshTokenService.verifyExpiration(token);

            assertThat(result).isSameAs(token);
            verifyNoInteractions(refreshTokenRepository);
        }

        @Test
        @DisplayName("token expirado (expiryDate en el pasado) → elimina y lanza RuntimeException")
        void tokenExpirado_eliminaYLanzaExcepcion() {
            RefreshToken token = RefreshToken.builder()
                    .token("expired-token")
                    .expiryDate(Instant.now().minusSeconds(60))
                    .revoked(false)
                    .build();

            assertThatThrownBy(() -> refreshTokenService.verifyExpiration(token))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("expirado o ha sido revocado");

            verify(refreshTokenRepository).delete(token);
        }

        @Test
        @DisplayName("token revocado (revoked=true) → elimina y lanza RuntimeException")
        void tokenRevocado_eliminaYLanzaExcepcion() {
            RefreshToken token = RefreshToken.builder()
                    .token("revoked-token")
                    .expiryDate(Instant.now().plusSeconds(3600))
                    .revoked(true)
                    .build();

            assertThatThrownBy(() -> refreshTokenService.verifyExpiration(token))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("expirado o ha sido revocado");

            verify(refreshTokenRepository).delete(token);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("deleteByUserId")
    class DeleteByUserId {

        @Test
        @DisplayName("usuario encontrado → invoca deleteByUser")
        void usuarioEncontrado_invocaDeleteByUser() {
            User user = buildUser(2L);
            when(userRepository.findById(2L)).thenReturn(Optional.of(user));

            refreshTokenService.deleteByUserId(2L);

            verify(refreshTokenRepository).deleteByUser(user);
        }

        @Test
        @DisplayName("usuario no encontrado → no interactúa con el repositorio de tokens")
        void usuarioNoEncontrado_noHaceNada() {
            when(userRepository.findById(55L)).thenReturn(Optional.empty());

            refreshTokenService.deleteByUserId(55L);

            verifyNoInteractions(refreshTokenRepository);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("findByToken")
    class FindByToken {

        @Test
        @DisplayName("token encontrado → retorna el RefreshToken")
        void tokenEncontrado_retornaToken() {
            RefreshToken token = RefreshToken.builder()
                    .token("valid-token")
                    .expiryDate(Instant.now().plusSeconds(3600))
                    .revoked(false)
                    .build();
            when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

            RefreshToken result = refreshTokenService.findByToken("valid-token");

            assertThat(result).isSameAs(token);
        }

        @Test
        @DisplayName("token no encontrado → lanza RuntimeException")
        void tokenNoEncontrado_lanzaExcepcion() {
            when(refreshTokenRepository.findByToken("ghost-token")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> refreshTokenService.findByToken("ghost-token"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Token de refresco no encontrado");
        }
    }
}
