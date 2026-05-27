package com.integrityfamily.report.service;

import com.integrityfamily.common.exception.NotFoundException;
import com.integrityfamily.common.repository.NotificationLogRepository;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.RiskSnapshot;
import com.integrityfamily.domain.repository.ChecklistRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.RiskSnapshotRepository;
import com.integrityfamily.report.dto.TransformationSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExecutiveReportService")
class ExecutiveReportServiceTest {

    @Mock FamilyRepository         familyRepository;
    @Mock RiskSnapshotRepository   riskSnapshotRepository;
    @Mock ChecklistRepository      checklistRepository;
    @Mock NotificationLogRepository notificationLogRepository;

    @InjectMocks ExecutiveReportService service;

    // ── Helper ────────────────────────────────────────────────────────────────

    private RiskSnapshot snap(double icf) {
        return RiskSnapshot.builder().icf(icf).build();
    }

    // ── generateRawSummary ────────────────────────────────────────────────────

    @Test
    @DisplayName("lanza NotFoundException cuando la familia no existe")
    void generateRawSummary_familyNotFound_throwsNotFoundException() {
        when(familyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateRawSummary(1L))
                .isInstanceOf(NotFoundException.class);
    }

    @Nested
    @DisplayName("sin historial de riesgo")
    class NoRiskHistory {

        @Test
        @DisplayName("retorna initialIcf y currentIcf en 0.0 cuando no hay snapshots")
        void noSnapshots_returnsZeroIcfs() {
            Family family = Family.builder().id(1L).name("Los García").municipio(null)
                    .currentMilestone("W1").build();
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(riskSnapshotRepository.findByFamilyIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
            when(notificationLogRepository.countByFamilyIdAndType(1L, "CRISIS_ALERT")).thenReturn(0L);
            when(checklistRepository.countByFamilyIdAndCompletedTrue(1L)).thenReturn(0L);

            TransformationSummary summary = service.generateRawSummary(1L);

            assertThat(summary.initialIcf()).isEqualTo(0.0);
            assertThat(summary.currentIcf()).isEqualTo(0.0);
            assertThat(summary.peakIcf()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("con historial de riesgo")
    class WithRiskHistory {

        @Test
        @DisplayName("calcula initialIcf como ultimo snapshot, currentIcf como primero y peakIcf como maximo")
        void multipleSnapshots_correctIcfDerivation() {
            // Orden DESC: más reciente primero → [70.0, 55.0, 40.0]
            // initialIcf = 40.0 (oldest = last), currentIcf = 70.0 (newest = first), peakIcf = 70.0
            Family family = Family.builder().id(2L).name("Los Pérez").municipio(null)
                    .currentMilestone("M3").build();
            when(familyRepository.findById(2L)).thenReturn(Optional.of(family));
            when(riskSnapshotRepository.findByFamilyIdOrderByCreatedAtDesc(2L))
                    .thenReturn(List.of(snap(70.0), snap(55.0), snap(40.0)));
            when(notificationLogRepository.countByFamilyIdAndType(2L, "CRISIS_ALERT")).thenReturn(2L);
            when(checklistRepository.countByFamilyIdAndCompletedTrue(2L)).thenReturn(10L);

            TransformationSummary summary = service.generateRawSummary(2L);

            assertThat(summary.initialIcf()).isEqualTo(40.0);
            assertThat(summary.currentIcf()).isEqualTo(70.0);
            assertThat(summary.peakIcf()).isEqualTo(70.0);
            assertThat(summary.sentinelAlertsTriggered()).isEqualTo(2L);
            assertThat(summary.missionsCompleted()).isEqualTo(10L);
            assertThat(summary.currentMilestone()).isEqualTo("M3");
        }

        @Test
        @DisplayName("peakIcf es el valor maximo del historial aunque no sea el ultimo ni el primero")
        void peakIcfIsMiddleElement() {
            // Orden DESC: [60.0, 85.0, 50.0]  → peak = 85.0
            Family family = Family.builder().id(3L).name("Los Silva").municipio(null)
                    .currentMilestone("M6").build();
            when(familyRepository.findById(3L)).thenReturn(Optional.of(family));
            when(riskSnapshotRepository.findByFamilyIdOrderByCreatedAtDesc(3L))
                    .thenReturn(List.of(snap(60.0), snap(85.0), snap(50.0)));
            when(notificationLogRepository.countByFamilyIdAndType(3L, "CRISIS_ALERT")).thenReturn(0L);
            when(checklistRepository.countByFamilyIdAndCompletedTrue(3L)).thenReturn(5L);

            TransformationSummary summary = service.generateRawSummary(3L);

            assertThat(summary.peakIcf()).isEqualTo(85.0);
            assertThat(summary.initialIcf()).isEqualTo(50.0);
        }
    }

    // ── calculateRegionalAverage ──────────────────────────────────────────────

    @Nested
    @DisplayName("benchmarking regional")
    class RegionalBenchmark {

        @Test
        @DisplayName("retorna 50.0 por defecto cuando municipio es null")
        void nullMunicipio_returnsDefault50() {
            Family family = Family.builder().id(4L).name("Sin ciudad").municipio(null)
                    .currentMilestone("W1").build();
            when(familyRepository.findById(4L)).thenReturn(Optional.of(family));
            when(riskSnapshotRepository.findByFamilyIdOrderByCreatedAtDesc(4L)).thenReturn(List.of());
            when(notificationLogRepository.countByFamilyIdAndType(4L, "CRISIS_ALERT")).thenReturn(0L);
            when(checklistRepository.countByFamilyIdAndCompletedTrue(4L)).thenReturn(0L);

            TransformationSummary summary = service.generateRawSummary(4L);

            assertThat(summary.regionalAverageIcf()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("calcula promedio del ICF mas reciente de cada familia del mismo municipio")
        void withMunicipio_calculatesAverageOfLatestSnapshots() {
            Family family = Family.builder().id(5L).name("Familia Cali").municipio("Cali")
                    .currentMilestone("M1").build();
            Family f2 = Family.builder().id(6L).name("Otra Cali").build();
            Family f3 = Family.builder().id(7L).name("Tercera Cali").build();

            when(familyRepository.findById(5L)).thenReturn(Optional.of(family));
            when(riskSnapshotRepository.findByFamilyIdOrderByCreatedAtDesc(5L)).thenReturn(List.of());
            when(notificationLogRepository.countByFamilyIdAndType(5L, "CRISIS_ALERT")).thenReturn(0L);
            when(checklistRepository.countByFamilyIdAndCompletedTrue(5L)).thenReturn(0L);
            // Regional: findByMunicipio y luego findFirst para cada una
            when(familyRepository.findByMunicipio("Cali")).thenReturn(List.of(f2, f3));
            when(riskSnapshotRepository.findFirstByFamilyIdOrderByCreatedAtDesc(6L))
                    .thenReturn(Optional.of(snap(70.0)));
            when(riskSnapshotRepository.findFirstByFamilyIdOrderByCreatedAtDesc(7L))
                    .thenReturn(Optional.of(snap(80.0)));

            TransformationSummary summary = service.generateRawSummary(5L);

            // (70.0 + 80.0) / 2 = 75.0
            assertThat(summary.regionalAverageIcf()).isEqualTo(75.0);
        }
    }
}
