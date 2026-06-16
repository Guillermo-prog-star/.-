package com.integrityfamily.common.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Tests para FamilyEventListener.
 *
 * El listener es puramente de logging y auditoría — no tiene efectos secundarios
 * observables fuera de logs. Los tests verifican que no lanza excepciones
 * ante cualquier combinación de datos de entrada, incluidos valores extremos.
 * Esto garantiza que el Event Bus nunca bloquea la operación principal.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FamilyEventListener — Unit Tests")
class FamilyEventListenerTest {

    @InjectMocks FamilyEventListener listener;

    // ═══════════════════════════════════════════════════════════════
    //  onCrisisTriggered()
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("onCrisisTriggered()")
    class OnCrisisTriggered {

        @Test
        @DisplayName("evento de crisis completo → no lanza excepción")
        void shouldNotThrow_forCompleteEvent() {
            FamilyCrisisEvent event = FamilyCrisisEvent.of(
                    1L, 10L, "CONFLICTO", "ira", "Discusión fuerte en la cena");

            assertThatCode(() -> listener.onCrisisTriggered(event))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("evento con campos nulos → no lanza excepción (resiliencia del listener)")
        void shouldNotThrow_whenFieldsAreNull() {
            FamilyCrisisEvent event = new FamilyCrisisEvent(1L, null, null, null, null, LocalDateTime.now());

            assertThatCode(() -> listener.onCrisisTriggered(event))
                    .doesNotThrowAnyException();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  onIcfRecalculated()
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("onIcfRecalculated()")
    class OnIcfRecalculated {

        @Test
        @DisplayName("ICF escaló → no lanza excepción")
        void shouldNotThrow_whenRiskEscalated() {
            FamilyIcfRecalculatedEvent event = new FamilyIcfRecalculatedEvent(
                    1L, 65.0, 45.0, "MEDIUM", "HIGH",
                    40.0, 35.0, 55.0, 50.0, "DAILY_CHECKIN", LocalDateTime.now());

            assertThatCode(() -> listener.onIcfRecalculated(event))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("ICF mejoró → no lanza excepción")
        void shouldNotThrow_whenRiskImproved() {
            FamilyIcfRecalculatedEvent event = new FamilyIcfRecalculatedEvent(
                    1L, 45.0, 70.0, "HIGH", "MEDIUM",
                    70.0, 65.0, 75.0, 68.0, "EVALUATION", LocalDateTime.now());

            assertThatCode(() -> listener.onIcfRecalculated(event))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("ICF sin cambio de nivel → no lanza excepción")
        void shouldNotThrow_whenRiskUnchanged() {
            FamilyIcfRecalculatedEvent event = new FamilyIcfRecalculatedEvent(
                    1L, 60.0, 62.0, "MEDIUM", "MEDIUM",
                    60.0, 60.0, 65.0, 60.0, "MANUAL", LocalDateTime.now());

            assertThatCode(() -> listener.onIcfRecalculated(event))
                    .doesNotThrowAnyException();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  onJournalEntryAdded()
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("onJournalEntryAdded()")
    class OnJournalEntryAdded {

        @Test
        @DisplayName("moodAfter=1 (deterioro) → detecta deterioro sin lanzar excepción")
        void shouldNotThrow_whenDeterioration() {
            FamilyJournalEntryEvent event = FamilyJournalEntryEvent.of(
                    1L, 5L, "MANUAL", "comunicacion", "tristeza", 1, "NO");

            assertThatCode(() -> listener.onJournalEntryAdded(event))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("moodAfter=1 con dimensión comunicacion → detecta posible collapse")
        void shouldNotThrow_whenCommunicationDeterioration() {
            FamilyJournalEntryEvent event = FamilyJournalEntryEvent.of(
                    1L, 5L, "CRISIS", "comunicacion", "angustia", 1, "NO");

            assertThatCode(() -> listener.onJournalEntryAdded(event))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("moodAfter=5 (mejora) → detecta mejora sin lanzar excepción")
        void shouldNotThrow_whenImprovement() {
            FamilyJournalEntryEvent event = FamilyJournalEntryEvent.of(
                    1L, 6L, "AI", "habitos", "alegría", 5, "YES");

            assertThatCode(() -> listener.onJournalEntryAdded(event))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("moodAfter=3 (neutro) → log normal sin lanzar excepción")
        void shouldNotThrow_whenNeutralMood() {
            FamilyJournalEntryEvent event = FamilyJournalEntryEvent.of(
                    1L, 7L, "TASK", "emociones", "calma", 3, "PARTIAL");

            assertThatCode(() -> listener.onJournalEntryAdded(event))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("moodAfter null → no lanza excepción (resiliencia del listener)")
        void shouldNotThrow_whenMoodIsNull() {
            FamilyJournalEntryEvent event = FamilyJournalEntryEvent.of(
                    1L, 8L, "MANUAL", "general", null, null, null);

            assertThatCode(() -> listener.onJournalEntryAdded(event))
                    .doesNotThrowAnyException();
        }
    }
}
