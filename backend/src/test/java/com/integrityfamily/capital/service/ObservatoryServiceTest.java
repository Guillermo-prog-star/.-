package com.integrityfamily.capital.service;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyCapitalSnapshot;
import com.integrityfamily.domain.FamilyCriticalEvent;
import com.integrityfamily.domain.FamilyLongitudinalState;
import com.integrityfamily.domain.ObservatorySnapshot;
import com.integrityfamily.domain.repository.FamilyCapitalSnapshotRepository;
import com.integrityfamily.domain.repository.FamilyCriticalEventRepository;
import com.integrityfamily.domain.repository.FamilyLongitudinalStateRepository;
import com.integrityfamily.domain.repository.ObservatorySnapshotRepository;
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
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ObservatoryService — Unit Tests")
class ObservatoryServiceTest {

    @Mock FamilyCapitalSnapshotRepository  capitalSnapshotRepo;
    @Mock FamilyCriticalEventRepository    criticalEventRepo;
    @Mock FamilyLongitudinalStateRepository longitudinalRepo;
    @Mock ObservatorySnapshotRepository    observatoryRepo;

    @InjectMocks ObservatoryService service;

    private static final YearMonth JUN_2026 = YearMonth.of(2026, 6);
    private static final LocalDate JUN_01   = LocalDate.of(2026, 6, 1);

    // ── helpers ──────────────────────────────────────────────────────────────

    private Family family(long id) {
        Family f = new Family();
        f.setId(id);
        return f;
    }

    private FamilyCapitalSnapshot snap(long familyId, double icaf, int madurez) {
        return snap(familyId, icaf, madurez, LocalDateTime.of(2026, 6, 15, 10, 0));
    }

    private FamilyCapitalSnapshot snap(long familyId, double icaf, int madurez, LocalDateTime at) {
        return FamilyCapitalSnapshot.builder()
                .family(family(familyId))
                .icaf(icaf).madurezNivel(madurez)
                .domCohesion(icaf).domConfianza(icaf).domResiliencia(icaf)
                .domComunicacion(icaf).domAutonomia(icaf).domBienestar(icaf)
                .domProposito(icaf).domIntegracion(icaf).domEmprendimiento(icaf)
                .domLegado(icaf).domMadurez(icaf)
                .createdAt(at)
                .build();
    }

    private FamilyCriticalEvent event(Integer daysToResolution) {
        FamilyCriticalEvent e = new FamilyCriticalEvent();
        e.setDaysToResolution(daysToResolution);
        return e;
    }

    private void stubEmptyEvents() {
        when(criticalEventRepo.findDetectedInRange(any(), any())).thenReturn(List.of());
        when(criticalEventRepo.findResolvedInRange(any(), any())).thenReturn(List.of());
    }

    private void stubNoTrends() {
        when(longitudinalRepo.findAll()).thenReturn(List.of());
    }

    private void stubNewSnapshot() {
        when(observatoryRepo.findBySnapshotMonth(any())).thenReturn(Optional.empty());
        when(observatoryRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("generateForMonth() — sin datos")
    class EmptyMonth {

        @Test
        @DisplayName("sin snapshots → familiesCount=0, icafAvg=0, resolutionRate=100%")
        void noFamilies() {
            when(capitalSnapshotRepo.findAllInMonth(any(), any())).thenReturn(List.of());
            stubEmptyEvents();
            stubNoTrends();
            stubNewSnapshot();

            ObservatorySnapshot result = service.generateForMonth(JUN_2026);

            assertThat(result.getFamiliesCount()).isEqualTo(0);
            assertThat(result.getIcafAvg()).isEqualTo(0.0);
            assertThat(result.getResolutionRatePct()).isCloseTo(100.0, within(0.01));
            assertThat(result.getSnapshotMonth()).isEqualTo(JUN_01);
        }

        @Test
        @DisplayName("sin eventos → eventsDetected=0, eventsResolved=0, resolutionRate=100%")
        void noEvents() {
            when(capitalSnapshotRepo.findAllInMonth(any(), any())).thenReturn(List.of());
            stubEmptyEvents();
            stubNoTrends();
            stubNewSnapshot();

            ObservatorySnapshot result = service.generateForMonth(JUN_2026);

            assertThat(result.getEventsDetected()).isEqualTo(0);
            assertThat(result.getEventsResolved()).isEqualTo(0);
            assertThat(result.getResolutionRatePct()).isCloseTo(100.0, within(0.01));
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("generateForMonth() — distribución ICaF")
    class IcafDistribution {

        @Test
        @DisplayName("1 familia → avg=icaf, p25=p50=p75=icaf")
        void oneFamily() {
            when(capitalSnapshotRepo.findAllInMonth(any(), any()))
                    .thenReturn(List.of(snap(1L, 70.0, 4)));
            stubEmptyEvents();
            stubNoTrends();
            stubNewSnapshot();

            ObservatorySnapshot result = service.generateForMonth(JUN_2026);

            assertThat(result.getFamiliesCount()).isEqualTo(1);
            assertThat(result.getIcafAvg()).isCloseTo(70.0, within(0.01));
            assertThat(result.getIcafP25()).isCloseTo(70.0, within(0.01));
            assertThat(result.getIcafMedian()).isCloseTo(70.0, within(0.01));
            assertThat(result.getIcafP75()).isCloseTo(70.0, within(0.01));
        }

        @Test
        @DisplayName("4 familias [40,50,60,70] → avg=55, median=55, p25=47.5, p75=62.5")
        void fourFamilies() {
            when(capitalSnapshotRepo.findAllInMonth(any(), any())).thenReturn(List.of(
                    snap(1L, 40.0, 2),
                    snap(2L, 50.0, 3),
                    snap(3L, 60.0, 3),
                    snap(4L, 70.0, 4)
            ));
            stubEmptyEvents();
            stubNoTrends();
            stubNewSnapshot();

            ObservatorySnapshot result = service.generateForMonth(JUN_2026);

            assertThat(result.getFamiliesCount()).isEqualTo(4);
            assertThat(result.getIcafAvg()).isCloseTo(55.0, within(0.01));
            assertThat(result.getIcafMedian()).isCloseTo(55.0, within(0.5));
            assertThat(result.getIcafP25()).isLessThan(result.getIcafMedian());
            assertThat(result.getIcafP75()).isGreaterThan(result.getIcafMedian());
        }

        @Test
        @DisplayName("misma familia con 2 snapshots → solo el más reciente cuenta")
        void deduplicatesByFamily() {
            LocalDateTime earlier = LocalDateTime.of(2026, 6, 5, 8, 0);
            LocalDateTime later   = LocalDateTime.of(2026, 6, 20, 8, 0);

            when(capitalSnapshotRepo.findAllInMonth(any(), any())).thenReturn(List.of(
                    snap(1L, 30.0, 2, earlier),  // snapshot viejo
                    snap(1L, 80.0, 5, later)     // snapshot reciente → debe ganar
            ));
            stubEmptyEvents();
            stubNoTrends();
            stubNewSnapshot();

            ObservatorySnapshot result = service.generateForMonth(JUN_2026);

            // Una sola familia, ICaF del snapshot más reciente
            assertThat(result.getFamiliesCount()).isEqualTo(1);
            assertThat(result.getIcafAvg()).isCloseTo(80.0, within(0.01));
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("generateForMonth() — distribución madurez")
    class MadurezDistribution {

        @Test
        @DisplayName("2 familias nivel 3 + 2 nivel 4 → nivel3Pct=50%, nivel4Pct=50%")
        void madurezPercentages() {
            when(capitalSnapshotRepo.findAllInMonth(any(), any())).thenReturn(List.of(
                    snap(1L, 55.0, 3),
                    snap(2L, 58.0, 3),
                    snap(3L, 70.0, 4),
                    snap(4L, 72.0, 4)
            ));
            stubEmptyEvents();
            stubNoTrends();
            stubNewSnapshot();

            ObservatorySnapshot result = service.generateForMonth(JUN_2026);

            assertThat(result.getNivel3Pct()).isCloseTo(50.0, within(0.01));
            assertThat(result.getNivel4Pct()).isCloseTo(50.0, within(0.01));
            assertThat(result.getNivel1Pct()).isEqualTo(0.0);
            assertThat(result.getNivel2Pct()).isEqualTo(0.0);
            assertThat(result.getNivel5Pct()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("madurezNivel null → se asume nivel 1")
        void nullMadurezCountsAs1() {
            FamilyCapitalSnapshot s = snap(1L, 20.0, 1);
            s.setMadurezNivel(null);
            when(capitalSnapshotRepo.findAllInMonth(any(), any())).thenReturn(List.of(s));
            stubEmptyEvents();
            stubNoTrends();
            stubNewSnapshot();

            ObservatorySnapshot result = service.generateForMonth(JUN_2026);

            assertThat(result.getNivel1Pct()).isCloseTo(100.0, within(0.01));
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("generateForMonth() — eventos críticos")
    class CriticalEvents {

        @Test
        @DisplayName("3 detectados, 2 resueltos → resolutionRate = 66.67%")
        void partialResolution() {
            when(capitalSnapshotRepo.findAllInMonth(any(), any())).thenReturn(List.of());
            when(criticalEventRepo.findDetectedInRange(any(), any()))
                    .thenReturn(List.of(event(null), event(null), event(null)));
            when(criticalEventRepo.findResolvedInRange(any(), any()))
                    .thenReturn(List.of(event(10), event(20)));
            stubNoTrends();
            stubNewSnapshot();

            ObservatorySnapshot result = service.generateForMonth(JUN_2026);

            assertThat(result.getEventsDetected()).isEqualTo(3);
            assertThat(result.getEventsResolved()).isEqualTo(2);
            assertThat(result.getResolutionRatePct()).isCloseTo(66.67, within(0.1));
        }

        @Test
        @DisplayName("avgDaysResolution = promedio de daysToResolution de eventos resueltos")
        void avgDaysResolution() {
            when(capitalSnapshotRepo.findAllInMonth(any(), any())).thenReturn(List.of());
            when(criticalEventRepo.findDetectedInRange(any(), any()))
                    .thenReturn(List.of(event(null), event(null)));
            when(criticalEventRepo.findResolvedInRange(any(), any()))
                    .thenReturn(List.of(event(10), event(30)));
            stubNoTrends();
            stubNewSnapshot();

            ObservatorySnapshot result = service.generateForMonth(JUN_2026);

            assertThat(result.getAvgDaysResolution()).isCloseTo(20.0, within(0.01));
        }

        @Test
        @DisplayName("eventos resueltos con daysToResolution null → excluidos del promedio")
        void nullDaysExcluded() {
            when(capitalSnapshotRepo.findAllInMonth(any(), any())).thenReturn(List.of());
            when(criticalEventRepo.findDetectedInRange(any(), any()))
                    .thenReturn(List.of(event(null)));
            when(criticalEventRepo.findResolvedInRange(any(), any()))
                    .thenReturn(List.of(event(null)));
            stubNoTrends();
            stubNewSnapshot();

            ObservatorySnapshot result = service.generateForMonth(JUN_2026);

            assertThat(result.getAvgDaysResolution()).isEqualTo(0.0);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("generateForMonth() — tendencias longitudinales")
    class Trends {

        @Test
        @DisplayName("2 IMPROVING, 1 DECLINING, 1 STABLE → conteos correctos")
        void trendCounts() {
            when(capitalSnapshotRepo.findAllInMonth(any(), any())).thenReturn(List.of());
            stubEmptyEvents();

            FamilyLongitudinalState imp1 = new FamilyLongitudinalState(); imp1.setIcafTrend("IMPROVING");
            FamilyLongitudinalState imp2 = new FamilyLongitudinalState(); imp2.setIcafTrend("IMPROVING");
            FamilyLongitudinalState dec  = new FamilyLongitudinalState(); dec.setIcafTrend("DECLINING");
            FamilyLongitudinalState stb  = new FamilyLongitudinalState(); stb.setIcafTrend("STABLE");
            when(longitudinalRepo.findAll()).thenReturn(List.of(imp1, imp2, dec, stb));
            stubNewSnapshot();

            ObservatorySnapshot result = service.generateForMonth(JUN_2026);

            assertThat(result.getFamiliesImproving()).isEqualTo(2);
            assertThat(result.getFamiliesDeclining()).isEqualTo(1);
            assertThat(result.getFamiliesStable()).isEqualTo(1);
        }

        @Test
        @DisplayName("trend null o desconocido → no cuenta en ningún grupo")
        void unknownTrendIgnored() {
            when(capitalSnapshotRepo.findAllInMonth(any(), any())).thenReturn(List.of());
            stubEmptyEvents();

            FamilyLongitudinalState unknown = new FamilyLongitudinalState();
            unknown.setIcafTrend(null);
            when(longitudinalRepo.findAll()).thenReturn(List.of(unknown));
            stubNewSnapshot();

            ObservatorySnapshot result = service.generateForMonth(JUN_2026);

            assertThat(result.getFamiliesImproving()).isEqualTo(0);
            assertThat(result.getFamiliesDeclining()).isEqualTo(0);
            assertThat(result.getFamiliesStable()).isEqualTo(0);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("generateForMonth() — UPSERT")
    class Upsert {

        @Test
        @DisplayName("snapshot previo existente → actualiza, no crea uno nuevo")
        void updatesExistingSnapshot() {
            ObservatorySnapshot existing = ObservatorySnapshot.builder()
                    .snapshotMonth(JUN_01).familiesCount(5).build();
            when(observatoryRepo.findBySnapshotMonth(JUN_01)).thenReturn(Optional.of(existing));
            when(observatoryRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(capitalSnapshotRepo.findAllInMonth(any(), any()))
                    .thenReturn(List.of(snap(1L, 60.0, 3)));
            stubEmptyEvents();
            stubNoTrends();

            service.generateForMonth(JUN_2026);

            ArgumentCaptor<ObservatorySnapshot> captor =
                    ArgumentCaptor.forClass(ObservatorySnapshot.class);
            verify(observatoryRepo, times(1)).save(captor.capture());
            assertThat(captor.getValue()).isSameAs(existing);
            assertThat(existing.getFamiliesCount()).isEqualTo(1); // actualizado
        }

        @Test
        @DisplayName("sin snapshot previo → crea uno nuevo con snapshotMonth correcto")
        void createsNewSnapshot() {
            when(capitalSnapshotRepo.findAllInMonth(any(), any())).thenReturn(List.of());
            stubEmptyEvents();
            stubNoTrends();
            stubNewSnapshot();

            ObservatorySnapshot result = service.generateForMonth(JUN_2026);

            verify(observatoryRepo).save(any());
            assertThat(result.getSnapshotMonth()).isEqualTo(JUN_01);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("generateForMonth() — promedios de dominio")
    class DomainAverages {

        @Test
        @DisplayName("cohesion = promedio de todas las familias del mes")
        void cohesionAverage() {
            when(capitalSnapshotRepo.findAllInMonth(any(), any())).thenReturn(List.of(
                    snap(1L, 40.0, 2),
                    snap(2L, 80.0, 5)
            ));
            stubEmptyEvents();
            stubNoTrends();
            stubNewSnapshot();

            ObservatorySnapshot result = service.generateForMonth(JUN_2026);

            // domCohesion = icaf en helper → avg(40,80)=60
            assertThat(result.getAvgDomCohesion()).isCloseTo(60.0, within(0.01));
        }

        @Test
        @DisplayName("dominio con valor 0 → excluido del promedio")
        void zeroValueExcluded() {
            FamilyCapitalSnapshot s1 = snap(1L, 60.0, 3);
            FamilyCapitalSnapshot s2 = snap(2L, 80.0, 4);
            s2.setDomCohesion(0.0); // excluido
            when(capitalSnapshotRepo.findAllInMonth(any(), any())).thenReturn(List.of(s1, s2));
            stubEmptyEvents();
            stubNoTrends();
            stubNewSnapshot();

            ObservatorySnapshot result = service.generateForMonth(JUN_2026);

            // Solo s1 contribuye: cohesion=60
            assertThat(result.getAvgDomCohesion()).isCloseTo(60.0, within(0.01));
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("getHistory()")
    class GetHistory {

        @Test
        @DisplayName("limita al número de meses solicitado")
        void limitsToN() {
            List<ObservatorySnapshot> all = List.of(
                    ObservatorySnapshot.builder().snapshotMonth(LocalDate.of(2026, 4, 1)).build(),
                    ObservatorySnapshot.builder().snapshotMonth(LocalDate.of(2026, 5, 1)).build(),
                    ObservatorySnapshot.builder().snapshotMonth(LocalDate.of(2026, 6, 1)).build()
            );
            when(observatoryRepo.findAllOrderByMonthDesc()).thenReturn(all);

            List<ObservatorySnapshot> result = service.getHistory(2);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("ordena por mes ASC")
        void sortedAsc() {
            List<ObservatorySnapshot> desc = List.of(
                    ObservatorySnapshot.builder().snapshotMonth(LocalDate.of(2026, 6, 1)).build(),
                    ObservatorySnapshot.builder().snapshotMonth(LocalDate.of(2026, 5, 1)).build(),
                    ObservatorySnapshot.builder().snapshotMonth(LocalDate.of(2026, 4, 1)).build()
            );
            when(observatoryRepo.findAllOrderByMonthDesc()).thenReturn(desc);

            List<ObservatorySnapshot> result = service.getHistory(3);

            assertThat(result.get(0).getSnapshotMonth()).isEqualTo(LocalDate.of(2026, 4, 1));
            assertThat(result.get(2).getSnapshotMonth()).isEqualTo(LocalDate.of(2026, 6, 1));
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("getLatest() y getRange()")
    class Queries {

        @Test
        @DisplayName("getLatest() delega a observatoryRepo.findLatest()")
        void getLatest() {
            ObservatorySnapshot snap = ObservatorySnapshot.builder().snapshotMonth(JUN_01).build();
            when(observatoryRepo.findLatest()).thenReturn(Optional.of(snap));

            assertThat(service.getLatest()).contains(snap);
        }

        @Test
        @DisplayName("getRange() delega a observatoryRepo.findInRange()")
        void getRange() {
            LocalDate from = LocalDate.of(2026, 1, 1);
            LocalDate to   = LocalDate.of(2026, 6, 1);
            List<ObservatorySnapshot> expected = List.of(
                    ObservatorySnapshot.builder().snapshotMonth(from).build()
            );
            when(observatoryRepo.findInRange(from, to)).thenReturn(expected);

            assertThat(service.getRange(from, to)).isEqualTo(expected);
        }
    }
}
