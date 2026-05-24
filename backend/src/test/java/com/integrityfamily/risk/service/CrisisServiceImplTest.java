package com.integrityfamily.risk.service;

import com.integrityfamily.ai.dto.AiContext;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.ai.service.ContextSynthesizer;
import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.common.service.WhatsAppService;
import com.integrityfamily.domain.CriticalDay;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.repository.CriticalDayRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de CrisisServiceImpl.
 *
 * Cubre: registerCrisis() (family not-found, AI failure fallback, success, WhatsApp
 * error silenciado), getHistory(), activateProtocol(), isUnderCrisis().
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CrisisServiceImpl — Unit Tests")
class CrisisServiceImplTest {

    @Mock CriticalDayRepository  repository;
    @Mock FamilyRepository       familyRepository;
    @Mock AiProvider             aiProvider;
    @Mock ContextSynthesizer     contextSynthesizer;
    @Mock WhatsAppService        whatsAppService;

    @InjectMocks
    CrisisServiceImpl crisisService;

    private Family family;

    @BeforeEach
    void setUp() {
        family = Family.builder()
                .id(1L)
                .name("Familia López")
                .familyCode("IF-2026-TEST")
                .sentinelActive(false)
                .members(new ArrayList<>())
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  registerCrisis()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("registerCrisis()")
    class RegisterCrisis {

        @Test
        @DisplayName("Familia no encontrada → BusinessException NOT_FOUND")
        void shouldThrow_whenFamilyNotFound() {
            when(familyRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    crisisService.registerCrisis(99L, 1L, "CONFLICTO", "Discusión fuerte", "IRA"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        @DisplayName("Llamada al proveedor de IA falla → usa guía de contención por defecto")
        void shouldUseFallbackGuide_whenAiProviderThrows() {
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(contextSynthesizer.synthesize(any(Family.class), anyString()))
                    .thenReturn(null);
            when(aiProvider.generateResponse(anyString(), any()))
                    .thenThrow(new RuntimeException("AI unavailable"));

            CriticalDay saved = CriticalDay.builder().id(10L).familyId(1L).build();
            when(repository.save(any(CriticalDay.class))).thenReturn(saved);
            when(familyRepository.save(any(Family.class))).thenReturn(family);
            lenient().doNothing().when(whatsAppService).sendToFamily(any(), anyString());

            CriticalDay result = crisisService.registerCrisis(
                    1L, null, "VIOLENCIA_VERBAL", "Insultos entre hermanos", "TRISTEZA");

            ArgumentCaptor<CriticalDay> captor = ArgumentCaptor.forClass(CriticalDay.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getAiContainmentGuide())
                    .contains("Guía de Contención Inmediata (Modo Seguro)");
            assertThat(result.getId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("Éxito → persiste CriticalDay con todos los campos correctos")
        void shouldSaveCriticalDay_withCorrectFields() {
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(contextSynthesizer.synthesize(any(Family.class), eq("CRISIS")))
                    .thenReturn(null);
            when(aiProvider.generateResponse(anyString(), any()))
                    .thenReturn("### Guía de Claude para la crisis\n1. Respira. 2. Escucha. 3. Actúa.");
            when(repository.save(any(CriticalDay.class))).thenAnswer(i -> i.getArgument(0));
            when(familyRepository.save(any(Family.class))).thenReturn(family);
            lenient().doNothing().when(whatsAppService).sendToFamily(any(), anyString());

            crisisService.registerCrisis(1L, 5L, "CONFLICTO_ECONOMICO", "Deuda familiar", "MIEDO");

            ArgumentCaptor<CriticalDay> captor = ArgumentCaptor.forClass(CriticalDay.class);
            verify(repository).save(captor.capture());
            CriticalDay saved = captor.getValue();

            assertThat(saved.getFamilyId()).isEqualTo(1L);
            assertThat(saved.getMemberId()).isEqualTo(5L);
            assertThat(saved.getCategory()).isEqualTo("CONFLICTO_ECONOMICO");
            assertThat(saved.getDescription()).isEqualTo("Deuda familiar");
            assertThat(saved.getEmotion()).isEqualTo("MIEDO");
            assertThat(saved.getAiContainmentGuide()).contains("Guía de Claude");
            assertThat(saved.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Éxito → activa sentinelActive en la familia")
        void shouldActivateSentinel_onFamily() {
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(contextSynthesizer.synthesize(any(), anyString())).thenReturn(null);
            when(aiProvider.generateResponse(anyString(), any())).thenReturn("Guía");
            when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
            lenient().doNothing().when(whatsAppService).sendToFamily(any(), anyString());

            ArgumentCaptor<Family> familyCaptor = ArgumentCaptor.forClass(Family.class);
            when(familyRepository.save(familyCaptor.capture())).thenReturn(family);

            crisisService.registerCrisis(1L, null, "OTRO", "Descripción", null);

            assertThat(familyCaptor.getValue().getSentinelActive()).isTrue();
        }

        @Test
        @DisplayName("Error en WhatsApp → no propaga excepción, retorna CriticalDay guardado")
        void shouldNotPropagate_whenWhatsAppFails() {
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(contextSynthesizer.synthesize(any(), anyString())).thenReturn(null);
            when(aiProvider.generateResponse(anyString(), any())).thenReturn("Guía OK");
            CriticalDay saved = CriticalDay.builder().id(7L).familyId(1L).build();
            when(repository.save(any())).thenReturn(saved);
            when(familyRepository.save(any())).thenReturn(family);
            doThrow(new RuntimeException("WhatsApp down"))
                    .when(whatsAppService).sendToFamily(any(), anyString());

            // No debe lanzar aunque WhatsApp falle
            CriticalDay result = crisisService.registerCrisis(
                    1L, null, "CAT", "desc", "emocion");

            assertThat(result.getId()).isEqualTo(7L);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  getHistory()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getHistory()")
    class GetHistory {

        @Test
        @DisplayName("Delega en el repositorio y devuelve su resultado")
        void shouldReturnRepositoryResult() {
            List<CriticalDay> days = List.of(
                    CriticalDay.builder().id(1L).familyId(1L).build());
            when(repository.findByFamilyIdOrderByCreatedAtDesc(1L)).thenReturn(days);

            assertThat(crisisService.getHistory(1L)).isEqualTo(days);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  activateProtocol()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("activateProtocol()")
    class ActivateProtocol {

        @Test
        @DisplayName("Familia no encontrada → BusinessException NOT_FOUND")
        void shouldThrow_whenFamilyNotFound() {
            when(familyRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> crisisService.activateProtocol(99L, "Test"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        @DisplayName("Familia encontrada → sentinelActive se establece en true")
        void shouldActivateSentinel() {
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            ArgumentCaptor<Family> captor = ArgumentCaptor.forClass(Family.class);
            when(familyRepository.save(captor.capture())).thenReturn(family);

            crisisService.activateProtocol(1L, "Razón de emergencia");

            assertThat(captor.getValue().getSentinelActive()).isTrue();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  isUnderCrisis()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("isUnderCrisis()")
    class IsUnderCrisis {

        @Test
        @DisplayName("Familia no encontrada → false (sin excepción)")
        void shouldReturnFalse_whenFamilyNotFound() {
            when(familyRepository.findById(99L)).thenReturn(Optional.empty());
            assertThat(crisisService.isUnderCrisis(99L)).isFalse();
        }

        @Test
        @DisplayName("sentinelActive=true → true")
        void shouldReturnTrue_whenSentinelActive() {
            family.setSentinelActive(true);
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            assertThat(crisisService.isUnderCrisis(1L)).isTrue();
        }

        @Test
        @DisplayName("sentinelActive=false → false")
        void shouldReturnFalse_whenSentinelNotActive() {
            family.setSentinelActive(false);
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            assertThat(crisisService.isUnderCrisis(1L)).isFalse();
        }
    }
}
