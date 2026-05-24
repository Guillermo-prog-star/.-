package com.integrityfamily.security;

import com.integrityfamily.domain.Role;
import com.integrityfamily.domain.User;
import com.integrityfamily.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de CustomUserDetailsService.
 *
 * Verifica que Spring Security recibe los UserDetails correctos:
 * username, passwordHash, enabled, authorities, y accountNonLocked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService — Unit Tests")
class CustomUserDetailsServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    CustomUserDetailsService service;

    // ─── Fixtures ────────────────────────────────────────────────────────

    private User activeUser(String role) {
        return User.builder()
                .id(1L)
                .email("william@integrityfamily.com")
                .passwordHash("$2a$10$hashedpassword")
                .fullName("William López")
                .enabled(true)
                .roles(List.of(Role.builder().id(1L).name(role).build()))
                .build();
    }

    private User lockedUser() {
        User u = activeUser("ROLE_USER");
        u.setAccountLockedUntil(LocalDateTime.now().plusHours(1)); // aún bloqueado
        return u;
    }

    private User disabledUser() {
        User u = activeUser("ROLE_USER");
        u.setEnabled(false);
        return u;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  loadUserByUsername() — usuario encontrado
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Usuario encontrado")
    class UserFound {

        @Test
        @DisplayName("Usuario activo → UserDetails con email, hash y authority ROLE_USER")
        void shouldReturnUserDetails_withCorrectCredentials() {
            when(userRepository.findByEmailIgnoreCase("william@integrityfamily.com"))
                    .thenReturn(Optional.of(activeUser("ROLE_USER")));

            UserDetails details = service.loadUserByUsername("william@integrityfamily.com");

            assertThat(details.getUsername()).isEqualTo("william@integrityfamily.com");
            assertThat(details.getPassword()).isEqualTo("$2a$10$hashedpassword");
            assertThat(details.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Usuario activo → tiene exactamente la authority de su rol")
        void shouldHaveCorrectAuthority_forRoleUser() {
            when(userRepository.findByEmailIgnoreCase("william@integrityfamily.com"))
                    .thenReturn(Optional.of(activeUser("ROLE_USER")));

            UserDetails details = service.loadUserByUsername("william@integrityfamily.com");

            assertThat(details.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_USER");
        }

        @Test
        @DisplayName("Admin → authority ROLE_ADMIN")
        void shouldHaveCorrectAuthority_forRoleAdmin() {
            when(userRepository.findByEmailIgnoreCase("admin@integrityfamily.com"))
                    .thenReturn(Optional.of(activeUser("ROLE_ADMIN")));

            UserDetails details = service.loadUserByUsername("admin@integrityfamily.com");

            assertThat(details.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_ADMIN");
        }

        @Test
        @DisplayName("Usuario activo no bloqueado → isAccountNonLocked() = true")
        void shouldBeNonLocked_whenNotLocked() {
            when(userRepository.findByEmailIgnoreCase("william@integrityfamily.com"))
                    .thenReturn(Optional.of(activeUser("ROLE_USER")));

            UserDetails details = service.loadUserByUsername("william@integrityfamily.com");

            assertThat(details.isAccountNonLocked()).isTrue();
        }

        @Test
        @DisplayName("Usuario con cuenta bloqueada → isAccountNonLocked() = false")
        void shouldBeAccountLocked_whenLockIsInFuture() {
            when(userRepository.findByEmailIgnoreCase("locked@integrityfamily.com"))
                    .thenReturn(Optional.of(lockedUser()));

            UserDetails details = service.loadUserByUsername("locked@integrityfamily.com");

            assertThat(details.isAccountNonLocked()).isFalse();
        }

        @Test
        @DisplayName("Usuario deshabilitado → isEnabled() = false")
        void shouldBeDisabled_whenUserIsDisabled() {
            when(userRepository.findByEmailIgnoreCase("disabled@integrityfamily.com"))
                    .thenReturn(Optional.of(disabledUser()));

            UserDetails details = service.loadUserByUsername("disabled@integrityfamily.com");

            assertThat(details.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("accountNonExpired y credentialsNonExpired siempre true (gestión manual)")
        void shouldAlwaysBeNonExpiredAndCredentialsNonExpired() {
            when(userRepository.findByEmailIgnoreCase("william@integrityfamily.com"))
                    .thenReturn(Optional.of(activeUser("ROLE_USER")));

            UserDetails details = service.loadUserByUsername("william@integrityfamily.com");

            assertThat(details.isAccountNonExpired()).isTrue();
            assertThat(details.isCredentialsNonExpired()).isTrue();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  loadUserByUsername() — usuario no encontrado
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Usuario no encontrado")
    class UserNotFound {

        @Test
        @DisplayName("Email inexistente → UsernameNotFoundException")
        void shouldThrowUsernameNotFoundException_whenEmailNotFound() {
            when(userRepository.findByEmailIgnoreCase("ghost@integrityfamily.com"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.loadUserByUsername("ghost@integrityfamily.com"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("no encontrado");
        }
    }
}
