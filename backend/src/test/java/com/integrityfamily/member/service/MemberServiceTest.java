package com.integrityfamily.member.service;

import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.MemberRepository;
import com.integrityfamily.member.dto.MemberRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Pruebas unitarias de MemberService.
 *
 * Cubre: findAll(), findByFamily(), findById(), createMember() (validaciones + defaults),
 * update(), delete(). El evento RabbitMQ se prueba implícitamente: errores al enviar
 * no deben propagar (la excepción es capturada en catch).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService — Unit Tests")
class MemberServiceTest {

    @Mock MemberRepository memberRepository;
    @Mock FamilyRepository familyRepository;
    @Mock RabbitTemplate   rabbitTemplate;

    @InjectMocks
    MemberService memberService;

    private Family family;
    private FamilyMember member;

    @BeforeEach
    void setUp() {
        family = Family.builder()
                .id(1L)
                .name("Familia López")
                .familyCode("IF-2026-TEST")
                .build();

        member = new FamilyMember();
        member.setId(10L);
        member.setFullName("Carlos Gómez");
        member.setRole("PADRE");
        member.setActive(true);
        member.setFamily(family);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  findAll() / findByFamily() / findById()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Consultas")
    class Queries {

        @Test
        @DisplayName("findAll() delega en el repositorio")
        void shouldReturnAll() {
            when(memberRepository.findAll()).thenReturn(List.of(member));
            assertThat(memberService.findAll()).containsExactly(member);
        }

        @Test
        @DisplayName("findByFamily() delega en el repositorio con el familyId dado")
        void shouldReturnByFamily() {
            when(memberRepository.findByFamilyId(1L)).thenReturn(List.of(member));
            assertThat(memberService.findByFamily(1L)).containsExactly(member);
        }

        @Test
        @DisplayName("findById() existente → devuelve el miembro")
        void shouldReturnMember_whenExists() {
            when(memberRepository.findById(10L)).thenReturn(Optional.of(member));
            assertThat(memberService.findById(10L)).isEqualTo(member);
        }

        @Test
        @DisplayName("findById() inexistente → RuntimeException")
        void shouldThrow_whenNotFound() {
            when(memberRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> memberService.findById(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("no encontrado");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  createMember()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createMember()")
    class CreateMember {

        @BeforeEach
        void stubFamily() {
            // lenient: shouldThrow_whenFamilyNotFound never calls findById(1L)
            lenient().when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        }

        @Test
        @DisplayName("Familia no encontrada → BusinessException NOT_FOUND")
        void shouldThrow_whenFamilyNotFound() {
            when(familyRepository.findById(99L)).thenReturn(Optional.empty());

            MemberRequest req = new MemberRequest(
                    "Test", "HIJO", null, null, null, null, null, 99L);

            assertThatThrownBy(() -> memberService.createMember(req))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException bex = (BusinessException) ex;
                        assertThat(bex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(bex.getCode()).isEqualTo("FAMILY_NOT_FOUND");
                    });
        }

        @Test
        @DisplayName("Creación exitosa → guarda miembro con campos correctos")
        void shouldSaveMember_withCorrectFields() {
            when(memberRepository.save(any(FamilyMember.class))).thenAnswer(i -> {
                FamilyMember m = i.getArgument(0);
                m.setId(20L);
                return m;
            });

            MemberRequest req = new MemberRequest(
                    "Ana Martínez", "MADRE", 35, 70, 80, "ana@test.com", "3001234567", 1L);
            FamilyMember result = memberService.createMember(req);

            ArgumentCaptor<FamilyMember> captor = ArgumentCaptor.forClass(FamilyMember.class);
            verify(memberRepository).save(captor.capture());

            FamilyMember saved = captor.getValue();
            assertThat(saved.getFullName()).isEqualTo("Ana Martínez");
            assertThat(saved.getRole()).isEqualTo("MADRE");
            assertThat(saved.getAge()).isEqualTo(35);
            assertThat(saved.getAutonomyLevel()).isEqualTo(70);
            assertThat(saved.getResponsibilityLevel()).isEqualTo(80);
            assertThat(saved.getFamily()).isEqualTo(family);
            assertThat(saved.isActive()).isTrue();
        }

        @Test
        @DisplayName("fullName con espacio → firstName = primera parte")
        void shouldDerivFirstName_fromFullName() {
            when(memberRepository.save(any(FamilyMember.class))).thenAnswer(i -> i.getArgument(0));

            memberService.createMember(new MemberRequest(
                    "Pedro Ramírez", "PADRE", 40, null, null, null, null, 1L));

            ArgumentCaptor<FamilyMember> captor = ArgumentCaptor.forClass(FamilyMember.class);
            verify(memberRepository).save(captor.capture());
            assertThat(captor.getValue().getFirstName()).isEqualTo("Pedro");
        }

        @Test
        @DisplayName("fullName sin espacio → firstName = fullName completo")
        void shouldUseFullNameAsFirstName_whenNoSpace() {
            when(memberRepository.save(any(FamilyMember.class))).thenAnswer(i -> i.getArgument(0));

            memberService.createMember(new MemberRequest(
                    "Luisa", "HIJA", 12, null, null, null, null, 1L));

            ArgumentCaptor<FamilyMember> captor = ArgumentCaptor.forClass(FamilyMember.class);
            verify(memberRepository).save(captor.capture());
            assertThat(captor.getValue().getFirstName()).isEqualTo("Luisa");
        }

        @Test
        @DisplayName("autonomyLevel null → default 50")
        void shouldDefault50_forAutonomyLevel_whenNull() {
            when(memberRepository.save(any(FamilyMember.class))).thenAnswer(i -> i.getArgument(0));

            memberService.createMember(new MemberRequest(
                    "X", "HIJO", null, null, null, null, null, 1L));

            ArgumentCaptor<FamilyMember> captor = ArgumentCaptor.forClass(FamilyMember.class);
            verify(memberRepository).save(captor.capture());
            assertThat(captor.getValue().getAutonomyLevel()).isEqualTo(50);
            assertThat(captor.getValue().getResponsibilityLevel()).isEqualTo(50);
        }

        @Test
        @DisplayName("Error en RabbitMQ → no propaga excepción (capturado internamente)")
        void shouldNotThrow_whenRabbitFails() {
            when(memberRepository.save(any(FamilyMember.class))).thenAnswer(i -> i.getArgument(0));
            doThrow(new RuntimeException("broker down")).when(rabbitTemplate)
                    .convertAndSend(anyString(), anyString(), (Object) any());

            // No debe lanzar aunque RabbitMQ falle
            assertThat(memberService.createMember(
                    new MemberRequest("Test", "HIJO", null, null, null, null, null, 1L)))
                    .isNotNull();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  update()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("ID existente → actualiza fullName, role y active")
        void shouldUpdateFields() {
            when(memberRepository.findById(10L)).thenReturn(Optional.of(member));
            when(memberRepository.save(any(FamilyMember.class))).thenAnswer(i -> i.getArgument(0));

            FamilyMember patch = new FamilyMember();
            patch.setFullName("Carlos Actualizado");
            patch.setRole("HIJO");
            patch.setActive(false);

            FamilyMember result = memberService.update(10L, patch);

            assertThat(result.getFullName()).isEqualTo("Carlos Actualizado");
            assertThat(result.getRole()).isEqualTo("HIJO");
            assertThat(result.isActive()).isFalse();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  delete()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("Llama deleteById con el ID correcto")
        void shouldCallDeleteById() {
            memberService.delete(10L);
            verify(memberRepository).deleteById(10L);
        }
    }
}
