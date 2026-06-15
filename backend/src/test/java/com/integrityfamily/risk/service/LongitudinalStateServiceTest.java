package com.integrityfamily.risk.service;

import com.integrityfamily.common.event.FamilyCrisisEvent;
import com.integrityfamily.common.event.FamilyIcfRecalculatedEvent;
import com.integrityfamily.common.event.FamilyJournalEntryEvent;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyLongitudinalState;
import com.integrityfamily.domain.repository.FamilyLongitudinalStateRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LongitudinalStateService")
class LongitudinalStateServiceTest {

    @Mock FamilyLongitudinalStateRepository longitudinalRepo;
    @Mock FamilyRepository                  familyRepository;
    @Mock FamilyCausalEngine                causalEngine;

    @InjectMocks LongitudinalStateService service;

    private static final long FAM_ID = 1L;

    private FamilyLongitudinalState state(int crisisCount30d, int crisisTotal,
                                           int dets, int imps) {
        return FamilyLongitudinalState.builder()
                .icfCurrent(70.0)
                .crisisCount30d(crisisCount30d)
                .crisisCountTotal(crisisTotal)
                .consecutiveDeteriorations(dets)
                .consecutiveImprovements(imps)
                .inactivityDays(0)
                .communicationCollapseActive(false)
                .build();
    }

    private FamilyCrisisEvent crisis() {
        return new FamilyCrisisEvent(FAM_ID, 1L, "COMUNICACION", "ANGUSTIA",
                "Crisis de prueba", LocalDateTime.now());
    }

    private FamilyJournalEntryEvent journal(int mood) {
        return FamilyJournalEntryEvent.of(FAM_ID, 10L, "MANUAL",
                "comunicacion", "triste", mood, "DONE");
    }

    private FamilyIcfRecalculatedEvent icfEvent(double prevIcf, double newIcf,
                                                  double emociones, double comunicacion,
                                                  double habitos, double tiempos) {
        return new FamilyIcfRecalculatedEvent(FAM_ID, prevIcf, newIcf,
                "MODERADO", "BAJO", emociones, comunicacion, habitos, tiempos,
                "ASSESSMENT", LocalDateTime.now());
    }

    // ─── onCrisisTriggered ────────────────────────────────────────────────────

    @Nested
    @DisplayName("onCrisisTriggered")
    class OnCrisisTriggered {

        @Test
        @DisplayName("incrementa crisisCount30d")
        void incrementsCrisisCount30d() {
            FamilyLongitudinalState s = state(2, 5, 0, 0);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            when(longitudinalRepo.save(any())).thenReturn(s);

            service.onCrisisTriggered(crisis());

            assertThat(s.getCrisisCount30d()).isEqualTo(3);
        }

        @Test
        @DisplayName("incrementa crisisCountTotal")
        void incrementsCrisisCountTotal() {
            FamilyLongitudinalState s = state(0, 10, 0, 0);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            when(longitudinalRepo.save(any())).thenReturn(s);

            service.onCrisisTriggered(crisis());

            assertThat(s.getCrisisCountTotal()).isEqualTo(11);
        }

        @Test
        @DisplayName("crisis resetea consecutiveImprovements a 0")
        void resetsConsecutiveImprovements() {
            FamilyLongitudinalState s = state(0, 0, 0, 5);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            when(longitudinalRepo.save(any())).thenReturn(s);

            service.onCrisisTriggered(crisis());

            assertThat(s.getConsecutiveImprovements()).isZero();
        }

        @Test
        @DisplayName("excepción del motor causal → absorbida silenciosamente")
        void causalEngineThrows_swallowed() {
            FamilyLongitudinalState s = state(0, 0, 0, 0);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            when(longitudinalRepo.save(any())).thenReturn(s);
            when(causalEngine.infer(FAM_ID)).thenThrow(new RuntimeException("motor caído"));

            assertThatCode(() -> service.onCrisisTriggered(crisis())).doesNotThrowAnyException();
        }
    }

    // ─── onIcfRecalculated ────────────────────────────────────────────────────

    @Nested
    @DisplayName("onIcfRecalculated")
    class OnIcfRecalculated {

        @Test
        @DisplayName("nuevo ICF asignado como icfCurrent")
        void icfCurrentUpdated() {
            FamilyLongitudinalState s = state(0, 0, 0, 0);
            s.setIcfCurrent(65.0);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            when(longitudinalRepo.save(any())).thenReturn(s);

            service.onIcfRecalculated(icfEvent(65.0, 75.0, 0, 0, 0, 0));

            assertThat(s.getIcfCurrent()).isEqualTo(75.0);
        }

        @Test
        @DisplayName("ICF anterior distinto → rota a icf30dAgo")
        void icf30dAgoRotated_whenCurrentDiffers() {
            FamilyLongitudinalState s = state(0, 0, 0, 0);
            s.setIcfCurrent(60.0);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            when(longitudinalRepo.save(any())).thenReturn(s);

            service.onIcfRecalculated(icfEvent(60.0, 80.0, 0, 0, 0, 0));

            assertThat(s.getIcf30dAgo()).isEqualTo(60.0);
            assertThat(s.getIcfCurrent()).isEqualTo(80.0);
        }

        @Test
        @DisplayName("ICF idéntico → icf30dAgo no se rota")
        void icf30dAgoNotRotated_whenIcfSame() {
            FamilyLongitudinalState s = state(0, 0, 0, 0);
            s.setIcfCurrent(70.0);
            s.setIcf30dAgo(55.0);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            when(longitudinalRepo.save(any())).thenReturn(s);

            service.onIcfRecalculated(icfEvent(70.0, 70.0, 0, 0, 0, 0));

            assertThat(s.getIcf30dAgo()).isEqualTo(55.0);
        }

        @Test
        @DisplayName("emociones=0 → dimEmociones no se actualiza")
        void dimensionNotUpdated_whenZero() {
            FamilyLongitudinalState s = state(0, 0, 0, 0);
            s.setDimEmociones(50.0);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            when(longitudinalRepo.save(any())).thenReturn(s);

            service.onIcfRecalculated(icfEvent(70.0, 75.0, 0, 0, 0, 0));

            assertThat(s.getDimEmociones()).isEqualTo(50.0); // sin cambio
        }

        @Test
        @DisplayName("todas las dimensiones > 0 → actualizadas correctamente")
        void allDimensions_updated_whenPositive() {
            FamilyLongitudinalState s = state(0, 0, 0, 0);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            when(longitudinalRepo.save(any())).thenReturn(s);

            service.onIcfRecalculated(icfEvent(70.0, 75.0, 85.0, 72.0, 68.0, 90.0));

            assertThat(s.getDimEmociones()).isEqualTo(85.0);
            assertThat(s.getDimComunicacion()).isEqualTo(72.0);
            assertThat(s.getDimHabitos()).isEqualTo(68.0);
            assertThat(s.getDimTiempos()).isEqualTo(90.0);
        }
    }

    // ─── onJournalEntryAdded ──────────────────────────────────────────────────

    @Nested
    @DisplayName("onJournalEntryAdded")
    class OnJournalEntryAdded {

        @Test
        @DisplayName("mood=1 → deterioración: contador++ y mejoras reseteadas a 0")
        void mood1_incrementsDeteriorationAndResetsImprovements() {
            FamilyLongitudinalState s = state(0, 0, 1, 3);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            when(longitudinalRepo.save(any())).thenReturn(s);

            service.onJournalEntryAdded(journal(1));

            assertThat(s.getConsecutiveDeteriorations()).isEqualTo(2);
            assertThat(s.getConsecutiveImprovements()).isZero();
        }

        @Test
        @DisplayName("mood=5 → mejora: contador++ y deterioraciones reseteadas a 0")
        void mood5_incrementsImprovementsAndResetsDeteriorations() {
            FamilyLongitudinalState s = state(0, 0, 4, 1);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            when(longitudinalRepo.save(any())).thenReturn(s);

            service.onJournalEntryAdded(journal(5));

            assertThat(s.getConsecutiveImprovements()).isEqualTo(2);
            assertThat(s.getConsecutiveDeteriorations()).isZero();
        }

        @Test
        @DisplayName("mood=3 (neutral) con racha negativa → consecutiveDeteriorations decrementado")
        void mood3_neutral_decrementsDeteriorationStreak() {
            FamilyLongitudinalState s = state(0, 0, 3, 0);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            when(longitudinalRepo.save(any())).thenReturn(s);

            service.onJournalEntryAdded(journal(3));

            assertThat(s.getConsecutiveDeteriorations()).isEqualTo(2);
        }

        @Test
        @DisplayName("mood=3 (neutral) con racha=0 → consecutiveDeteriorations permanece en 0")
        void mood3_neutral_noStreak_staysZero() {
            FamilyLongitudinalState s = state(0, 0, 0, 0);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            when(longitudinalRepo.save(any())).thenReturn(s);

            service.onJournalEntryAdded(journal(3));

            assertThat(s.getConsecutiveDeteriorations()).isZero();
        }

        @Test
        @DisplayName("deterioración alcanza ≥3 → dispara re-inferencia causal")
        void deteriorationReachesThreshold3_triggersCausalInference() {
            FamilyLongitudinalState s = state(0, 0, 2, 0); // agregar 1 → 3
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            when(longitudinalRepo.save(any())).thenReturn(s);

            service.onJournalEntryAdded(journal(1));

            verify(causalEngine).infer(FAM_ID);
        }

        @Test
        @DisplayName("deterioración queda en 2 → causalEngine NO se invoca")
        void deteriorationBelow3_noCausalInference() {
            FamilyLongitudinalState s = state(0, 0, 1, 0); // agregar 1 → 2
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            when(longitudinalRepo.save(any())).thenReturn(s);

            service.onJournalEntryAdded(journal(1));

            verifyNoInteractions(causalEngine);
        }

        @Test
        @DisplayName("cualquier entrada de bitácora resetea inactivityDays a 0")
        void inactivityDaysResetToZero() {
            FamilyLongitudinalState s = state(0, 0, 0, 0);
            s.setInactivityDays(7);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            when(longitudinalRepo.save(any())).thenReturn(s);

            service.onJournalEntryAdded(journal(5));

            assertThat(s.getInactivityDays()).isZero();
        }
    }

    // ─── syncFromSnapshot ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("syncFromSnapshot")
    class SyncFromSnapshot {

        @Test
        @DisplayName("ICF existente → rota a icf30dAgo antes de actualizar")
        void icfRotated_whenCurrentExists() {
            FamilyLongitudinalState s = state(0, 0, 0, 0);
            s.setIcfCurrent(65.0);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            when(longitudinalRepo.save(any())).thenReturn(s);

            service.syncFromSnapshot(FAM_ID, 80.0, "BAJO", 85.0, 72.0, 68.0, 90.0);

            assertThat(s.getIcf30dAgo()).isEqualTo(65.0);
            assertThat(s.getIcfCurrent()).isEqualTo(80.0);
        }

        @Test
        @DisplayName("ICF null previo → icf30dAgo no se actualiza")
        void icfNotRotated_whenCurrentNull() {
            FamilyLongitudinalState s = state(0, 0, 0, 0);
            s.setIcfCurrent(null);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            when(longitudinalRepo.save(any())).thenReturn(s);

            service.syncFromSnapshot(FAM_ID, 75.0, "MODERADO", 70.0, 70.0, 70.0, 70.0);

            assertThat(s.getIcf30dAgo()).isNull();
            assertThat(s.getIcfCurrent()).isEqualTo(75.0);
        }

        @Test
        @DisplayName("todas las dimensiones y nivel de riesgo actualizados")
        void allDimensionsAndRiskLevelUpdated() {
            FamilyLongitudinalState s = state(0, 0, 0, 0);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            when(longitudinalRepo.save(any())).thenReturn(s);

            service.syncFromSnapshot(FAM_ID, 78.0, "BAJO", 82.0, 75.0, 80.0, 71.0);

            assertThat(s.getDimEmociones()).isEqualTo(82.0);
            assertThat(s.getDimComunicacion()).isEqualTo(75.0);
            assertThat(s.getDimHabitos()).isEqualTo(80.0);
            assertThat(s.getDimTiempos()).isEqualTo(71.0);
            assertThat(s.getCurrentRiskLevel()).isEqualTo("BAJO");
        }
    }

    // ─── getState / getOrCreate ───────────────────────────────────────────────

    @Nested
    @DisplayName("getState")
    class GetState {

        @Test
        @DisplayName("estado existe → retornado directamente sin guardar")
        void existingState_returnedWithoutSave() {
            FamilyLongitudinalState existing = state(1, 2, 0, 0);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(existing));

            FamilyLongitudinalState result = service.getState(FAM_ID);

            assertThat(result).isSameAs(existing);
            verify(longitudinalRepo, never()).save(any());
        }

        @Test
        @DisplayName("estado no existe, familia encontrada → crea y guarda nuevo estado")
        void noState_familyFound_createsAndSaves() {
            Family family = Family.builder().id(FAM_ID).name("Test").build();
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.empty());
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
            FamilyLongitudinalState newState = state(0, 0, 0, 0);
            when(longitudinalRepo.save(any())).thenReturn(newState);

            FamilyLongitudinalState result = service.getState(FAM_ID);

            assertThat(result).isNotNull();
            verify(longitudinalRepo).save(any(FamilyLongitudinalState.class));
        }

        @Test
        @DisplayName("estado no existe, familia no encontrada → devuelve estado por defecto sin guardar")
        void noState_familyNotFound_returnsDefaultUnsaved() {
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.empty());
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.empty());

            FamilyLongitudinalState result = service.getState(FAM_ID);

            assertThat(result).isNotNull();
            assertThat(result.getIcfCurrent()).isEqualTo(50.0);
            assertThat(result.getCurrentRiskLevel()).isEqualTo("MODERADO");
            verify(longitudinalRepo, never()).save(any());
        }
    }
}
