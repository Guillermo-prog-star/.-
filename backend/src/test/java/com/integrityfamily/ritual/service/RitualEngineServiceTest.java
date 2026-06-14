package com.integrityfamily.ritual.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.domain.repository.*;
import com.integrityfamily.ritual.domain.FamilyRitual;
import com.integrityfamily.ritual.domain.RitualStatus;
import com.integrityfamily.ritual.domain.RitualType;
import com.integrityfamily.ritual.dto.RitualDto;
import com.integrityfamily.ritual.repository.FamilyRitualRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RitualEngineService")
class RitualEngineServiceTest {

    @Mock FamilyRitualRepository        ritualRepository;
    @Mock FamilyRepository              familyRepository;
    @Mock ImprovementPlanRepository     planRepository;
    @Mock TaskEvidenceRepository        evidenceRepository;
    @Mock FamilyLogbookRepository       logbookRepository;
    @Mock FamilyGratitudeEntryRepository gratitudeRepository;
    @Mock AiProvider                    aiProvider;
    @Spy  ObjectMapper                  objectMapper = new ObjectMapper();

    @InjectMocks RitualEngineService service;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private FamilyRitual ritual(long id, long familyId, RitualType type, RitualStatus status) {
        return FamilyRitual.builder()
                .id(id).familyId(familyId).ritualType(type).status(status)
                .title("Ritual " + type).description("Descripción de prueba")
                .triggerContext("Contexto " + type)
                .triggeredAt(LocalDateTime.now())
                .guidedSteps("[\"Paso 1\",\"Paso 2\"]")
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getActiveRituals
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getActiveRituals")
    class GetActiveRituals {

        @Test
        @DisplayName("dos rituales PENDING → retorna 2 DTOs con campos mapeados")
        void twoActiveRituals_returnsTwoDtos() {
            FamilyRitual r1 = ritual(1L, 10L, RitualType.DOMINGO_FAMILIAR, RitualStatus.PENDING);
            FamilyRitual r2 = ritual(2L, 10L, RitualType.CUMPLEANOS, RitualStatus.PENDING);
            when(ritualRepository.findByFamilyIdAndStatusOrderByTriggeredAtDesc(10L, RitualStatus.PENDING))
                    .thenReturn(List.of(r1, r2));

            List<RitualDto> dtos = service.getActiveRituals(10L);

            assertThat(dtos).hasSize(2);
            assertThat(dtos.get(0).id()).isEqualTo(1L);
            assertThat(dtos.get(0).familyId()).isEqualTo(10L);
            assertThat(dtos.get(0).status()).isEqualTo(RitualStatus.PENDING);
            assertThat(dtos.get(0).ritualType()).isEqualTo(RitualType.DOMINGO_FAMILIAR);
        }

        @Test
        @DisplayName("sin rituales activos → lista vacía")
        void noActiveRituals_emptyList() {
            when(ritualRepository.findByFamilyIdAndStatusOrderByTriggeredAtDesc(10L, RitualStatus.PENDING))
                    .thenReturn(List.of());

            assertThat(service.getActiveRituals(10L)).isEmpty();
        }

        @Test
        @DisplayName("guidedSteps JSON se deserializa a lista")
        void guidedSteps_parsedToList() {
            FamilyRitual r = ritual(1L, 10L, RitualType.DOMINGO_FAMILIAR, RitualStatus.PENDING);
            when(ritualRepository.findByFamilyIdAndStatusOrderByTriggeredAtDesc(10L, RitualStatus.PENDING))
                    .thenReturn(List.of(r));

            RitualDto dto = service.getActiveRituals(10L).get(0);

            assertThat(dto.guidedSteps()).containsExactly("Paso 1", "Paso 2");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getHistory
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getHistory")
    class GetHistory {

        @Test
        @DisplayName("31 rituales en DB → retorna solo los primeros 30")
        void thirtyOneRituals_returnsOnly30() {
            List<FamilyRitual> rituals = IntStream.rangeClosed(1, 31)
                    .mapToObj(i -> ritual((long) i, 5L, RitualType.FIN_DE_MES, RitualStatus.COMPLETED))
                    .toList();
            when(ritualRepository.findByFamilyIdOrderByTriggeredAtDesc(5L)).thenReturn(rituals);

            List<RitualDto> history = service.getHistory(5L);

            assertThat(history).hasSize(30);
        }

        @Test
        @DisplayName("historial vacío → lista vacía")
        void emptyHistory_emptyList() {
            when(ritualRepository.findByFamilyIdOrderByTriggeredAtDesc(5L)).thenReturn(List.of());

            assertThat(service.getHistory(5L)).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // activate
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("activate")
    class Activate {

        @Test
        @DisplayName("ritual existente → status=ACTIVE, activatedAt poblado, save() invocado")
        void existingRitual_activatesAndSaves() {
            FamilyRitual r = ritual(1L, 10L, RitualType.DOMINGO_FAMILIAR, RitualStatus.PENDING);
            when(ritualRepository.findById(1L)).thenReturn(Optional.of(r));
            when(ritualRepository.save(r)).thenReturn(r);

            RitualDto dto = service.activate(1L);

            assertThat(dto.status()).isEqualTo(RitualStatus.ACTIVE);
            assertThat(r.getActivatedAt()).isNotNull();
            verify(ritualRepository).save(r);
        }

        @Test
        @DisplayName("ritual no encontrado → IllegalArgumentException")
        void notFound_throwsException() {
            when(ritualRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.activate(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("99");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // complete
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("complete")
    class Complete {

        @Test
        @DisplayName("ritual existente → status=COMPLETED, completedAt poblado")
        void existingRitual_completesAndSaves() {
            FamilyRitual r = ritual(2L, 10L, RitualType.CUMPLEANOS, RitualStatus.ACTIVE);
            when(ritualRepository.findById(2L)).thenReturn(Optional.of(r));
            when(ritualRepository.save(r)).thenReturn(r);

            RitualDto dto = service.complete(2L);

            assertThat(dto.status()).isEqualTo(RitualStatus.COMPLETED);
            assertThat(r.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("ritual no encontrado → IllegalArgumentException")
        void notFound_throwsException() {
            when(ritualRepository.findById(98L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.complete(98L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // dismiss
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("dismiss")
    class Dismiss {

        @Test
        @DisplayName("ritual existente → status=DISMISSED, dismissedAt poblado, save() invocado")
        void existingRitual_dismisses() {
            FamilyRitual r = ritual(3L, 10L, RitualType.SIN_ACTIVIDAD, RitualStatus.PENDING);
            when(ritualRepository.findById(3L)).thenReturn(Optional.of(r));
            when(ritualRepository.save(r)).thenReturn(r);

            service.dismiss(3L);

            assertThat(r.getStatus()).isEqualTo(RitualStatus.DISMISSED);
            assertThat(r.getDismissedAt()).isNotNull();
            verify(ritualRepository).save(r);
        }

        @Test
        @DisplayName("ritual no encontrado → IllegalArgumentException")
        void notFound_throwsException() {
            when(ritualRepository.findById(97L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.dismiss(97L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // buildRitualContextBlock
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("buildRitualContextBlock")
    class ContextBlock {

        @Test
        @DisplayName("rituales activos → bloque de texto con título y contexto")
        void activeRituals_buildsContextString() {
            FamilyRitual r1 = ritual(1L, 10L, RitualType.DOMINGO_FAMILIAR, RitualStatus.PENDING);
            FamilyRitual r2 = ritual(2L, 10L, RitualType.CUMPLEANOS, RitualStatus.PENDING);
            when(ritualRepository.findByFamilyIdAndStatusOrderByTriggeredAtDesc(10L, RitualStatus.PENDING))
                    .thenReturn(List.of(r1, r2));

            String block = service.buildRitualContextBlock(10L);

            assertThat(block).isNotNull();
            assertThat(block).contains("Rituales activos");
            assertThat(block).contains("Ritual DOMINGO_FAMILIAR");
            assertThat(block).contains("Contexto DOMINGO_FAMILIAR");
        }

        @Test
        @DisplayName("sin rituales activos → retorna null")
        void noActiveRituals_returnsNull() {
            when(ritualRepository.findByFamilyIdAndStatusOrderByTriggeredAtDesc(10L, RitualStatus.PENDING))
                    .thenReturn(List.of());

            assertThat(service.buildRitualContextBlock(10L)).isNull();
        }

        @Test
        @DisplayName("ritual sin triggerContext → no rompe el formato, solo muestra título")
        void noTriggerContext_titleOnly() {
            FamilyRitual r = FamilyRitual.builder()
                    .id(1L).familyId(10L).ritualType(RitualType.FIN_DE_MES)
                    .status(RitualStatus.PENDING)
                    .title("Cierre de mes").triggerContext(null)
                    .guidedSteps("[]")
                    .build();
            when(ritualRepository.findByFamilyIdAndStatusOrderByTriggeredAtDesc(10L, RitualStatus.PENDING))
                    .thenReturn(List.of(r));

            String block = service.buildRitualContextBlock(10L);

            assertThat(block).contains("Cierre de mes");
            // No debe lanzar NPE ni incluir " — null"
            assertThat(block).doesNotContain("null");
        }
    }
}
