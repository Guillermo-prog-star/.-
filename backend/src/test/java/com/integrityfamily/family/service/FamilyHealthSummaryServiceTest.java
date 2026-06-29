package com.integrityfamily.family.service;

import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import com.integrityfamily.family.dto.FamilyHealthSummaryResponse;
import com.integrityfamily.scanner.dto.SubtleSignalRadarResponse;
import com.integrityfamily.scanner.service.SubtleSignalRadarService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FamilyHealthSummaryService — Unit Tests")
class FamilyHealthSummaryServiceTest {

    @Mock FamilyRepository          familyRepository;
    @Mock MemberRepository          memberRepository;
    @Mock EvaluationRepository      evaluationRepository;
    @Mock RiskSnapshotRepository    riskSnapshotRepository;
    @Mock FamilySprintRepository    sprintRepository;
    @Mock TaskEvidenceRepository    evidenceRepository;
    @Mock SubtleSignalRadarService  radarService;
    @Mock FamilyJourneyService      journeyService;

    @InjectMocks FamilyHealthSummaryService service;

    private static final Long FAM_ID = 1L;
    private Family family;

    @BeforeEach
    void setUp() {
        family = new Family();
        family.setId(FAM_ID);
        family.setName("Familia Test");
        family.setSentinelActive(false);
        when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));

        // Defaults — sin datos
        when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM_ID)).thenReturn(List.of());
        when(riskSnapshotRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of());
        when(sprintRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of());
        when(sprintRepository.countByFamilyId(FAM_ID)).thenReturn(0L);
        when(evidenceRepository.findByFamilyId(FAM_ID)).thenReturn(List.of());
        when(memberRepository.countByFamilyId(FAM_ID)).thenReturn(0L);

        var journey = new com.integrityfamily.family.dto.FamilyJourneyResponse(
                FAM_ID, "Familia Test", 0, 7, List.of(), "Completa el perfil.", 1);
        when(journeyService.evaluate(FAM_ID)).thenReturn(journey);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Evaluation finalizedEval(double icf, LocalDateTime finalizedAt) {
        Evaluation e = new Evaluation();
        e.setStatus(EvaluationStatus.FINALIZED);
        e.setIcf(icf);
        e.setFinalizedAt(finalizedAt);
        return e;
    }

    private SubtleSignalRadarResponse radarWith(String phase, String direction, int highSignals) {
        var icfTrend = new SubtleSignalRadarResponse.IcfTrend(75.0, 2.0, 4.0, direction, phase);
        List<SubtleSignalRadarResponse.MicroSignal> signals = new java.util.ArrayList<>();
        for (int i = 0; i < highSignals; i++) {
            signals.add(new SubtleSignalRadarResponse.MicroSignal(
                    "emociones", "CODE_" + i, "Señal alta", "HIGH", 0.9));
        }
        return new SubtleSignalRadarResponse(
                FAM_ID, 3, null, null, null, null, icfTrend,
                signals, List.of(), List.of(), 80, "Narrativa.", LocalDateTime.now());
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("summarize() — familia no encontrada")
    class FamilyNotFound {

        @Test
        @DisplayName("lanza IllegalArgumentException cuando familia no existe")
        void throwsWhenNotFound() {
            when(familyRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.summarize(99L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("summarize() — ICF")
    class IcfSection {

        @Test
        @DisplayName("currentIcf es null sin evaluaciones")
        void icfNullWithoutEvals() {
            FamilyHealthSummaryResponse r = service.summarize(FAM_ID);
            assertThat(r.currentIcf()).isNull();
            assertThat(r.icfLabel()).isEqualTo("Sin datos");
        }

        @Test
        @DisplayName("currentIcf toma el último valor de evaluaciones FINALIZED")
        void icfFromLatestEval() {
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM_ID))
                    .thenReturn(List.of(
                            finalizedEval(60.0, LocalDateTime.now().minusDays(60)),
                            finalizedEval(75.0, LocalDateTime.now().minusDays(5))
                    ));
            FamilyHealthSummaryResponse r = service.summarize(FAM_ID);
            assertThat(r.currentIcf()).isEqualTo(75.0);
        }

        @Test
        @DisplayName("icfLabel es 'Fortaleza' cuando ICF >= 80")
        void icfLabelFortaleza() {
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM_ID))
                    .thenReturn(List.of(finalizedEval(82.0, LocalDateTime.now().minusDays(1))));
            FamilyHealthSummaryResponse r = service.summarize(FAM_ID);
            assertThat(r.icfLabel()).isEqualTo("Fortaleza");
        }

        @Test
        @DisplayName("icfLabel es 'Crítico' cuando ICF < 40")
        void icfLabelCritico() {
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM_ID))
                    .thenReturn(List.of(finalizedEval(35.0, LocalDateTime.now().minusDays(1))));
            FamilyHealthSummaryResponse r = service.summarize(FAM_ID);
            assertThat(r.icfLabel()).isEqualTo("Crítico");
        }

        @Test
        @DisplayName("icfDelta30d calcula la diferencia respecto a evaluación anterior a 30 días")
        void icfDelta30d() {
            var old    = finalizedEval(60.0, LocalDateTime.now().minusDays(45));
            var recent = finalizedEval(70.0, LocalDateTime.now().minusDays(3));
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM_ID))
                    .thenReturn(List.of(old, recent));

            FamilyHealthSummaryResponse r = service.summarize(FAM_ID);
            // delta = 70 - 60 = 10
            assertThat(r.icfDelta30d()).isEqualTo(10.0);
        }

        @Test
        @DisplayName("icfDelta30d es null con una sola evaluación")
        void icfDeltaNullWithOneEval() {
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM_ID))
                    .thenReturn(List.of(finalizedEval(70.0, LocalDateTime.now().minusDays(3))));
            FamilyHealthSummaryResponse r = service.summarize(FAM_ID);
            assertThat(r.icfDelta30d()).isNull();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("summarize() — radar y riesgo")
    class RadarSection {

        @Test
        @DisplayName("evolutionPhase toma el valor del radar cuando hay >= 2 evaluaciones")
        void evolutionPhaseFromRadar() {
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM_ID))
                    .thenReturn(List.of(
                            finalizedEval(60.0, LocalDateTime.now().minusDays(30)),
                            finalizedEval(70.0, LocalDateTime.now().minusDays(5))
                    ));
            when(radarService.analyze(FAM_ID)).thenReturn(radarWith("consciente", "IMPROVING", 1));

            FamilyHealthSummaryResponse r = service.summarize(FAM_ID);
            assertThat(r.evolutionPhase()).isEqualTo("consciente");
            assertThat(r.highSignalCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("evolutionPhase es 'inconsciente' por defecto cuando el radar falla")
        void evolutionPhaseDefaultOnError() {
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM_ID))
                    .thenReturn(List.of(
                            finalizedEval(60.0, LocalDateTime.now().minusDays(30)),
                            finalizedEval(70.0, LocalDateTime.now().minusDays(5))
                    ));
            when(radarService.analyze(FAM_ID)).thenThrow(new RuntimeException("Radar no disponible"));

            FamilyHealthSummaryResponse r = service.summarize(FAM_ID);
            assertThat(r.evolutionPhase()).isEqualTo("inconsciente");
            assertThat(r.highSignalCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("riskLevel toma el valor del snapshot más reciente")
        void riskLevelFromSnapshot() {
            RiskSnapshot snap = new RiskSnapshot();
            snap.setRiskLevel("ALTO");
            when(riskSnapshotRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID))
                    .thenReturn(List.of(snap));

            FamilyHealthSummaryResponse r = service.summarize(FAM_ID);
            assertThat(r.riskLevel()).isEqualTo("ALTO");
        }

        @Test
        @DisplayName("riskLevel es 'BAJO' cuando no hay snapshots")
        void riskLevelDefaultBajo() {
            FamilyHealthSummaryResponse r = service.summarize(FAM_ID);
            assertThat(r.riskLevel()).isEqualTo("BAJO");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("summarize() — sprint y métricas")
    class SprintSection {

        @Test
        @DisplayName("hasActiveSprint es true cuando existe sprint con status ACTIVE")
        void hasActiveSprint() {
            FamilySprint sprint = new FamilySprint();
            sprint.setId(10L);
            sprint.setStatus("ACTIVE");
            when(sprintRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID))
                    .thenReturn(List.of(sprint));

            FamilyHealthSummaryResponse r = service.summarize(FAM_ID);
            assertThat(r.hasActiveSprint()).isTrue();
            assertThat(r.activeSprintId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("hasActiveSprint es false cuando todos los sprints están COMPLETED")
        void noActiveSprint() {
            FamilySprint sprint = new FamilySprint();
            sprint.setStatus("COMPLETED");
            when(sprintRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID))
                    .thenReturn(List.of(sprint));

            FamilyHealthSummaryResponse r = service.summarize(FAM_ID);
            assertThat(r.hasActiveSprint()).isFalse();
            assertThat(r.activeSprintId()).isNull();
        }

        @Test
        @DisplayName("sentinelActive refleja el campo de la familia")
        void sentinelActive() {
            family.setSentinelActive(true);
            FamilyHealthSummaryResponse r = service.summarize(FAM_ID);
            assertThat(r.sentinelActive()).isTrue();
        }

        @Test
        @DisplayName("métricas rápidas reflejan los conteos de repositorios")
        void quickMetrics() {
            when(memberRepository.countByFamilyId(FAM_ID)).thenReturn(4L);
            when(evidenceRepository.findByFamilyId(FAM_ID)).thenReturn(List.of(new TaskEvidence(), new TaskEvidence()));
            when(sprintRepository.countByFamilyId(FAM_ID)).thenReturn(3L);

            FamilyHealthSummaryResponse r = service.summarize(FAM_ID);
            assertThat(r.memberCount()).isEqualTo(4L);
            assertThat(r.evidenceCount()).isEqualTo(2L);
            assertThat(r.totalSprints()).isEqualTo(3L);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("summarize() — journey y metadata")
    class JourneySection {

        @Test
        @DisplayName("datos de journey provienen de FamilyJourneyService")
        void journeyDelegatedToJourneyService() {
            var journey = new com.integrityfamily.family.dto.FamilyJourneyResponse(
                    FAM_ID, "Familia Test", 5, 43, List.of(), "Inicia el sprint.", 8);
            when(journeyService.evaluate(FAM_ID)).thenReturn(journey);

            FamilyHealthSummaryResponse r = service.summarize(FAM_ID);
            assertThat(r.journeyCurrentLevel()).isEqualTo(5);
            assertThat(r.journeyProgress()).isEqualTo(43);
            assertThat(r.journeyNextAction()).isEqualTo("Inicia el sprint.");
        }

        @Test
        @DisplayName("generatedAt está presente y es reciente")
        void generatedAtIsPresent() {
            FamilyHealthSummaryResponse r = service.summarize(FAM_ID);
            assertThat(r.generatedAt()).isNotNull();
            assertThat(r.generatedAt()).isAfter(LocalDateTime.now().minusSeconds(5));
        }

        @Test
        @DisplayName("familyId y familyName se copian correctamente")
        void familyMetadata() {
            FamilyHealthSummaryResponse r = service.summarize(FAM_ID);
            assertThat(r.familyId()).isEqualTo(FAM_ID);
            assertThat(r.familyName()).isEqualTo("Familia Test");
        }
    }
}
