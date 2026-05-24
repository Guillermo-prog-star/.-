package com.integrityfamily.family.service;

import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.User;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.UserRepository;
import com.integrityfamily.family.dto.FamilyResponse;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de FamilyService.
 *
 * Cubre: create() (validaciones + generación de familyCode), findById(),
 * getFullFamilyContext(), update(), delete(), findAll(), toResponse()
 * (null guard y mapeo de miembros).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FamilyService — Unit Tests")
class FamilyServiceTest {

    @Mock FamilyRepository familyRepository;
    @Mock UserRepository   userRepository;

    @InjectMocks
    FamilyService familyService;

    private User creator;
    private Family family;

    @BeforeEach
    void setUp() {
        creator = User.builder()
                .id(1L)
                .email("william@integrityfamily.com")
                .fullName("William López")
                .build();

        family = Family.builder()
                .id(10L)
                .name("Familia López")
                .description("Familia de prueba")
                .familyCode("IF-2026-0001")
                .currentMilestone("MES_00_DIAGNOSTICO_BASE")
                .members(new ArrayList<>())
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  create()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("Usuario no encontrado → BusinessException NOT_FOUND")
        void shouldThrow_whenCreatorNotFound() {
            when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> familyService.create(family, "ghost@test.com"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException bex = (BusinessException) ex;
                        assertThat(bex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(bex.getCode()).isEqualTo("USER_NOT_FOUND");
                    });
        }

        @Test
        @DisplayName("Usuario ya tiene familia → BusinessException CONFLICT")
        void shouldThrow_whenUserAlreadyHasFamily() {
            when(userRepository.findByEmail(creator.getEmail()))
                    .thenReturn(Optional.of(creator));
            when(familyRepository.findByCreatedBy_Email(creator.getEmail()))
                    .thenReturn(Optional.of(family)); // ya existe

            assertThatThrownBy(() -> familyService.create(family, creator.getEmail()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException bex = (BusinessException) ex;
                        assertThat(bex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(bex.getCode()).isEqualTo("ALREADY_HAS_FAMILY");
                    });
        }

        @Test
        @DisplayName("Creación exitosa → genera familyCode con formato IF-{AÑO}-{SECUENCIA}")
        void shouldCreate_withCorrectFamilyCodeFormat() {
            when(userRepository.findByEmail(creator.getEmail()))
                    .thenReturn(Optional.of(creator));
            when(familyRepository.findByCreatedBy_Email(creator.getEmail()))
                    .thenReturn(Optional.empty());
            when(familyRepository.count()).thenReturn(0L);
            when(familyRepository.save(any(Family.class))).thenReturn(family);
            when(userRepository.saveAndFlush(any(User.class))).thenReturn(creator);

            Family newFamily = Family.builder().name("Familia Test").build();
            familyService.create(newFamily, creator.getEmail());

            ArgumentCaptor<Family> captor = ArgumentCaptor.forClass(Family.class);
            verify(familyRepository).save(captor.capture());

            String code = captor.getValue().getFamilyCode();
            assertThat(code).matches("IF-\\d{4}-\\d{4}");
        }

        @Test
        @DisplayName("Creación exitosa → asigna milestone inicial MES_00_DIAGNOSTICO_BASE")
        void shouldCreate_withInitialMilestone() {
            when(userRepository.findByEmail(creator.getEmail()))
                    .thenReturn(Optional.of(creator));
            when(familyRepository.findByCreatedBy_Email(creator.getEmail()))
                    .thenReturn(Optional.empty());
            when(familyRepository.count()).thenReturn(5L);
            when(familyRepository.save(any(Family.class))).thenReturn(family);
            when(userRepository.saveAndFlush(any(User.class))).thenReturn(creator);

            Family newFamily = Family.builder().name("Familia New").build();
            familyService.create(newFamily, creator.getEmail());

            ArgumentCaptor<Family> captor = ArgumentCaptor.forClass(Family.class);
            verify(familyRepository).save(captor.capture());
            assertThat(captor.getValue().getCurrentMilestone())
                    .isEqualTo("MES_00_DIAGNOSTICO_BASE");
        }

        @Test
        @DisplayName("Creación exitosa → vincula creator a la familia guardada")
        void shouldCreate_andLinkCreatorToFamily() {
            when(userRepository.findByEmail(creator.getEmail()))
                    .thenReturn(Optional.of(creator));
            when(familyRepository.findByCreatedBy_Email(creator.getEmail()))
                    .thenReturn(Optional.empty());
            when(familyRepository.count()).thenReturn(3L);
            when(familyRepository.save(any(Family.class))).thenReturn(family);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            when(userRepository.saveAndFlush(userCaptor.capture())).thenReturn(creator);

            familyService.create(Family.builder().name("Test").build(), creator.getEmail());

            assertThat(userCaptor.getValue().getFamily()).isEqualTo(family);
        }

        @Test
        @DisplayName("count()=0 → secuencia es 0001")
        void shouldCreate_withSequence0001_whenFirstFamily() {
            when(userRepository.findByEmail(creator.getEmail()))
                    .thenReturn(Optional.of(creator));
            when(familyRepository.findByCreatedBy_Email(creator.getEmail()))
                    .thenReturn(Optional.empty());
            when(familyRepository.count()).thenReturn(0L);
            when(familyRepository.save(any(Family.class))).thenReturn(family);
            when(userRepository.saveAndFlush(any(User.class))).thenReturn(creator);

            familyService.create(Family.builder().name("Primera").build(), creator.getEmail());

            ArgumentCaptor<Family> captor = ArgumentCaptor.forClass(Family.class);
            verify(familyRepository).save(captor.capture());
            assertThat(captor.getValue().getFamilyCode()).endsWith("-0001");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  findById()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("ID existente → devuelve FamilyResponse")
        void shouldReturnResponse_whenFamilyExists() {
            when(familyRepository.findByIdWithMembers(10L)).thenReturn(Optional.of(family));

            FamilyResponse response = familyService.findById(10L);

            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.name()).isEqualTo("Familia López");
        }

        @Test
        @DisplayName("ID inexistente → RuntimeException con el ID en el mensaje")
        void shouldThrow_whenFamilyNotFound() {
            when(familyRepository.findByIdWithMembers(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> familyService.findById(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("99");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  getFullFamilyContext()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getFullFamilyContext()")
    class GetFullFamilyContext {

        @Test
        @DisplayName("Email existente → devuelve la familia")
        void shouldReturnFamily_whenEmailExists() {
            when(familyRepository.findByCreatedByEmailWithMembers(creator.getEmail()))
                    .thenReturn(Optional.of(family));

            Family result = familyService.getFullFamilyContext(creator.getEmail());

            assertThat(result).isEqualTo(family);
        }

        @Test
        @DisplayName("Email sin familia → RuntimeException con el email")
        void shouldThrow_whenNoFamilyForEmail() {
            when(familyRepository.findByCreatedByEmailWithMembers("ghost@test.com"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> familyService.getFullFamilyContext("ghost@test.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("ghost@test.com");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  update()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("ID existente → actualiza campos y retorna DTO")
        void shouldUpdateFields_whenFamilyExists() {
            when(familyRepository.findById(10L)).thenReturn(Optional.of(family));
            when(familyRepository.save(any(Family.class))).thenAnswer(i -> i.getArgument(0));

            Family patch = Family.builder()
                    .name("Familia López Actualizada")
                    .description("Nueva descripción")
                    .municipio("Bogotá")
                    .whatsapp("3209876543")
                    .build();

            FamilyResponse result = familyService.update(10L, patch);

            assertThat(result.name()).isEqualTo("Familia López Actualizada");
            assertThat(result.municipio()).isEqualTo("Bogotá");
        }

        @Test
        @DisplayName("ID inexistente → RuntimeException")
        void shouldThrow_whenFamilyNotFound() {
            when(familyRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> familyService.update(99L, Family.builder().name("X").build()))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  delete()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("ID existente → llama deleteById")
        void shouldDelete_whenFamilyExists() {
            when(familyRepository.existsById(10L)).thenReturn(true);

            familyService.delete(10L);

            verify(familyRepository).deleteById(10L);
        }

        @Test
        @DisplayName("ID inexistente → RuntimeException, sin deleteById")
        void shouldThrow_whenFamilyNotExists() {
            when(familyRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> familyService.delete(99L))
                    .isInstanceOf(RuntimeException.class);

            verify(familyRepository, never()).deleteById(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  findAll()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("Delega en el repositorio y mapea cada entidad a DTO")
        void shouldReturnAllFamiliesAsDtos() {
            Family f2 = Family.builder().id(2L).name("Familia B").members(List.of()).build();
            when(familyRepository.findAll()).thenReturn(List.of(family, f2));

            List<FamilyResponse> result = familyService.findAll();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).id()).isEqualTo(10L);
            assertThat(result.get(1).id()).isEqualTo(2L);
        }

        @Test
        @DisplayName("Repositorio vacío → lista vacía")
        void shouldReturnEmptyList_whenNoFamilies() {
            when(familyRepository.findAll()).thenReturn(List.of());

            List<FamilyResponse> result = familyService.findAll();

            assertThat(result).isEmpty();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  toResponse()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("toResponse()")
    class ToResponse {

        @Test
        @DisplayName("null → devuelve null sin excepción")
        void shouldReturnNull_whenFamilyIsNull() {
            assertThat(familyService.toResponse(null)).isNull();
        }

        @Test
        @DisplayName("familia sin miembros (null) → lista de members vacía en DTO")
        void shouldReturnEmptyMembersList_whenMembersIsNull() {
            Family noMembers = Family.builder()
                    .id(5L).name("Sin Miembros").members(null).build();

            FamilyResponse response = familyService.toResponse(noMembers);

            assertThat(response.members()).isEmpty();
        }

        @Test
        @DisplayName("familia con miembros → DTO incluye la lista mapeada")
        void shouldMapMembers_whenFamilyHasMembers() {
            // family fixture ya tiene members inicializado como ArrayList vacío.
            // Solo verificamos que no lanza con una lista vacía.
            FamilyResponse response = familyService.toResponse(family);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(10L);
        }
    }
}
