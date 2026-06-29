package com.integrityfamily.scanner.service;

import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.EvaluationDimensionScore;
import com.integrityfamily.domain.EvaluationStatus;
import com.integrityfamily.domain.FamilyLongitudinalState;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.repository.FamilyLongitudinalStateRepository;
import com.integrityfamily.domain.repository.RiskSnapshotRepository;
import com.integrityfamily.scanner.dto.SubtleSignalRadarResponse;
import com.integrityfamily.trajectory.service.TrajectorySuggestionService;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubtleSignalRadarService — Unit Tests")
class SubtleSignalRadarServiceTest {

    @Mock EvaluationRepository evaluationRepository;
    @Mock FamilyLongitudinalStateRepository ltsRepository;
    @Mock RiskSnapshotRepository riskSnapshotRepository;
    @Mock TrajectorySuggestionService suggestionService;

    @InjectMocks SubtleSignalRadarService service;

    private static final Long FAM = 1L;

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Evaluation evalWithIcf(double icf) {
        return Evaluation.builder()
                .status(EvaluationStatus.FINALIZED)
                .icf(icf)
                .build();
    }

    private Evaluation evalWithDims(double icf, double emoc, double comun, double habit, double tiempo) {
        EvaluationDimensionScore e = EvaluationDimensionScore.builder()
                .dimensionName("emociones").score(emoc).build();
        EvaluationDimensionScore c = EvaluationDimensionScore.builder()
                .dimensionName("comunicacion").score(comun).build();
        EvaluationDimensionScore h = EvaluationDimensionScore.builder()
                .dimensionName("habitos").score(habit).build();
        EvaluationDimensionScore t = EvaluationDimensionScore.builder()
                .dimensionName("tiempos").score(tiempo).build();
        return Evaluation.builder()
                .status(EvaluationStatus.FINALIZED)
                .icf(icf)
                .dimensionScores(List.of(e, c, h, t))
                .build();
    }

    private void stubNoLts() {
        when(ltsRepository.findByFamilyId(FAM)).thenReturn(Optional.empty());
    }

    private void stubSuggestions() {
        when(suggestionService.suggest(FAM)).thenReturn(List.of());
    }

    // ─── Sin evaluaciones ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("analyze() — sin evaluaciones")
    class SinEvaluaciones {

        @Test
        @DisplayName("devuelve radar vacío con evaluationsAnalyzed = 0")
        void sinEvals_retornaRadarVacio() {
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM))
                    .thenReturn(List.of());
            stubNoLts();

            SubtleSignalRadarResponse result = service.analyze(FAM);

            assertThat(result.evaluationsAnalyzed()).isZero();
            assertThat(result.microSignals()).isEmpty();
            assertThat(result.strengths()).isEmpty();
            assertThat(result.narrativeSummary()).isNotBlank();
            assertThat(result.emociones()).isNull();
        }

        @Test
        @DisplayName("filtra evaluaciones no finalizadas")
        void filtrarNoFinalizadas_noContribuyen() {
            Evaluation enCurso = Evaluation.builder()
                    .status(EvaluationStatus.IN_PROGRESS)
                    .icf(70.0)
                    .build();
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM))
                    .thenReturn(List.of(enCurso));
            stubNoLts();

            SubtleSignalRadarResponse result = service.analyze(FAM);

            assertThat(result.evaluationsAnalyzed()).isZero();
        }
    }

    // ─── Confianza ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("confidenceScore()")
    class Confianza {

        @Test
        @DisplayName("1 evaluación sin LTS → confianza baja (< 30)")
        void unaEvalSinLts_confianzaBaja() {
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM))
                    .thenReturn(List.of(evalWithIcf(65)));
            stubNoLts();
            stubSuggestions();

            int conf = service.analyze(FAM).confidenceScore();

            assertThat(conf).isLessThan(30);
        }

        @Test
        @DisplayName("5+ evaluaciones con dimensiones + LTS e icf30dAgo e icf90dAgo → confianza 100")
        void cincoEvalsConLtsCompleto_confianza100() {
            List<Evaluation> evals = List.of(
                    evalWithDims(55, 60, 55, 50, 55),
                    evalWithDims(58, 62, 57, 52, 57),
                    evalWithDims(60, 63, 59, 54, 59),
                    evalWithDims(62, 64, 61, 56, 61),
                    evalWithDims(65, 67, 64, 58, 63));
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM))
                    .thenReturn(evals);

            FamilyLongitudinalState lts = FamilyLongitudinalState.builder()
                    .icfCurrent(65.0).icf30dAgo(60.0).icf90dAgo(55.0).build();
            when(ltsRepository.findByFamilyId(FAM)).thenReturn(Optional.of(lts));
            stubSuggestions();

            int conf = service.analyze(FAM).confidenceScore();

            assertThat(conf).isEqualTo(100);
        }
    }

    // ─── Tendencias de dimensión ───────────────────────────────────────────────

    @Nested
    @DisplayName("Tendencias de dimensión")
    class Tendencias {

        @Test
        @DisplayName("comunicación bajó 10 pts → direction DECLINING")
        void comunicacionDecae_directionDeclining() {
            Evaluation e1 = evalWithDims(65, 70, 70, 65, 65);
            Evaluation e2 = evalWithDims(60, 68, 60, 64, 63);
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM))
                    .thenReturn(List.of(e1, e2));
            stubNoLts();
            stubSuggestions();

            SubtleSignalRadarResponse result = service.analyze(FAM);

            assertThat(result.comunicacion().direction()).isEqualTo("DECLINING");
            assertThat(result.comunicacion().delta()).isEqualTo(-10.0);
        }

        @Test
        @DisplayName("emociones subió 20 pts → direction STRONG_IMPROVING")
        void emocionesSube_directionStrongImproving() {
            Evaluation e1 = evalWithDims(60, 50, 60, 60, 60);
            Evaluation e2 = evalWithDims(70, 70, 62, 61, 61);
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM))
                    .thenReturn(List.of(e1, e2));
            stubNoLts();
            stubSuggestions();

            SubtleSignalRadarResponse result = service.analyze(FAM);

            assertThat(result.emociones().direction()).isEqualTo("STRONG_IMPROVING");
        }
    }

    // ─── Microseñales ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("detectMicroSignals()")
    class MicroSenales {

        @Test
        @DisplayName("comunicación con delta < -5 genera COMM_SUSTAINED_DECLINE")
        void caídaComunicación_generaCommSustainedDecline() {
            Evaluation e1 = evalWithDims(70, 70, 70, 65, 65);
            Evaluation e2 = evalWithDims(60, 68, 58, 64, 63);
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM))
                    .thenReturn(List.of(e1, e2));
            stubNoLts();
            stubSuggestions();

            SubtleSignalRadarResponse result = service.analyze(FAM);

            boolean hasSignal = result.microSignals().stream()
                    .anyMatch(s -> "COMM_SUSTAINED_DECLINE".equals(s.signalCode()));
            assertThat(hasSignal).isTrue();
        }

        @Test
        @DisplayName("dimensión emociones < 40 genera EMOCIONES_CRITICAL_LOW")
        void emocionesCriticamentebajas_generaCriticalLow() {
            Evaluation e1 = evalWithDims(50, 35, 55, 55, 55);
            Evaluation e2 = evalWithDims(52, 38, 56, 56, 56);
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM))
                    .thenReturn(List.of(e1, e2));
            stubNoLts();
            stubSuggestions();

            SubtleSignalRadarResponse result = service.analyze(FAM);

            boolean hasSignal = result.microSignals().stream()
                    .anyMatch(s -> "EMOCIONES_CRITICAL_LOW".equals(s.signalCode()));
            assertThat(hasSignal).isTrue();
        }

        @Test
        @DisplayName("LTS con communicationCollapseActive=true genera COMM_COLLAPSE_ACTIVE")
        void ltsCollapseActivo_generaCommCollapseActive() {
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM))
                    .thenReturn(List.of(evalWithIcf(60)));
            FamilyLongitudinalState lts = FamilyLongitudinalState.builder()
                    .communicationCollapseActive(true).build();
            when(ltsRepository.findByFamilyId(FAM)).thenReturn(Optional.of(lts));
            stubSuggestions();

            SubtleSignalRadarResponse result = service.analyze(FAM);

            boolean hasSignal = result.microSignals().stream()
                    .anyMatch(s -> "COMM_COLLAPSE_ACTIVE".equals(s.signalCode()));
            assertThat(hasSignal).isTrue();
        }

        @Test
        @DisplayName("LTS con consecutiveDeteriorations >= 3 genera LTS_SUSTAINED_DETERIORATION")
        void ltsTresDeterioraciones_generaSustainedDeterioration() {
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM))
                    .thenReturn(List.of(evalWithIcf(60)));
            FamilyLongitudinalState lts = FamilyLongitudinalState.builder()
                    .consecutiveDeteriorations(3).build();
            when(ltsRepository.findByFamilyId(FAM)).thenReturn(Optional.of(lts));
            stubSuggestions();

            SubtleSignalRadarResponse result = service.analyze(FAM);

            boolean hasSignal = result.microSignals().stream()
                    .anyMatch(s -> "LTS_SUSTAINED_DETERIORATION".equals(s.signalCode()));
            assertThat(hasSignal).isTrue();
        }

        @Test
        @DisplayName("sin condiciones de riesgo → microSignals vacío")
        void sinRiesgo_microSignalsVacio() {
            Evaluation e1 = evalWithDims(75, 75, 75, 75, 75);
            Evaluation e2 = evalWithDims(78, 77, 78, 76, 76);
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM))
                    .thenReturn(List.of(e1, e2));
            stubNoLts();
            stubSuggestions();

            SubtleSignalRadarResponse result = service.analyze(FAM);

            assertThat(result.microSignals()).isEmpty();
        }
    }

    // ─── Fortalezas invisibles ────────────────────────────────────────────────

    @Nested
    @DisplayName("detectStrengths()")
    class Fortalezas {

        @Test
        @DisplayName("emociones subió >= 15 pts → fortaleza 'emociones' detectada")
        void emocionesSaltoCualitativo_fortalezaDetectada() {
            Evaluation e1 = evalWithDims(60, 50, 65, 65, 65);
            Evaluation e2 = evalWithDims(70, 70, 66, 66, 66);
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM))
                    .thenReturn(List.of(e1, e2));
            stubNoLts();
            stubSuggestions();

            SubtleSignalRadarResponse result = service.analyze(FAM);

            boolean hasEmoc = result.strengths().stream()
                    .anyMatch(s -> "emociones".equals(s.dimension()));
            assertThat(hasEmoc).isTrue();
        }

        @Test
        @DisplayName("ICF creció >= 10 pts entre primera y última evaluación (>= 3 evals) → fortaleza general")
        void crecimientoIcfSostenido_fortalezaGeneral() {
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM))
                    .thenReturn(List.of(evalWithIcf(55), evalWithIcf(60), evalWithIcf(67)));
            stubNoLts();
            stubSuggestions();

            SubtleSignalRadarResponse result = service.analyze(FAM);

            boolean hasGeneral = result.strengths().stream()
                    .anyMatch(s -> "general".equals(s.dimension())
                            && s.description().contains("sostenida"));
            assertThat(hasGeneral).isTrue();
        }
    }

    // ─── ICF trend ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("icfOverall()")
    class IcfOverall {

        @Test
        @DisplayName("con LTS y icf30dAgo → delta30d calculado correctamente")
        void conLts_delta30dCalculado() {
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM))
                    .thenReturn(List.of(evalWithIcf(65)));
            FamilyLongitudinalState lts = FamilyLongitudinalState.builder()
                    .icfCurrent(65.0).icf30dAgo(58.0).build();
            when(ltsRepository.findByFamilyId(FAM)).thenReturn(Optional.of(lts));
            stubSuggestions();

            SubtleSignalRadarResponse result = service.analyze(FAM);

            assertThat(result.icfOverall()).isNotNull();
            assertThat(result.icfOverall().delta30d()).isEqualTo(7.0);
            assertThat(result.icfOverall().direction()).isEqualTo("IMPROVING");
        }

        @Test
        @DisplayName("sin LTS → delta30d es null y direction STABLE")
        void sinLts_delta30dNull() {
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM))
                    .thenReturn(List.of(evalWithIcf(65)));
            stubNoLts();
            stubSuggestions();

            SubtleSignalRadarResponse result = service.analyze(FAM);

            assertThat(result.icfOverall().delta30d()).isNull();
            assertThat(result.icfOverall().direction()).isEqualTo("STABLE");
        }
    }
}
