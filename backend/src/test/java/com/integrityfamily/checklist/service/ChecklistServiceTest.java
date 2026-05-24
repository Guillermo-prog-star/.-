package com.integrityfamily.checklist.service;

import com.integrityfamily.domain.ChecklistItem;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.ChecklistRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Pruebas unitarias de ChecklistService.
 *
 * Cubre: extracción de líneas de texto con regex, detectDimension,
 * markAsCompleted, createChecklistItem, y getFamilyChecklist.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChecklistService — Unit Tests")
class ChecklistServiceTest {

    @Mock ChecklistRepository checklistRepository;
    @Mock FamilyRepository    familyRepository;

    @InjectMocks
    ChecklistService checklistService;

    private Family family;

    @BeforeEach
    void setUp() {
        family = Family.builder()
                .id(1L)
                .name("Familia López")
                .familyCode("IF-2026-TEST")
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  getFamilyChecklist()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getFamilyChecklist()")
    class GetFamilyChecklist {

        @Test
        @DisplayName("Delega en el repositorio y devuelve su resultado")
        void shouldReturnRepositoryResult() {
            List<ChecklistItem> items = List.of(
                    ChecklistItem.builder().id(1L).description("Tarea 1").build());
            when(checklistRepository.findByFamilyIdOrderByCreatedAtDesc(1L)).thenReturn(items);

            List<ChecklistItem> result = checklistService.getFamilyChecklist(1L);

            assertThat(result).isEqualTo(items);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  extractAndAdd()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("extractAndAdd() — extracción de actividades desde texto")
    class ExtractAndAdd {

        @BeforeEach
        void stubFamily() {
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            // lenient: save() no se llama en todos los tests (ej. texto null → 0 items)
            lenient().when(checklistRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        }

        @Test
        @DisplayName("Texto con guiones (- item) → crea una entrada por línea válida")
        void shouldExtractBulletLines() {
            String text = "- Dedica tiempo de calidad a cada hijo\n" +
                          "- Practica la escucha activa en familia\n" +
                          "- Establece rituales familiares semanales";

            int added = checklistService.extractAndAdd(text, "PLAN", 1L);

            assertThat(added).isEqualTo(3);
            verify(checklistRepository, times(3)).save(any(ChecklistItem.class));
        }

        @Test
        @DisplayName("Texto con asteriscos (* item) → extrae correctamente")
        void shouldExtractAsteriskLines() {
            String text = "* Realiza cenas en familia tres veces por semana\n" +
                          "* Comparte actividades recreativas juntos";

            int added = checklistService.extractAndAdd(text, "PLAN", 1L);

            assertThat(added).isEqualTo(2);
        }

        @Test
        @DisplayName("Texto con números (1. item) → extrae correctamente")
        void shouldExtractNumberedLines() {
            String text = "1. Agenda una reunión familiar mensual\n" +
                          "2. Escucha las necesidades de cada miembro\n" +
                          "3. Celebra los logros de la familia";

            int added = checklistService.extractAndAdd(text, "EVAL", 1L);

            assertThat(added).isEqualTo(3);
        }

        @Test
        @DisplayName("Línea demasiado corta (< 10 chars) → no se agrega")
        void shouldSkipShortLines() {
            String text = "- Corto\n" +               // 6 chars → skip
                          "- Realiza la actividad familiar completa"; // long → add

            int added = checklistService.extractAndAdd(text, "PLAN", 1L);

            assertThat(added).isEqualTo(1);
        }

        @Test
        @DisplayName("Línea con contenido que empieza con # → no se agrega")
        void shouldSkipHashPrefixContent() {
            String text = "1. #header ignorado de todas maneras\n" +
                          "- Actividad familiar concreta y prioritaria"; // add

            int added = checklistService.extractAndAdd(text, "PLAN", 1L);

            assertThat(added).isEqualTo(1);
        }

        @Test
        @DisplayName("Texto sin líneas tipo bullet → retorna 0")
        void shouldReturnZero_whenNoBullets() {
            String text = "Este texto no tiene formato de lista.\n" +
                          "Solo párrafos regulares sin actividades.";

            int added = checklistService.extractAndAdd(text, "PLAN", 1L);

            assertThat(added).isEqualTo(0);
            verify(checklistRepository, never()).save(any());
        }

        @Test
        @DisplayName("Texto null → retorna 0 sin excepción")
        void shouldReturnZero_whenTextIsNull() {
            int added = checklistService.extractAndAdd(null, "PLAN", 1L);

            assertThat(added).isEqualTo(0);
            verify(checklistRepository, never()).save(any());
        }

        @Test
        @DisplayName("Los items creados tienen el source y familyId correctos")
        void shouldSetSourceAndFamily_onCreatedItems() {
            String text = "- Realizar actividad de gratitud familiar";
            ArgumentCaptor<ChecklistItem> captor = ArgumentCaptor.forClass(ChecklistItem.class);

            checklistService.extractAndAdd(text, "CRISIS", 1L);

            verify(checklistRepository).save(captor.capture());
            ChecklistItem saved = captor.getValue();
            assertThat(saved.getSource()).isEqualTo("CRISIS");
            assertThat(saved.getFamily().getId()).isEqualTo(1L);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  detectDimension() — probado vía extractAndAdd / createChecklistItem
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("detectDimension() — clasificación por palabras clave")
    class DetectDimension {

        @BeforeEach
        void stubFamily() {
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(checklistRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        }

        @Test
        @DisplayName("\"reconoci\" → dimensión Reconocimiento")
        void shouldDetectReconocimiento() {
            ArgumentCaptor<ChecklistItem> captor = ArgumentCaptor.forClass(ChecklistItem.class);
            checklistService.extractAndAdd("- Practica el reconocimiento de los demás", "PLAN", 1L);
            verify(checklistRepository).save(captor.capture());
            assertThat(captor.getValue().getDimension()).isEqualTo("Reconocimiento");
        }

        @Test
        @DisplayName("\"amor\" → dimensión Amor")
        void shouldDetectAmor() {
            ArgumentCaptor<ChecklistItem> captor = ArgumentCaptor.forClass(ChecklistItem.class);
            checklistService.extractAndAdd("- Expresa amor y afecto a tu familia", "PLAN", 1L);
            verify(checklistRepository).save(captor.capture());
            assertThat(captor.getValue().getDimension()).isEqualTo("Amor");
        }

        @Test
        @DisplayName("\"servicio\" → dimensión Entrega")
        void shouldDetectEntrega() {
            ArgumentCaptor<ChecklistItem> captor = ArgumentCaptor.forClass(ChecklistItem.class);
            checklistService.extractAndAdd("- Ofrece servicio voluntario a la comunidad", "PLAN", 1L);
            verify(checklistRepository).save(captor.capture());
            assertThat(captor.getValue().getDimension()).isEqualTo("Entrega");
        }

        @Test
        @DisplayName("Sin palabras clave → dimensión General")
        void shouldFallbackToGeneral() {
            ArgumentCaptor<ChecklistItem> captor = ArgumentCaptor.forClass(ChecklistItem.class);
            checklistService.extractAndAdd("- Cumple con las tareas del hogar regularmente", "PLAN", 1L);
            verify(checklistRepository).save(captor.capture());
            assertThat(captor.getValue().getDimension()).isEqualTo("General");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  createChecklistItem()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createChecklistItem()")
    class CreateChecklistItem {

        @Test
        @DisplayName("Familia existe → guarda item con los campos correctos")
        void shouldSaveItem_withCorrectFields() {
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            ChecklistItem saved = ChecklistItem.builder()
                    .id(10L).family(family)
                    .description("Actividad de gratitud").dimension("Amor").source("SENTINEL")
                    .build();
            when(checklistRepository.save(any())).thenReturn(saved);

            ChecklistItem result = checklistService.createChecklistItem(1L, "Actividad de gratitud", "Amor", "SENTINEL");

            assertThat(result.getId()).isEqualTo(10L);
            ArgumentCaptor<ChecklistItem> captor = ArgumentCaptor.forClass(ChecklistItem.class);
            verify(checklistRepository).save(captor.capture());
            assertThat(captor.getValue().getDescription()).isEqualTo("Actividad de gratitud");
            assertThat(captor.getValue().getDimension()).isEqualTo("Amor");
            assertThat(captor.getValue().getSource()).isEqualTo("SENTINEL");
            assertThat(captor.getValue().isCompleted()).isFalse();
        }

        @Test
        @DisplayName("dimension null → default \"General\"")
        void shouldDefaultToGeneral_whenDimensionIsNull() {
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(checklistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            checklistService.createChecklistItem(1L, "Tarea sin dimensión específica", null, "PLAN");

            ArgumentCaptor<ChecklistItem> captor = ArgumentCaptor.forClass(ChecklistItem.class);
            verify(checklistRepository).save(captor.capture());
            assertThat(captor.getValue().getDimension()).isEqualTo("General");
        }

        @Test
        @DisplayName("source null → default \"SENTINEL\"")
        void shouldDefaultToSentinel_whenSourceIsNull() {
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(checklistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            checklistService.createChecklistItem(1L, "Tarea con source por defecto", "General", null);

            ArgumentCaptor<ChecklistItem> captor = ArgumentCaptor.forClass(ChecklistItem.class);
            verify(checklistRepository).save(captor.capture());
            assertThat(captor.getValue().getSource()).isEqualTo("SENTINEL");
        }

        @Test
        @DisplayName("Familia no encontrada → IllegalArgumentException descriptiva")
        void shouldThrowIllegalArgument_whenFamilyNotFound() {
            when(familyRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    checklistService.createChecklistItem(99L, "Tarea X", "General", "PLAN"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("99");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  markAsCompleted()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("markAsCompleted()")
    class MarkAsCompleted {

        @Test
        @DisplayName("Marca el item como completado con el nombre del usuario")
        void shouldMarkItemAsCompleted() {
            ChecklistItem item = ChecklistItem.builder()
                    .id(1L).family(family).description("Tarea A")
                    .source("PLAN").completed(false).build();
            when(checklistRepository.findById(1L)).thenReturn(Optional.of(item));
            when(checklistRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(checklistRepository.countByFamilyIdAndSourceAndCompletedFalse(1L, "PLAN"))
                    .thenReturn(2L);

            checklistService.markAsCompleted(1L, "william@if.com");

            assertThat(item.isCompleted()).isTrue();
            assertThat(item.getCompletedBy()).isEqualTo("william@if.com");
            assertThat(item.getCompletedAt()).isNotNull()
                    .isBeforeOrEqualTo(LocalDateTime.now());
        }

        @Test
        @DisplayName("Última tarea pendiente → no lanza excepción (milestone log interno)")
        void shouldNotThrow_whenLastPendingItemCompleted() {
            ChecklistItem item = ChecklistItem.builder()
                    .id(2L).family(family).description("Última tarea de la fuente")
                    .source("EVAL").completed(false).build();
            when(checklistRepository.findById(2L)).thenReturn(Optional.of(item));
            when(checklistRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(checklistRepository.countByFamilyIdAndSourceAndCompletedFalse(1L, "EVAL"))
                    .thenReturn(0L); // ← ya no hay más pendientes

            // No debe lanzar — el milestone-ready es solo un log
            checklistService.markAsCompleted(2L, "william@if.com");

            assertThat(item.isCompleted()).isTrue();
        }
    }
}
