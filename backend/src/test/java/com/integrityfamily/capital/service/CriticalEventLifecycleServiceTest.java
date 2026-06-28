package com.integrityfamily.capital.service;

import com.integrityfamily.common.event.EventPublisher;
import com.integrityfamily.common.event.FamilyCrisisEvent;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyCapitalSnapshot;
import com.integrityfamily.domain.FamilyCriticalEvent;
import com.integrityfamily.domain.repository.FamilyCapitalSnapshotRepository;
import com.integrityfamily.domain.repository.FamilyCriticalEventRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CriticalEventLifecycleService — Unit Tests")
class CriticalEventLifecycleServiceTest {

    @Mock FamilyCriticalEventRepository   criticalEventRepo;
    @Mock FamilyCapitalSnapshotRepository capitalSnapshotRepo;
    @Mock FamilyRepository                familyRepo;
    @Mock EventPublisher                  eventPublisher;
    @Mock IcafScoringEngine               icafScoringEngine;

    @InjectMocks CriticalEventLifecycleService service;

    private static final Long FAM_ID   = 1L;
    private static final Long EVENT_ID = 10L;

    private Family family;

    @BeforeEach
    void setUp() {
        family = new Family();
        family.setId(FAM_ID);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private FamilyCrisisEvent crisisEvent(Long criticalDayId, String category, String emotion) {
        return new FamilyCrisisEvent(FAM_ID, criticalDayId, category, emotion, "desc", null);
    }

    private FamilyCriticalEvent criticalEvent(Long id, String status) {
        FamilyCriticalEvent e = FamilyCriticalEvent.builder()
                .family(family)
                .category("VIOLENCIA")
                .status(status)
                .severity("HIGH")
                .build();
        e.setId(id);
        return e;
    }

    private void stubNoSnapshot() {
        when(capitalSnapshotRepo.findTopByFamilyIdOrderByCreatedAtDesc(FAM_ID))
                .thenReturn(Optional.empty());
    }

    private void stubSnapshotIcaf(double icaf) {
        FamilyCapitalSnapshot snap = FamilyCapitalSnapshot.builder().icaf(icaf).build();
        when(capitalSnapshotRepo.findTopByFamilyIdOrderByCreatedAtDesc(FAM_ID))
                .thenReturn(Optional.of(snap));
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("onCrisisTriggered()")
    class OnCrisisTriggered {

        @Test
        @DisplayName("crea FamilyCriticalEvent con status DETECTED")
        void createsEventDetected() {
            when(criticalEventRepo.findByFamilyIdAndCriticalDayId(FAM_ID, 42L))
                    .thenReturn(Optional.empty());
            when(familyRepo.findById(FAM_ID)).thenReturn(Optional.of(family));
            stubNoSnapshot();
            when(criticalEventRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.onCrisisTriggered(crisisEvent(42L, "VIOLENCIA", "miedo"));

            ArgumentCaptor<FamilyCriticalEvent> captor =
                    ArgumentCaptor.forClass(FamilyCriticalEvent.class);
            verify(criticalEventRepo).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo("DETECTED");
        }

        @Test
        @DisplayName("idempotente: si ya existe criticalDayId → no guarda duplicado")
        void idempotentWithExistingCriticalDay() {
            when(criticalEventRepo.findByFamilyIdAndCriticalDayId(FAM_ID, 42L))
                    .thenReturn(Optional.of(criticalEvent(EVENT_ID, "DETECTED")));

            service.onCrisisTriggered(crisisEvent(42L, "VIOLENCIA", "miedo"));

            verify(criticalEventRepo, never()).save(any());
        }

        @Test
        @DisplayName("criticalDayId null → omite verificación de duplicado y crea igualmente")
        void nullCriticalDayIdSkipsCheck() {
            when(familyRepo.findById(FAM_ID)).thenReturn(Optional.of(family));
            stubNoSnapshot();
            when(criticalEventRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.onCrisisTriggered(crisisEvent(null, "OTRO", null));

            verify(criticalEventRepo, never()).findByFamilyIdAndCriticalDayId(any(), any());
            verify(criticalEventRepo).save(any());
        }

        @Test
        @DisplayName("familia no encontrada → no crea evento")
        void familyNotFound() {
            when(criticalEventRepo.findByFamilyIdAndCriticalDayId(FAM_ID, 5L))
                    .thenReturn(Optional.empty());
            when(familyRepo.findById(FAM_ID)).thenReturn(Optional.empty());

            service.onCrisisTriggered(crisisEvent(5L, "ALCOHOL", "angustia"));

            verify(criticalEventRepo, never()).save(any());
        }

        @Test
        @DisplayName("captura ICaF del snapshot actual en icafAtDetection")
        void capturesCurrentIcaf() {
            when(criticalEventRepo.findByFamilyIdAndCriticalDayId(FAM_ID, 7L))
                    .thenReturn(Optional.empty());
            when(familyRepo.findById(FAM_ID)).thenReturn(Optional.of(family));
            stubSnapshotIcaf(65.5);
            when(criticalEventRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.onCrisisTriggered(crisisEvent(7L, "COMUNICACION", null));

            ArgumentCaptor<FamilyCriticalEvent> captor =
                    ArgumentCaptor.forClass(FamilyCriticalEvent.class);
            verify(criticalEventRepo).save(captor.capture());
            assertThat(captor.getValue().getIcafAtDetection()).isCloseTo(65.5, within(0.01));
        }

        @Test
        @DisplayName("sin snapshot → icafAtDetection null")
        void nullIcafWhenNoSnapshot() {
            when(criticalEventRepo.findByFamilyIdAndCriticalDayId(FAM_ID, 8L))
                    .thenReturn(Optional.empty());
            when(familyRepo.findById(FAM_ID)).thenReturn(Optional.of(family));
            stubNoSnapshot();
            when(criticalEventRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.onCrisisTriggered(crisisEvent(8L, "ALCOHOL", null));

            ArgumentCaptor<FamilyCriticalEvent> captor =
                    ArgumentCaptor.forClass(FamilyCriticalEvent.class);
            verify(criticalEventRepo).save(captor.capture());
            assertThat(captor.getValue().getIcafAtDetection()).isNull();
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("normalizeCategoryFromCrisis()")
    class NormalizeCategory {

        @ParameterizedTest(name = "'{0}' → '{1}'")
        @CsvSource({
            "ALCOHOL,          ALCOHOL",
            "DROGAS,           ALCOHOL",
            "SUSTANCIAS,       ALCOHOL",
            "VIOLENCIA,        VIOLENCIA",
            "AGRESION,         VIOLENCIA",
            "COMUNICACION,     COMUNICACION_ROTA",
            "CONFLICTO,        COMUNICACION_ROTA",
            "ADOLESCENTE,      ADOLESCENTE_AISLADO",
            "HIJO,             ADOLESCENTE_AISLADO",
            "ECONOMICA,        ECONOMICA"
        })
        @DisplayName("normaliza categoría al crear el evento")
        void normalizes(String input, String expected) {
            when(criticalEventRepo.findByFamilyIdAndCriticalDayId(FAM_ID, 99L))
                    .thenReturn(Optional.empty());
            when(familyRepo.findById(FAM_ID)).thenReturn(Optional.of(family));
            stubNoSnapshot();
            when(criticalEventRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.onCrisisTriggered(crisisEvent(99L, input, null));

            ArgumentCaptor<FamilyCriticalEvent> captor =
                    ArgumentCaptor.forClass(FamilyCriticalEvent.class);
            verify(criticalEventRepo).save(captor.capture());
            assertThat(captor.getValue().getCategory()).isEqualTo(expected);
        }

        @Test
        @DisplayName("categoría null → OTRO")
        void nullCategoryDefaultsToOtro() {
            when(familyRepo.findById(FAM_ID)).thenReturn(Optional.of(family));
            stubNoSnapshot();
            when(criticalEventRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.onCrisisTriggered(crisisEvent(null, null, null));

            ArgumentCaptor<FamilyCriticalEvent> captor =
                    ArgumentCaptor.forClass(FamilyCriticalEvent.class);
            verify(criticalEventRepo).save(captor.capture());
            assertThat(captor.getValue().getCategory()).isEqualTo("OTRO");
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("inferSeverity()")
    class InferSeverity {

        @ParameterizedTest(name = "emotion='{0}' → severity='{1}'")
        @CsvSource({
            "terror,    CRITICAL",
            "pánico,    CRITICAL",
            "desesper,  CRITICAL",
            "miedo,     HIGH",
            "angustia,  HIGH",
            "agres,     HIGH",
            "tristeza,  MODERATE",
            "enojo,     MODERATE",
            "frustrac,  MODERATE",
            "cansancio, LOW"
        })
        @DisplayName("infiere severidad desde la emoción")
        void infers(String emotion, String expected) {
            when(familyRepo.findById(FAM_ID)).thenReturn(Optional.of(family));
            stubNoSnapshot();
            when(criticalEventRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.onCrisisTriggered(crisisEvent(null, "OTRO", emotion));

            ArgumentCaptor<FamilyCriticalEvent> captor =
                    ArgumentCaptor.forClass(FamilyCriticalEvent.class);
            verify(criticalEventRepo).save(captor.capture());
            assertThat(captor.getValue().getSeverity()).isEqualTo(expected);
        }

        @Test
        @DisplayName("emotion null → MODERATE")
        void nullEmotionIsModerate() {
            when(familyRepo.findById(FAM_ID)).thenReturn(Optional.of(family));
            stubNoSnapshot();
            when(criticalEventRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.onCrisisTriggered(crisisEvent(null, "OTRO", null));

            ArgumentCaptor<FamilyCriticalEvent> captor =
                    ArgumentCaptor.forClass(FamilyCriticalEvent.class);
            verify(criticalEventRepo).save(captor.capture());
            assertThat(captor.getValue().getSeverity()).isEqualTo("MODERATE");
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("startIntervention()")
    class StartIntervention {

        @Test
        @DisplayName("cambia status a IN_PROGRESS y guarda")
        void setsInProgress() {
            FamilyCriticalEvent event = criticalEvent(EVENT_ID, "DETECTED");
            when(criticalEventRepo.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(criticalEventRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            FamilyCriticalEvent result = service.startIntervention(EVENT_ID);

            assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
            assertThat(result.getInterventionStartAt()).isNotNull();
            verify(criticalEventRepo).save(event);
        }

        @Test
        @DisplayName("evento no encontrado → IllegalArgumentException")
        void eventNotFound() {
            when(criticalEventRepo.findById(EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.startIntervention(EVENT_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(EVENT_ID.toString());
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("resolve()")
    class Resolve {

        @Test
        @DisplayName("cambia status a RESOLVED y dispara recálculo ICaF")
        void resolvesAndRecalculates() {
            FamilyCriticalEvent event = criticalEvent(EVENT_ID, "IN_PROGRESS");
            when(criticalEventRepo.findById(EVENT_ID)).thenReturn(Optional.of(event));
            stubSnapshotIcaf(70.0);
            when(criticalEventRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.resolve(EVENT_ID, "Resuelto con mediación");

            assertThat(event.getStatus()).isEqualTo("RESOLVED");
            assertThat(event.getResolutionSummary()).isEqualTo("Resuelto con mediación");
            assertThat(event.getIcafAtResolution()).isCloseTo(70.0, within(0.01));
            verify(icafScoringEngine).compute(FAM_ID, "CRITICAL_EVENT");
        }

        @Test
        @DisplayName("sin snapshot → icafAtResolution null, igual dispara recálculo")
        void resolveNoSnapshot() {
            FamilyCriticalEvent event = criticalEvent(EVENT_ID, "IN_PROGRESS");
            when(criticalEventRepo.findById(EVENT_ID)).thenReturn(Optional.of(event));
            stubNoSnapshot();
            when(criticalEventRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.resolve(EVENT_ID, null);

            assertThat(event.getIcafAtResolution()).isNull();
            verify(icafScoringEngine).compute(FAM_ID, "CRITICAL_EVENT");
        }

        @Test
        @DisplayName("error en recálculo ICaF no propaga excepción")
        void recalculationErrorIsSilent() {
            FamilyCriticalEvent event = criticalEvent(EVENT_ID, "IN_PROGRESS");
            when(criticalEventRepo.findById(EVENT_ID)).thenReturn(Optional.of(event));
            stubNoSnapshot();
            when(criticalEventRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doThrow(new RuntimeException("ICaF error")).when(icafScoringEngine).compute(any(), any());

            // No debe lanzar excepción
            service.resolve(EVENT_ID, "ok");
        }

        @Test
        @DisplayName("evento no encontrado → IllegalArgumentException")
        void eventNotFound() {
            when(criticalEventRepo.findById(EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.resolve(EVENT_ID, "x"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("registerRelapse()")
    class RegisterRelapse {

        @Test
        @DisplayName("incrementa relapseCount, status RELAPSED, dispara recálculo")
        void registersRelapse() {
            FamilyCriticalEvent event = criticalEvent(EVENT_ID, "RESOLVED");
            event.setRelapseCount(1);
            when(criticalEventRepo.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(criticalEventRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            FamilyCriticalEvent result = service.registerRelapse(EVENT_ID, "volvió a pelear");

            assertThat(result.getStatus()).isEqualTo("RELAPSED");
            assertThat(result.getRelapseCount()).isEqualTo(2);
            verify(icafScoringEngine).compute(FAM_ID, "CRITICAL_EVENT");
        }

        @Test
        @DisplayName("primera recaída: relapseCount null → se inicia en 1")
        void firstRelapseFromNull() {
            FamilyCriticalEvent event = criticalEvent(EVENT_ID, "RESOLVED");
            event.setRelapseCount(null);
            when(criticalEventRepo.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(criticalEventRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.registerRelapse(EVENT_ID, null);

            assertThat(event.getRelapseCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("notas actualizadas cuando se pasan")
        void notesUpdated() {
            FamilyCriticalEvent event = criticalEvent(EVENT_ID, "RESOLVED");
            when(criticalEventRepo.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(criticalEventRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.registerRelapse(EVENT_ID, "nueva nota");

            assertThat(event.getNotes()).isEqualTo("nueva nota");
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("close()")
    class Close {

        @Test
        @DisplayName("cambia status a CLOSED y guarda closedAt")
        void closes() {
            FamilyCriticalEvent event = criticalEvent(EVENT_ID, "RESOLVED");
            when(criticalEventRepo.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(criticalEventRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            FamilyCriticalEvent result = service.close(EVENT_ID);

            assertThat(result.getStatus()).isEqualTo("CLOSED");
            assertThat(result.getClosedAt()).isNotNull();
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("getResilienceMetrics()")
    class GetResilienceMetrics {

        @Test
        @DisplayName("sin eventos → totalEvents=0, resolutionRate=100%")
        void noEvents() {
            when(criticalEventRepo.countByFamilyIdAndStatus(eq(FAM_ID), anyString())).thenReturn(0L);
            when(criticalEventRepo.countActiveByFamilyId(FAM_ID)).thenReturn(0L);
            when(criticalEventRepo.avgDaysToResolutionByFamilyId(FAM_ID)).thenReturn(0.0);
            when(criticalEventRepo.totalRelapsesByFamilyId(FAM_ID)).thenReturn(0L);

            CriticalEventLifecycleService.ResilienceMetrics m = service.getResilienceMetrics(FAM_ID);

            assertThat(m.totalEvents()).isEqualTo(0);
            assertThat(m.resolutionRate()).isCloseTo(100.0, within(0.01));
        }

        @Test
        @DisplayName("3 total, 2 resolved → resolutionRate ≈ 66.7%")
        void partialResolution() {
            // totalEvents = DETECTED+IN_PROGRESS+RESOLVED+CLOSED+RELAPSED
            when(criticalEventRepo.countByFamilyIdAndStatus(FAM_ID, "DETECTED")).thenReturn(1L);
            when(criticalEventRepo.countByFamilyIdAndStatus(FAM_ID, "IN_PROGRESS")).thenReturn(0L);
            when(criticalEventRepo.countByFamilyIdAndStatus(FAM_ID, "RESOLVED")).thenReturn(1L);
            when(criticalEventRepo.countByFamilyIdAndStatus(FAM_ID, "CLOSED")).thenReturn(1L);
            when(criticalEventRepo.countByFamilyIdAndStatus(FAM_ID, "RELAPSED")).thenReturn(0L);
            when(criticalEventRepo.countActiveByFamilyId(FAM_ID)).thenReturn(1L);
            when(criticalEventRepo.avgDaysToResolutionByFamilyId(FAM_ID)).thenReturn(10.0);
            when(criticalEventRepo.totalRelapsesByFamilyId(FAM_ID)).thenReturn(2L);

            CriticalEventLifecycleService.ResilienceMetrics m = service.getResilienceMetrics(FAM_ID);

            // RESOLVED(1)+CLOSED(1)=2 resueltos / 3 total → 66.7%
            assertThat(m.totalEvents()).isEqualTo(3);
            assertThat(m.resolvedEvents()).isEqualTo(2);
            assertThat(m.resolutionRate()).isCloseTo(66.7, within(0.1));
            assertThat(m.activeEvents()).isEqualTo(1);
            assertThat(m.avgDaysToResolution()).isCloseTo(10.0, within(0.01));
            assertThat(m.totalRelapses()).isEqualTo(2);
        }

        @Test
        @DisplayName("todos resueltos → resolutionRate = 100%")
        void allResolved() {
            when(criticalEventRepo.countByFamilyIdAndStatus(FAM_ID, "DETECTED")).thenReturn(0L);
            when(criticalEventRepo.countByFamilyIdAndStatus(FAM_ID, "IN_PROGRESS")).thenReturn(0L);
            when(criticalEventRepo.countByFamilyIdAndStatus(FAM_ID, "RESOLVED")).thenReturn(4L);
            when(criticalEventRepo.countByFamilyIdAndStatus(FAM_ID, "CLOSED")).thenReturn(0L);
            when(criticalEventRepo.countByFamilyIdAndStatus(FAM_ID, "RELAPSED")).thenReturn(0L);
            when(criticalEventRepo.countActiveByFamilyId(FAM_ID)).thenReturn(0L);
            when(criticalEventRepo.avgDaysToResolutionByFamilyId(FAM_ID)).thenReturn(7.5);
            when(criticalEventRepo.totalRelapsesByFamilyId(FAM_ID)).thenReturn(0L);

            CriticalEventLifecycleService.ResilienceMetrics m = service.getResilienceMetrics(FAM_ID);

            assertThat(m.resolutionRate()).isCloseTo(100.0, within(0.01));
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("ResilienceMetrics.resolutionRate()")
    class ResolutionRate {

        @Test
        @DisplayName("0 eventos → 100.0")
        void zeroEvents() {
            var m = new CriticalEventLifecycleService.ResilienceMetrics(0, 0, 0, 0.0, 0);
            assertThat(m.resolutionRate()).isCloseTo(100.0, within(0.01));
        }

        @Test
        @DisplayName("1 de 2 → 50.0")
        void halfResolved() {
            var m = new CriticalEventLifecycleService.ResilienceMetrics(2, 1, 1, 5.0, 0);
            assertThat(m.resolutionRate()).isCloseTo(50.0, within(0.01));
        }

        @Test
        @DisplayName("resultado redondeado a 1 decimal")
        void roundedToOneDecimal() {
            // 2/3 = 66.6666... → 66.7
            var m = new CriticalEventLifecycleService.ResilienceMetrics(3, 2, 1, 0.0, 0);
            assertThat(m.resolutionRate()).isCloseTo(66.7, within(0.01));
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("consultas — getActiveEvents / getResolvedEvents")
    class Queries {

        @Test
        @DisplayName("getActiveEvents() delega a criticalEventRepo.findActiveByFamilyId()")
        void delegatesActive() {
            List<FamilyCriticalEvent> events = List.of(criticalEvent(1L, "IN_PROGRESS"));
            when(criticalEventRepo.findActiveByFamilyId(FAM_ID)).thenReturn(events);

            assertThat(service.getActiveEvents(FAM_ID)).isEqualTo(events);
        }

        @Test
        @DisplayName("getResolvedEvents() delega a criticalEventRepo.findResolvedByFamilyId()")
        void delegatesResolved() {
            List<FamilyCriticalEvent> events = List.of(criticalEvent(2L, "RESOLVED"));
            when(criticalEventRepo.findResolvedByFamilyId(FAM_ID)).thenReturn(events);

            assertThat(service.getResolvedEvents(FAM_ID)).isEqualTo(events);
        }
    }
}
