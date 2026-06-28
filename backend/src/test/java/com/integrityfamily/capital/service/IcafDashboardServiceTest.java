package com.integrityfamily.capital.service;

import com.integrityfamily.capital.dto.IcafDashboardResponse;
import com.integrityfamily.domain.FamilyCapitalSnapshot;
import com.integrityfamily.domain.FamilyLongitudinalState;
import com.integrityfamily.domain.repository.FamilyCapitalSnapshotRepository;
import com.integrityfamily.domain.repository.FamilyCriticalEventRepository;
import com.integrityfamily.domain.repository.FamilyIcafAnswerRepository;
import com.integrityfamily.domain.repository.FamilyLongitudinalStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("IcafDashboardService — Unit Tests")
class IcafDashboardServiceTest {

    @Mock FamilyCapitalSnapshotRepository    snapshotRepo;
    @Mock FamilyLongitudinalStateRepository  longitudinalRepo;
    @Mock FamilyCriticalEventRepository      criticalEventRepo;
    @Mock FamilyIcafAnswerRepository         icafAnswerRepo;
    @Mock IcafScoringEngine                  icafScoringEngine;

    @InjectMocks IcafDashboardService service;

    private static final Long FAM_ID = 1L;

    // ── helpers ──────────────────────────────────────────────────────────────

    private FamilyCapitalSnapshot snapshot(double icaf, int madurez) {
        return FamilyCapitalSnapshot.builder()
                .icaf(icaf).madurezNivel(madurez)
                .domCohesion(icaf).domConfianza(icaf).domResiliencia(icaf)
                .domComunicacion(icaf).domAutonomia(icaf).domBienestar(icaf)
                .domProposito(icaf).domIntegracion(icaf).domEmprendimiento(icaf)
                .domLegado(icaf).domMadurez(icaf)
                .triggerType("MANUAL")
                .createdAt(LocalDateTime.of(2026, 6, 20, 10, 0))
                .build();
    }

    private void stubNoEvents() {
        when(criticalEventRepo.countActiveByFamilyId(FAM_ID)).thenReturn(0L);
        when(criticalEventRepo.countByFamilyIdAndStatus(eq(FAM_ID), anyString())).thenReturn(0L);
        when(criticalEventRepo.avgDaysToResolutionByFamilyId(FAM_ID)).thenReturn(0.0);
        when(criticalEventRepo.totalRelapsesByFamilyId(FAM_ID)).thenReturn(0L);
    }

    private void stubNoAnswers() {
        when(icafAnswerRepo.hasAnswers(eq(FAM_ID), anyString())).thenReturn(false);
    }

    private void stubNoLongitudinalState() {
        when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.empty());
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("sin snapshot previo")
    class NoSnapshot {

        @Test
        @DisplayName("calcula ICaF automáticamente y retorna el nuevo snapshot")
        void triggersCalculation() {
            FamilyCapitalSnapshot computed = snapshot(55.0, 3);
            when(snapshotRepo.findTopByFamilyIdOrderByCreatedAtDesc(FAM_ID))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of(computed));
            stubNoLongitudinalState();
            stubNoEvents();
            stubNoAnswers();

            IcafDashboardResponse response = service.getDashboard(FAM_ID);

            verify(icafScoringEngine).compute(FAM_ID, "SCHEDULED");
            assertThat(response.icaf()).isCloseTo(55.0, within(0.01));
            assertThat(response.hasRealData()).isTrue();
        }

        @Test
        @DisplayName("cálculo falla → retorna dashboard vacío con valores seguros")
        void calculationFails() {
            when(snapshotRepo.findTopByFamilyIdOrderByCreatedAtDesc(FAM_ID))
                    .thenReturn(Optional.empty());
            doThrow(new RuntimeException("DB error"))
                    .when(icafScoringEngine).compute(FAM_ID, "SCHEDULED");

            IcafDashboardResponse response = service.getDashboard(FAM_ID);

            assertThat(response.icaf()).isEqualTo(0.0);
            assertThat(response.madurezNivel()).isEqualTo(1);
            assertThat(response.madurezLabel()).isEqualTo("Supervivencia");
            assertThat(response.hasRealData()).isFalse();
            assertThat(response.domains()).isEmpty();
        }

        @Test
        @DisplayName("cálculo exitoso pero segundo findTop sigue vacío → dashboard vacío")
        void calculationSuccessButStillEmpty() {
            when(snapshotRepo.findTopByFamilyIdOrderByCreatedAtDesc(FAM_ID))
                    .thenReturn(Optional.empty());

            IcafDashboardResponse response = service.getDashboard(FAM_ID);

            assertThat(response.icaf()).isEqualTo(0.0);
            assertThat(response.hasRealData()).isFalse();
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("con snapshot existente")
    class WithSnapshot {

        @Test
        @DisplayName("retorna ICaF y madurez del snapshot")
        void returnsSnapshotData() {
            when(snapshotRepo.findTopByFamilyIdOrderByCreatedAtDesc(FAM_ID))
                    .thenReturn(Optional.of(snapshot(72.0, 4)));
            stubNoLongitudinalState();
            stubNoEvents();
            stubNoAnswers();

            IcafDashboardResponse response = service.getDashboard(FAM_ID);

            assertThat(response.icaf()).isCloseTo(72.0, within(0.01));
            assertThat(response.madurezNivel()).isEqualTo(4);
            assertThat(response.madurezLabel()).isEqualTo("Propósito");
            assertThat(response.familyId()).isEqualTo(FAM_ID);
        }

        @Test
        @DisplayName("NO recalcula si ya hay snapshot")
        void doesNotRecalculate() {
            when(snapshotRepo.findTopByFamilyIdOrderByCreatedAtDesc(FAM_ID))
                    .thenReturn(Optional.of(snapshot(60.0, 3)));
            stubNoLongitudinalState();
            stubNoEvents();
            stubNoAnswers();

            service.getDashboard(FAM_ID);

            verify(icafScoringEngine, never()).compute(any(), any());
        }

        @Test
        @DisplayName("retorna 11 dominios en la lista")
        void returns11Domains() {
            when(snapshotRepo.findTopByFamilyIdOrderByCreatedAtDesc(FAM_ID))
                    .thenReturn(Optional.of(snapshot(60.0, 3)));
            stubNoLongitudinalState();
            stubNoEvents();
            stubNoAnswers();

            IcafDashboardResponse response = service.getDashboard(FAM_ID);

            assertThat(response.domains()).hasSize(11);
        }

        @Test
        @DisplayName("lastCalculatedAt formateado correctamente")
        void lastCalculatedAtFormatted() {
            when(snapshotRepo.findTopByFamilyIdOrderByCreatedAtDesc(FAM_ID))
                    .thenReturn(Optional.of(snapshot(60.0, 3)));
            stubNoLongitudinalState();
            stubNoEvents();
            stubNoAnswers();

            IcafDashboardResponse response = service.getDashboard(FAM_ID);

            assertThat(response.lastCalculatedAt()).isEqualTo("2026-06-20 10:00");
        }

        @Test
        @DisplayName("snapshot con createdAt null → lastCalculatedAt null")
        void nullCreatedAt() {
            FamilyCapitalSnapshot snap = snapshot(60.0, 3);
            snap.setCreatedAt(null);
            when(snapshotRepo.findTopByFamilyIdOrderByCreatedAtDesc(FAM_ID))
                    .thenReturn(Optional.of(snap));
            stubNoLongitudinalState();
            stubNoEvents();
            stubNoAnswers();

            IcafDashboardResponse response = service.getDashboard(FAM_ID);

            assertThat(response.lastCalculatedAt()).isNull();
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("historial longitudinal")
    class LongitudinalHistory {

        @Test
        @DisplayName("con estado longitudinal → popula icaf6m / icaf12m / icaf36m / trend")
        void populatesHistory() {
            FamilyLongitudinalState state = new FamilyLongitudinalState();
            state.setIcaf6mAgo(48.0);
            state.setIcaf12mAgo(40.0);
            state.setIcaf36mAgo(30.0);
            state.setIcafTrend("IMPROVING");

            when(snapshotRepo.findTopByFamilyIdOrderByCreatedAtDesc(FAM_ID))
                    .thenReturn(Optional.of(snapshot(60.0, 3)));
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(state));
            stubNoEvents();
            stubNoAnswers();

            IcafDashboardResponse response = service.getDashboard(FAM_ID);

            assertThat(response.icaf6mAgo()).isCloseTo(48.0, within(0.01));
            assertThat(response.icaf12mAgo()).isCloseTo(40.0, within(0.01));
            assertThat(response.icaf36mAgo()).isCloseTo(30.0, within(0.01));
            assertThat(response.trend()).isEqualTo("IMPROVING");
        }

        @Test
        @DisplayName("sin estado longitudinal → historial null y trend STABLE")
        void noStateDefaults() {
            when(snapshotRepo.findTopByFamilyIdOrderByCreatedAtDesc(FAM_ID))
                    .thenReturn(Optional.of(snapshot(60.0, 3)));
            stubNoLongitudinalState();
            stubNoEvents();
            stubNoAnswers();

            IcafDashboardResponse response = service.getDashboard(FAM_ID);

            assertThat(response.icaf6mAgo()).isNull();
            assertThat(response.icaf12mAgo()).isNull();
            assertThat(response.icaf36mAgo()).isNull();
            assertThat(response.trend()).isEqualTo("STABLE");
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("métricas de eventos críticos")
    class CriticalEventMetrics {

        @Test
        @DisplayName("sin eventos → resolutionRate = 100%, relapses = 0")
        void noEvents() {
            when(snapshotRepo.findTopByFamilyIdOrderByCreatedAtDesc(FAM_ID))
                    .thenReturn(Optional.of(snapshot(60.0, 3)));
            stubNoLongitudinalState();
            stubNoEvents();
            stubNoAnswers();

            IcafDashboardResponse response = service.getDashboard(FAM_ID);

            assertThat(response.activeEvents()).isEqualTo(0L);
            assertThat(response.resolvedEvents()).isEqualTo(0L);
            assertThat(response.resolutionRate()).isCloseTo(100.0, within(0.01));
            assertThat(response.totalRelapses()).isEqualTo(0L);
        }

        @Test
        @DisplayName("2 resueltos + 1 activo → resolutionRate ≈ 66.7%")
        void partialResolution() {
            when(snapshotRepo.findTopByFamilyIdOrderByCreatedAtDesc(FAM_ID))
                    .thenReturn(Optional.of(snapshot(60.0, 3)));
            stubNoLongitudinalState();
            stubNoAnswers();

            when(criticalEventRepo.countActiveByFamilyId(FAM_ID)).thenReturn(1L);
            when(criticalEventRepo.countByFamilyIdAndStatus(FAM_ID, "RESOLVED")).thenReturn(2L);
            when(criticalEventRepo.countByFamilyIdAndStatus(FAM_ID, "CLOSED")).thenReturn(0L);
            when(criticalEventRepo.countByFamilyIdAndStatus(FAM_ID, "RELAPSED")).thenReturn(0L);
            when(criticalEventRepo.avgDaysToResolutionByFamilyId(FAM_ID)).thenReturn(15.0);
            when(criticalEventRepo.totalRelapsesByFamilyId(FAM_ID)).thenReturn(1L);

            IcafDashboardResponse response = service.getDashboard(FAM_ID);

            // total=3, resolved=2 → 2/3*100 = 66.7
            assertThat(response.resolutionRate()).isCloseTo(66.7, within(0.1));
            assertThat(response.activeEvents()).isEqualTo(1L);
            assertThat(response.resolvedEvents()).isEqualTo(2L);
            assertThat(response.avgDaysToResolution()).isCloseTo(15.0, within(0.01));
            assertThat(response.totalRelapses()).isEqualTo(1L);
        }

        @Test
        @DisplayName("RESOLVED + CLOSED ambos cuentan como resueltos")
        void resolvedAndClosedCounted() {
            when(snapshotRepo.findTopByFamilyIdOrderByCreatedAtDesc(FAM_ID))
                    .thenReturn(Optional.of(snapshot(60.0, 3)));
            stubNoLongitudinalState();
            stubNoAnswers();

            when(criticalEventRepo.countActiveByFamilyId(FAM_ID)).thenReturn(0L);
            when(criticalEventRepo.countByFamilyIdAndStatus(FAM_ID, "RESOLVED")).thenReturn(3L);
            when(criticalEventRepo.countByFamilyIdAndStatus(FAM_ID, "CLOSED")).thenReturn(2L);
            when(criticalEventRepo.countByFamilyIdAndStatus(FAM_ID, "RELAPSED")).thenReturn(0L);
            when(criticalEventRepo.avgDaysToResolutionByFamilyId(FAM_ID)).thenReturn(0.0);
            when(criticalEventRepo.totalRelapsesByFamilyId(FAM_ID)).thenReturn(0L);

            IcafDashboardResponse response = service.getDashboard(FAM_ID);

            assertThat(response.resolvedEvents()).isEqualTo(5L); // 3 + 2
            assertThat(response.resolutionRate()).isCloseTo(100.0, within(0.01));
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("indicadores de fuentes reales")
    class DataSources {

        @Test
        @DisplayName("con respuestas de confianza y bienestar → hasRealData=true")
        void withQuestionnaireAnswers() {
            when(snapshotRepo.findTopByFamilyIdOrderByCreatedAtDesc(FAM_ID))
                    .thenReturn(Optional.of(snapshot(60.0, 3)));
            stubNoLongitudinalState();
            stubNoEvents();
            when(icafAnswerRepo.hasAnswers(FAM_ID, IcafQuestionnaireService.DOMAIN_CONFIANZA))
                    .thenReturn(true);
            when(icafAnswerRepo.hasAnswers(FAM_ID, IcafQuestionnaireService.DOMAIN_BIENESTAR))
                    .thenReturn(true);

            IcafDashboardResponse response = service.getDashboard(FAM_ID);

            // Dominio confianza debe tener source != ESTIMADO
            assertThat(response.domains()).isNotEmpty();
            assertThat(response.hasRealData()).isTrue();
        }

        @Test
        @DisplayName("hasRealData=false cuando ICaF=0")
        void hasRealDataFalseWhenZero() {
            when(snapshotRepo.findTopByFamilyIdOrderByCreatedAtDesc(FAM_ID))
                    .thenReturn(Optional.of(snapshot(0.0, 1)));
            stubNoLongitudinalState();
            stubNoEvents();
            stubNoAnswers();

            IcafDashboardResponse response = service.getDashboard(FAM_ID);

            assertThat(response.hasRealData()).isFalse();
        }
    }
}
