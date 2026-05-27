package com.integrityfamily.bitacora.service;

import com.integrityfamily.bitacora.dto.BitacoraRequest;
import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyLogbookEntry;
import com.integrityfamily.domain.LogbookStatus;
import com.integrityfamily.domain.repository.FamilyLogbookRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link BitacoraService}.
 *
 * No levanta contexto Spring — usa Mockito strict stubs.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BitacoraService — Unit Tests")
class BitacoraServiceTest {

    @Mock FamilyLogbookRepository logbookRepository;
    @Mock FamilyRepository familyRepository;

    @InjectMocks BitacoraService service;

    private Family family;

    @BeforeEach
    void setUp() {
        family = Family.builder()
                .id(1L)
                .name("Los García")
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  createEntry()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createEntry() — registrar hito cognitivo en bitácora")
    class CreateEntry {

        private BitacoraRequest buildRequest(Long familyId) {
            return new BitacoraRequest(
                    familyId,
                    "TASK",
                    42L,
                    "Aprendimos a comunicarnos mejor",
                    "Si practicamos escucha activa mejoraremos",
                    "Reunión semanal de reflexión",
                    "Resultado pendiente"
            );
        }

        @Test
        @DisplayName("éxito: persiste la entrada y retorna el objeto guardado")
        void createEntry_success_returnsSavedEntry() {
            BitacoraRequest req = buildRequest(1L);

            FamilyLogbookEntry saved = new FamilyLogbookEntry();
            saved.setId(100L);
            saved.setFamily(family);
            saved.setStatus(LogbookStatus.OPEN);
            saved.setCreatedBy("IA-MOTOR");

            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(logbookRepository.save(any(FamilyLogbookEntry.class))).thenReturn(saved);

            FamilyLogbookEntry result = service.createEntry(req);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
            verify(logbookRepository).save(any(FamilyLogbookEntry.class));
        }

        @Test
        @DisplayName("mapeo: los campos request se propagan correctamente a la entidad")
        void createEntry_mapsRequestFieldsCorrectly() {
            BitacoraRequest req = buildRequest(1L);

            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(logbookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            FamilyLogbookEntry result = service.createEntry(req);

            assertThat(result.getSituation())
                    .contains("TASK").contains("42");
            assertThat(result.getDifficultyDetected())
                    .contains("Si practicamos escucha activa mejoraremos");
            assertThat(result.getUnderstanding())
                    .contains("Aprendimos a comunicarnos mejor");
            assertThat(result.getCorrectionAction())
                    .isEqualTo("Reunión semanal de reflexión");
            assertThat(result.getFamilyAgreement())
                    .isEqualTo("Aprendimos a comunicarnos mejor");
            assertThat(result.getCreatedBy()).isEqualTo("IA-MOTOR");
            assertThat(result.getEmotionIdentified()).isEqualTo("Expectativa Evolutiva");
        }

        @Test
        @DisplayName("estado inicial: la entrada queda con status OPEN")
        void createEntry_statusIsOpen() {
            BitacoraRequest req = buildRequest(1L);

            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(logbookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            FamilyLogbookEntry result = service.createEntry(req);

            assertThat(result.getStatus()).isEqualTo(LogbookStatus.OPEN);
        }

        @Test
        @DisplayName("familia no existe → BusinessException FAMILY_NOT_FOUND 404")
        void createEntry_familyNotFound_throwsBusinessException() {
            BitacoraRequest req = buildRequest(99L);

            when(familyRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createEntry(req))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo("FAMILY_NOT_FOUND");
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });

            verify(logbookRepository, never()).save(any());
        }

        @Test
        @DisplayName("la familia asignada a la entrada corresponde a la encontrada por ID")
        void createEntry_assignsCorrectFamily() {
            BitacoraRequest req = buildRequest(1L);
            ArgumentCaptor<FamilyLogbookEntry> captor = ArgumentCaptor.forClass(FamilyLogbookEntry.class);

            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(logbookRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.createEntry(req);

            assertThat(captor.getValue().getFamily()).isSameAs(family);
        }
    }
}
