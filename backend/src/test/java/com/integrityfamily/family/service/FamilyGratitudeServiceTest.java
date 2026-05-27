package com.integrityfamily.family.service;

import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyGratitudeEntry;
import com.integrityfamily.dto.CreateFamilyGratitudeRequest;
import com.integrityfamily.dto.FamilyGratitudeResponse;
import com.integrityfamily.domain.repository.FamilyGratitudeEntryRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link FamilyGratitudeService}.
 *
 * No levanta contexto Spring — usa Mockito strict stubs.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FamilyGratitudeService — Unit Tests")
class FamilyGratitudeServiceTest {

    @Mock FamilyRepository familyRepository;
    @Mock FamilyGratitudeEntryRepository gratitudeRepository;

    @InjectMocks FamilyGratitudeService service;

    private Family family;

    @BeforeEach
    void setUp() {
        family = Family.builder()
                .id(1L)
                .name("Los García")
                .familyCode("IF-CO-BOG-2026-0001")
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  create()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("create() — registrar agradecimiento")
    class Create {

        @Test
        @DisplayName("éxito: persiste la entrada y retorna el DTO con los campos correctos")
        void create_success() {
            var request = new CreateFamilyGratitudeRequest(1L, "Papá", "Mamá", "Gracias por tu apoyo");

            FamilyGratitudeEntry saved = new FamilyGratitudeEntry(
                    family, "Papá", "Mamá", "Gracias por tu apoyo"
            );
            // Simular ID asignado por la BD
            saved.setId(10L);

            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(gratitudeRepository.save(any(FamilyGratitudeEntry.class))).thenReturn(saved);

            FamilyGratitudeResponse response = service.create(request);

            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.familyId()).isEqualTo(1L);
            assertThat(response.fromMember()).isEqualTo("Papá");
            assertThat(response.toMember()).isEqualTo("Mamá");
            assertThat(response.description()).isEqualTo("Gracias por tu apoyo");
            verify(gratitudeRepository).save(any(FamilyGratitudeEntry.class));
        }

        @Test
        @DisplayName("familia no existe → BusinessException FAMILY_NOT_FOUND 404")
        void create_familyNotFound_throwsBusinessException() {
            var request = new CreateFamilyGratitudeRequest(99L, "Papá", "Mamá", "Gracias");

            when(familyRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo("FAMILY_NOT_FOUND");
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });

            verify(gratitudeRepository, never()).save(any());
        }

        @Test
        @DisplayName("la entrada guardada propaga los campos de la request al DTO")
        void create_mapsAllFieldsFromRequest() {
            var request = new CreateFamilyGratitudeRequest(1L, "Hijo", "Abuela", "Te quiero mucho");

            FamilyGratitudeEntry saved = new FamilyGratitudeEntry(
                    family, "Hijo", "Abuela", "Te quiero mucho"
            );
            saved.setId(5L);

            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(gratitudeRepository.save(any())).thenReturn(saved);

            FamilyGratitudeResponse response = service.create(request);

            assertThat(response.fromMember()).isEqualTo("Hijo");
            assertThat(response.toMember()).isEqualTo("Abuela");
            assertThat(response.description()).isEqualTo("Te quiero mucho");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  findByFamily()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findByFamily() — listar agradecimientos por familia")
    class FindByFamily {

        @Test
        @DisplayName("retorna lista mapeada en orden descendente de creación")
        void findByFamily_returnsMappedList() {
            LocalDateTime earlier = LocalDateTime.now().minusHours(2);
            LocalDateTime later   = LocalDateTime.now().minusHours(1);

            FamilyGratitudeEntry entry1 = FamilyGratitudeEntry.builder()
                    .id(1L).family(family)
                    .fromMember("Papá").toMember("Mamá")
                    .description("Primera").createdAt(earlier)
                    .build();

            FamilyGratitudeEntry entry2 = FamilyGratitudeEntry.builder()
                    .id(2L).family(family)
                    .fromMember("Hijo").toMember("Papá")
                    .description("Segunda").createdAt(later)
                    .build();

            // Repositorio ya devuelve en orden desc — el servicio sólo mapea
            when(gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of(entry2, entry1));

            List<FamilyGratitudeResponse> result = service.findByFamily(1L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).id()).isEqualTo(2L);
            assertThat(result.get(0).fromMember()).isEqualTo("Hijo");
            assertThat(result.get(1).id()).isEqualTo(1L);
            assertThat(result.get(1).fromMember()).isEqualTo("Papá");
        }

        @Test
        @DisplayName("lista vacía cuando la familia no tiene agradecimientos")
        void findByFamily_emptyList() {
            when(gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of());

            List<FamilyGratitudeResponse> result = service.findByFamily(1L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("todos los campos del DTO coinciden con los de la entidad")
        void findByFamily_fieldMapping() {
            LocalDateTime now = LocalDateTime.of(2026, 5, 26, 10, 0);

            FamilyGratitudeEntry entry = FamilyGratitudeEntry.builder()
                    .id(7L).family(family)
                    .fromMember("Mamá").toMember("Hija")
                    .description("Eres increíble").createdAt(now)
                    .build();

            when(gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of(entry));

            FamilyGratitudeResponse dto = service.findByFamily(1L).get(0);

            assertThat(dto.id()).isEqualTo(7L);
            assertThat(dto.familyId()).isEqualTo(1L);
            assertThat(dto.fromMember()).isEqualTo("Mamá");
            assertThat(dto.toMember()).isEqualTo("Hija");
            assertThat(dto.description()).isEqualTo("Eres increíble");
            assertThat(dto.createdAt()).isEqualTo(now);
        }
    }
}
