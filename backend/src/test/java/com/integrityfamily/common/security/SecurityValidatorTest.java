package com.integrityfamily.common.security;

import com.integrityfamily.common.exception.NotFoundException;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.Role;
import com.integrityfamily.domain.User;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.MemberRepository;
import com.integrityfamily.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityValidator — Unit Tests")
class SecurityValidatorTest {

    @Mock FamilyRepository familyRepository;
    @Mock MemberRepository memberRepository;
    @Mock UserRepository   userRepository;

    @InjectMocks SecurityValidator validator;

    private static final Long FAM_ID = 1L;
    private static final String EMAIL = "william@integrityfamily.com";

    private Principal principal;
    private Family    family;
    private User      regularUser;

    @BeforeEach
    void setUp() {
        principal   = () -> EMAIL;
        family      = Family.builder().id(FAM_ID).name("Familia López").build();
        regularUser = User.builder().email(EMAIL).roles(List.of()).build();
    }

    // ═══════════════════════════════════════════════════════════════
    //  Principal null
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Principal null → siempre AccessDeniedException")
    class NullPrincipal {

        @Test
        @DisplayName("principal null → AccessDeniedException 'No autenticado'")
        void shouldThrow_whenPrincipalIsNull() {
            assertThatThrownBy(() -> validator.validateFamilyOwnership(FAM_ID, null))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("No autenticado");

            verifyNoInteractions(familyRepository, memberRepository, userRepository);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Bypass: ROLE_ADMIN
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Bypass de ROLE_ADMIN")
    class AdminBypass {

        @Test
        @DisplayName("usuario ROLE_ADMIN → acceso permitido sin verificar familia ni miembro")
        void shouldAllow_whenUserIsAdmin() {
            Role adminRole = Role.builder().name("ROLE_ADMIN").build();
            User adminUser = User.builder().email(EMAIL).roles(List.of(adminRole)).build();

            when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(adminUser));

            assertThatCode(() -> validator.validateFamilyOwnership(FAM_ID, principal))
                    .doesNotThrowAnyException();

            verifyNoInteractions(familyRepository, memberRepository);
        }

        @Test
        @DisplayName("usuario con otros roles (no ROLE_ADMIN) → continúa validación normal")
        void shouldContinueValidation_whenUserHasOtherRoles() {
            Role userRole = Role.builder().name("ROLE_USER").build();
            User normalUser = User.builder().email(EMAIL).roles(List.of(userRole)).build();
            User creator = User.builder().email(EMAIL).build();
            family.setCreatedBy(creator);

            when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(normalUser));
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));

            // Creador → pasa la validación (no llega a memberRepository)
            assertThatCode(() -> validator.validateFamilyOwnership(FAM_ID, principal))
                    .doesNotThrowAnyException();

            verify(familyRepository).findById(FAM_ID);
            verifyNoInteractions(memberRepository);
        }

        @Test
        @DisplayName("usuario no encontrado en BD → continúa validación sin crash")
        void shouldContinueValidation_whenUserNotFoundInDb() {
            User creator = User.builder().email(EMAIL).build();
            family.setCreatedBy(creator);

            when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));

            assertThatCode(() -> validator.validateFamilyOwnership(FAM_ID, principal))
                    .doesNotThrowAnyException();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Familia no encontrada
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Familia no encontrada")
    class FamilyNotFound {

        @Test
        @DisplayName("familyId no existe → NotFoundException")
        void shouldThrow_whenFamilyNotFound() {
            when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(regularUser));
            when(familyRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> validator.validateFamilyOwnership(99L, principal))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Familia no encontrada");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Check 1: Creador del núcleo (líder del nodo)
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Acceso como creador del núcleo familiar")
    class CreatorAccess {

        @Test
        @DisplayName("email coincide con createdBy → acceso permitido sin verificar miembro")
        void shouldAllow_whenUserIsCreator() {
            User creator = User.builder().email(EMAIL).build();
            family.setCreatedBy(creator);

            when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(regularUser));
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));

            assertThatCode(() -> validator.validateFamilyOwnership(FAM_ID, principal))
                    .doesNotThrowAnyException();

            verifyNoInteractions(memberRepository);
        }

        @Test
        @DisplayName("familia sin createdBy → continúa a verificación de miembro")
        void shouldContinueToMemberCheck_whenNoCreator() {
            family.setCreatedBy(null);
            FamilyMember member = FamilyMember.builder()
                    .email(EMAIL).family(family).active(true).build();

            when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(regularUser));
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
            when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member));

            assertThatCode(() -> validator.validateFamilyOwnership(FAM_ID, principal))
                    .doesNotThrowAnyException();

            verify(memberRepository).findByEmail(EMAIL);
        }

        @Test
        @DisplayName("email de createdBy distinto → continúa a verificación de miembro")
        void shouldContinueToMemberCheck_whenCreatorEmailDiffers() {
            User otherCreator = User.builder().email("otro@if.com").build();
            family.setCreatedBy(otherCreator);
            FamilyMember member = FamilyMember.builder()
                    .email(EMAIL).family(family).active(true).build();

            when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(regularUser));
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
            when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member));

            assertThatCode(() -> validator.validateFamilyOwnership(FAM_ID, principal))
                    .doesNotThrowAnyException();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Check 2: Verificación de miembro
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Acceso como miembro de la familia")
    class MemberAccess {

        @BeforeEach
        void stubNoCreator() {
            family.setCreatedBy(null);
            when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(regularUser));
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
        }

        @Test
        @DisplayName("miembro activo de la familia → acceso permitido")
        void shouldAllow_whenActiveMemberOfFamily() {
            FamilyMember member = FamilyMember.builder()
                    .email(EMAIL).family(family).active(true).build();
            when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member));

            assertThatCode(() -> validator.validateFamilyOwnership(FAM_ID, principal))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("miembro no encontrado → AccessDeniedException")
        void shouldThrow_whenMemberNotFound() {
            when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> validator.validateFamilyOwnership(FAM_ID, principal))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("No tienes permisos");
        }

        @Test
        @DisplayName("miembro de otra familia → AccessDeniedException 'Acceso denegado'")
        void shouldThrow_whenMemberBelongsToOtherFamily() {
            Family otherFamily = Family.builder().id(99L).name("Otra familia").build();
            FamilyMember memberOfOther = FamilyMember.builder()
                    .email(EMAIL).family(otherFamily).active(true).build();
            when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(memberOfOther));

            assertThatThrownBy(() -> validator.validateFamilyOwnership(FAM_ID, principal))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Acceso denegado");
        }

        @Test
        @DisplayName("miembro inactivo → AccessDeniedException 'Cuenta inactiva'")
        void shouldThrow_whenMemberIsInactive() {
            FamilyMember inactiveMember = FamilyMember.builder()
                    .email(EMAIL).family(family).active(false).build();
            when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(inactiveMember));

            assertThatThrownBy(() -> validator.validateFamilyOwnership(FAM_ID, principal))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("inactiva");
        }
    }
}
