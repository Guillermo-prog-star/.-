package com.integrityfamily.familyhome.adapter;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.Role;
import com.integrityfamily.domain.User;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.MemberRepository;
import com.integrityfamily.domain.repository.UserRepository;
import com.integrityfamily.dto.home.ViewerRole;
import com.integrityfamily.familyhome.security.FamilyIdentifierBridge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FamilyMembershipQueryPortAdapter — Unit Tests")
class FamilyMembershipQueryPortAdapterTest {

    @Mock FamilyRepository familyRepository;
    @Mock MemberRepository memberRepository;
    @Mock UserRepository userRepository;
    @Mock FamilyIdentifierBridge idBridge;

    FamilyMembershipQueryPortAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new FamilyMembershipQueryPortAdapter(familyRepository, memberRepository, userRepository, idBridge);
    }

    @Nested
    @DisplayName("getPermissions()")
    class GetPermissions {

        @Test
        @DisplayName("ROLE_ADMIN sin membresía en la familia igual recibe VIEW_FAMILY_HUD y VIEW_PROFESSIONAL_HUD")
        void admin_getsHudViewPermissions_evenWithoutFamilyMembership() {
            UUID familyId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            User admin = User.builder()
                    .id(1L)
                    .email("admin@integrityfamily.com")
                    .roles(List.of(Role.builder().id(1L).name("ROLE_ADMIN").build()))
                    .build();

            when(idBridge.resolveFamilyId(familyId)).thenReturn(Optional.of(5L));
            when(idBridge.resolveUserId(userId)).thenReturn(Optional.of(1L));
            when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

            List<String> permissions = adapter.getPermissions(familyId, userId);

            // Bug corregido: sin estos dos permisos, HudAuthorizationPolicy bloqueaba
            // al admin del HUD Profesional pese al bypass total en el resto del sistema
            // (SecurityValidator, FamilySecurityEvaluator, isMember() de esta misma clase).
            assertThat(permissions).contains("VIEW_FAMILY_HUD", "VIEW_PROFESSIONAL_HUD");
        }

        @Test
        @DisplayName("miembro regular (no admin, no creador) solo recibe READ")
        void regularMember_getsReadOnly() {
            UUID familyId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            User member = User.builder()
                    .id(2L)
                    .email("member@example.com")
                    .roles(List.of())
                    .build();

            when(idBridge.resolveFamilyId(familyId)).thenReturn(Optional.of(5L));
            when(idBridge.resolveUserId(userId)).thenReturn(Optional.of(2L));
            when(userRepository.findById(2L)).thenReturn(Optional.of(member));
            when(familyRepository.findById(5L)).thenReturn(Optional.empty());

            List<String> permissions = adapter.getPermissions(familyId, userId);

            assertThat(permissions).containsExactly("READ");
        }

        @Test
        @DisplayName("creador de la familia (no admin) recibe READ y WRITE, comparando por ID sin tocar getEmail() del proxy")
        void creator_getsReadWrite_comparedById() {
            UUID familyId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            User creator = User.builder().id(3L).email("creador@example.com").roles(List.of()).build();
            // Simula el proxy Hibernate real: getId() resuelto, getEmail() no se debe invocar aquí.
            User createdByProxy = org.mockito.Mockito.mock(User.class);
            when(createdByProxy.getId()).thenReturn(3L);
            Family family = Family.builder().id(5L).createdBy(createdByProxy).build();

            when(idBridge.resolveFamilyId(familyId)).thenReturn(Optional.of(5L));
            when(idBridge.resolveUserId(userId)).thenReturn(Optional.of(3L));
            when(userRepository.findById(3L)).thenReturn(Optional.of(creator));
            when(familyRepository.findById(5L)).thenReturn(Optional.of(family));

            List<String> permissions = adapter.getPermissions(familyId, userId);

            assertThat(permissions).containsExactly("READ", "WRITE");
            org.mockito.Mockito.verify(createdByProxy, org.mockito.Mockito.never()).getEmail();
        }
    }

    @Nested
    @DisplayName("isMember()")
    class IsMember {

        @Test
        @DisplayName("creador de la familia (no admin) es miembro, comparando por ID sin tocar getEmail() del proxy")
        void creator_isMember_comparedById() {
            UUID familyId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            User creator = User.builder().id(3L).email("creador@example.com").roles(List.of()).build();
            User createdByProxy = org.mockito.Mockito.mock(User.class);
            when(createdByProxy.getId()).thenReturn(3L);
            Family family = Family.builder().id(5L).createdBy(createdByProxy).build();

            when(idBridge.resolveFamilyId(familyId)).thenReturn(Optional.of(5L));
            when(idBridge.resolveUserId(userId)).thenReturn(Optional.of(3L));
            when(userRepository.findById(3L)).thenReturn(Optional.of(creator));
            when(familyRepository.findById(5L)).thenReturn(Optional.of(family));

            boolean isMember = adapter.isMember(familyId, userId);

            assertThat(isMember).isTrue();
            org.mockito.Mockito.verify(createdByProxy, org.mockito.Mockito.never()).getEmail();
        }
    }

    @Nested
    @DisplayName("getRole()")
    class GetRole {

        @Test
        @DisplayName("ROLE_ADMIN sin membresía activa cae a ADULT_MEMBER (getPermissions es lo que hoy le da acceso real)")
        void admin_withoutActiveMembership_fallsBackToAdultMember() {
            UUID familyId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            User admin = User.builder()
                    .id(1L)
                    .email("admin@integrityfamily.com")
                    .roles(List.of(Role.builder().id(1L).name("ROLE_ADMIN").build()))
                    .build();

            when(idBridge.resolveFamilyId(familyId)).thenReturn(Optional.of(5L));
            when(idBridge.resolveUserId(userId)).thenReturn(Optional.of(1L));
            when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
            when(memberRepository.findByEmail("admin@integrityfamily.com")).thenReturn(Optional.empty());

            ViewerRole role = adapter.getRole(familyId, userId);

            assertThat(role).isEqualTo(ViewerRole.ADULT_MEMBER);
        }
    }
}
