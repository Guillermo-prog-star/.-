package com.integrityfamily.family.service;

import com.integrityfamily.common.service.WhatsAppService;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyJourneySnapshot;
import com.integrityfamily.domain.repository.FamilyJourneySnapshotRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.family.dto.FamilyJourneyResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JourneyProgressTrackerService — Unit Tests")
class JourneyProgressTrackerServiceTest {

    @Mock FamilyJourneyService           journeyService;
    @Mock FamilyJourneySnapshotRepository snapshotRepository;
    @Mock FamilyRepository               familyRepository;
    @Mock WhatsAppService                whatsAppService;

    @InjectMocks JourneyProgressTrackerService service;

    private static final Long FAM_ID = 1L;
    private Family family;

    @BeforeEach
    void setUp() {
        family = new Family();
        family.setId(FAM_ID);
        family.setName("Familia López");
        family.setWhatsapp("+573001234567");

        when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
        when(snapshotRepository.existsByFamilyIdAndSnapshotDate(FAM_ID, LocalDate.now()))
                .thenReturn(false);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private FamilyJourneyResponse journeyAt(int level, int progress) {
        return new FamilyJourneyResponse(FAM_ID, "Familia López",
                level, progress, List.of(),
                "Continúa al nivel " + (level + 1), level + 1);
    }

    private FamilyJourneySnapshot snapshotAt(int level) {
        FamilyJourneySnapshot s = new FamilyJourneySnapshot();
        s.setFamilyId(FAM_ID);
        s.setJourneyLevel(level);
        s.setJourneyProgress(level * 7);
        s.setLevelUp(false);
        s.setSnapshotDate(LocalDate.now().minusDays(1));
        return s;
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("trackAndCelebrate() — snapshot ya existe hoy")
    class SnapshotAlreadyExists {

        @Test
        @DisplayName("devuelve false y no llama a journeyService si ya hay snapshot hoy")
        void skipsIfSnapshotExistsToday() {
            when(snapshotRepository.existsByFamilyIdAndSnapshotDate(FAM_ID, LocalDate.now()))
                    .thenReturn(true);

            boolean result = service.trackAndCelebrate(FAM_ID);

            assertThat(result).isFalse();
            verifyNoInteractions(journeyService);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("trackAndCelebrate() — primer snapshot (sin historial)")
    class FirstSnapshot {

        @Test
        @DisplayName("persiste snapshot sin level-up en el primer registro")
        void firstSnapshotIsNotLevelUp() {
            when(journeyService.evaluate(FAM_ID)).thenReturn(journeyAt(3, 29));
            when(snapshotRepository.findTopByFamilyIdOrderBySnapshotDateDesc(FAM_ID))
                    .thenReturn(Optional.empty());

            boolean result = service.trackAndCelebrate(FAM_ID);

            assertThat(result).isFalse();
            verifyNoInteractions(whatsAppService);

            ArgumentCaptor<FamilyJourneySnapshot> captor =
                    ArgumentCaptor.forClass(FamilyJourneySnapshot.class);
            verify(snapshotRepository).save(captor.capture());
            assertThat(captor.getValue().isLevelUp()).isFalse();
            assertThat(captor.getValue().getJourneyLevel()).isEqualTo(3);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("trackAndCelebrate() — sin level-up")
    class NoLevelUp {

        @Test
        @DisplayName("devuelve false y no envía WhatsApp cuando el nivel no cambió")
        void noLevelUpWhenSameLevel() {
            when(journeyService.evaluate(FAM_ID)).thenReturn(journeyAt(5, 43));
            when(snapshotRepository.findTopByFamilyIdOrderBySnapshotDateDesc(FAM_ID))
                    .thenReturn(Optional.of(snapshotAt(5)));

            boolean result = service.trackAndCelebrate(FAM_ID);

            assertThat(result).isFalse();
            verifyNoInteractions(whatsAppService);
        }

        @Test
        @DisplayName("persiste snapshot aunque no haya level-up")
        void persistsSnapshotEvenWithoutLevelUp() {
            when(journeyService.evaluate(FAM_ID)).thenReturn(journeyAt(4, 36));
            when(snapshotRepository.findTopByFamilyIdOrderBySnapshotDateDesc(FAM_ID))
                    .thenReturn(Optional.of(snapshotAt(4)));

            service.trackAndCelebrate(FAM_ID);

            verify(snapshotRepository).save(any(FamilyJourneySnapshot.class));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("trackAndCelebrate() — con level-up")
    class WithLevelUp {

        @Test
        @DisplayName("devuelve true cuando el nivel aumentó")
        void returnsTrueOnLevelUp() {
            when(journeyService.evaluate(FAM_ID)).thenReturn(journeyAt(6, 50));
            when(snapshotRepository.findTopByFamilyIdOrderBySnapshotDateDesc(FAM_ID))
                    .thenReturn(Optional.of(snapshotAt(5)));

            boolean result = service.trackAndCelebrate(FAM_ID);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("persiste snapshot con levelUp=true y previousLevel correcto")
        void persistsLevelUpSnapshot() {
            when(journeyService.evaluate(FAM_ID)).thenReturn(journeyAt(7, 57));
            when(snapshotRepository.findTopByFamilyIdOrderBySnapshotDateDesc(FAM_ID))
                    .thenReturn(Optional.of(snapshotAt(6)));

            service.trackAndCelebrate(FAM_ID);

            ArgumentCaptor<FamilyJourneySnapshot> captor =
                    ArgumentCaptor.forClass(FamilyJourneySnapshot.class);
            verify(snapshotRepository).save(captor.capture());
            FamilyJourneySnapshot saved = captor.getValue();
            assertThat(saved.isLevelUp()).isTrue();
            assertThat(saved.getPreviousLevel()).isEqualTo(6);
            assertThat(saved.getJourneyLevel()).isEqualTo(7);
        }

        @Test
        @DisplayName("envía WhatsApp de celebración cuando hay number configurado")
        void sendsCelebrationWhatsApp() {
            when(journeyService.evaluate(FAM_ID)).thenReturn(journeyAt(8, 64));
            when(snapshotRepository.findTopByFamilyIdOrderBySnapshotDateDesc(FAM_ID))
                    .thenReturn(Optional.of(snapshotAt(7)));

            service.trackAndCelebrate(FAM_ID);

            verify(whatsAppService).sendToFamily(eq(family), anyString());
        }

        @Test
        @DisplayName("el mensaje de celebración incluye el nombre de la familia")
        void celebrationMessageIncludesFamilyName() {
            when(journeyService.evaluate(FAM_ID)).thenReturn(journeyAt(3, 29));
            when(snapshotRepository.findTopByFamilyIdOrderBySnapshotDateDesc(FAM_ID))
                    .thenReturn(Optional.of(snapshotAt(2)));

            service.trackAndCelebrate(FAM_ID);

            ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
            verify(whatsAppService).sendToFamily(any(), msgCaptor.capture());
            assertThat(msgCaptor.getValue()).contains("Familia López");
        }

        @Test
        @DisplayName("el mensaje menciona el nuevo nivel por nombre")
        void celebrationMessageIncludesNewLevelName() {
            when(journeyService.evaluate(FAM_ID)).thenReturn(journeyAt(5, 43));
            when(snapshotRepository.findTopByFamilyIdOrderBySnapshotDateDesc(FAM_ID))
                    .thenReturn(Optional.of(snapshotAt(4)));

            service.trackAndCelebrate(FAM_ID);

            ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
            verify(whatsAppService).sendToFamily(any(), msgCaptor.capture());
            assertThat(msgCaptor.getValue()).contains("Diagnóstico Vivo");
        }

        @Test
        @DisplayName("marca celebrationSent=true en snapshot cuando el envío tiene éxito")
        void snapshotMarkedAsSent() {
            when(journeyService.evaluate(FAM_ID)).thenReturn(journeyAt(4, 36));
            when(snapshotRepository.findTopByFamilyIdOrderBySnapshotDateDesc(FAM_ID))
                    .thenReturn(Optional.of(snapshotAt(3)));

            service.trackAndCelebrate(FAM_ID);

            ArgumentCaptor<FamilyJourneySnapshot> captor =
                    ArgumentCaptor.forClass(FamilyJourneySnapshot.class);
            verify(snapshotRepository).save(captor.capture());
            assertThat(captor.getValue().isCelebrationSent()).isTrue();
        }

        @Test
        @DisplayName("marca celebrationSent=false cuando la familia no tiene WhatsApp")
        void snapshotNotSentWithoutWhatsApp() {
            family.setWhatsapp(null);
            when(journeyService.evaluate(FAM_ID)).thenReturn(journeyAt(4, 36));
            when(snapshotRepository.findTopByFamilyIdOrderBySnapshotDateDesc(FAM_ID))
                    .thenReturn(Optional.of(snapshotAt(3)));

            service.trackAndCelebrate(FAM_ID);

            ArgumentCaptor<FamilyJourneySnapshot> captor =
                    ArgumentCaptor.forClass(FamilyJourneySnapshot.class);
            verify(snapshotRepository).save(captor.capture());
            assertThat(captor.getValue().isCelebrationSent()).isFalse();
            verifyNoInteractions(whatsAppService);
        }

        @Test
        @DisplayName("no propaga excepción cuando WhatsApp falla durante el level-up")
        void doesNotPropagateWhatsAppError() {
            when(journeyService.evaluate(FAM_ID)).thenReturn(journeyAt(6, 50));
            when(snapshotRepository.findTopByFamilyIdOrderBySnapshotDateDesc(FAM_ID))
                    .thenReturn(Optional.of(snapshotAt(5)));
            doThrow(new RuntimeException("Fallo de red"))
                    .when(whatsAppService).sendToFamily(any(), any());

            assertThatCode(() -> service.trackAndCelebrate(FAM_ID)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("mensaje especial cuando la familia alcanza el nivel 13 (Legado)")
        void specialMessageAtLevel13() {
            when(journeyService.evaluate(FAM_ID)).thenReturn(journeyAt(13, 100));
            when(snapshotRepository.findTopByFamilyIdOrderBySnapshotDateDesc(FAM_ID))
                    .thenReturn(Optional.of(snapshotAt(12)));

            service.trackAndCelebrate(FAM_ID);

            ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
            verify(whatsAppService).sendToFamily(any(), msgCaptor.capture());
            assertThat(msgCaptor.getValue()).containsIgnoringCase("viaje familiar");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("retryCelebrations()")
    class RetryCelebrations {

        @Test
        @DisplayName("envía celebraciones pendientes y actualiza el snapshot")
        void retriesPendingCelebrations() {
            FamilyJourneySnapshot pending = new FamilyJourneySnapshot();
            pending.setFamilyId(FAM_ID);
            pending.setJourneyLevel(5);
            pending.setPreviousLevel(4);
            pending.setLevelUp(true);
            pending.setCelebrationSent(false);

            when(snapshotRepository.findByLevelUpTrueAndCelebrationSentFalse())
                    .thenReturn(List.of(pending));

            int count = service.retryCelebrations();

            assertThat(count).isEqualTo(1);
            verify(whatsAppService).sendToFamily(eq(family), anyString());
            assertThat(pending.isCelebrationSent()).isTrue();
        }

        @Test
        @DisplayName("devuelve 0 cuando no hay celebraciones pendientes")
        void returnsZeroWhenNoPending() {
            when(snapshotRepository.findByLevelUpTrueAndCelebrationSentFalse())
                    .thenReturn(List.of());

            int count = service.retryCelebrations();

            assertThat(count).isEqualTo(0);
            verifyNoInteractions(whatsAppService);
        }
    }
}
