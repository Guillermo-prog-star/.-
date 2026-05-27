package com.integrityfamily.analytics.service;

import com.integrityfamily.analytics.domain.ProgressSnapshot;
import com.integrityfamily.analytics.dto.FamilyProgressResponse;
import com.integrityfamily.analytics.repository.ProgressSnapshotRepository;
import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.EvaluationStatus;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.EvaluationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link FamilyProgressAnalyticsService}.
 *
 * Documenta la clasificación de progreso (INICIAL / MEJORA_FUERTE / MEJORA_LEVE /
 * ESTANCAMIENTO / DETERIORO) según la variación del ICF entre evaluaciones.
 *
 * Rangos:
 *   deltaIcf ≥  10 → MEJORA_FUERTE
 *   deltaIcf ≥   3 → MEJORA_LEVE
 *   deltaIcf ≥  -2 → ESTANCAMIENTO
 *   deltaIcf  < -2 → DETERIORO
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FamilyProgressAnalyticsService — Unit Tests")
class FamilyProgressAnalyticsServiceTest {

    @Mock EvaluationRepository      evaluationRepository;
    @Mock ProgressSnapshotRepository progressSnapshotRepository;

    @InjectMocks FamilyProgressAnalyticsService service;

    private Family   family;
    private Evaluation current;

    @BeforeEach
    void setUp() {
        family = Family.builder().id(1L).name("Los García").build();
        current = Evaluation.builder()
                .id(10L)
                .family(family)
                .status(EvaluationStatus.FINALIZED)
                .milestoneKey("W1")
                .icf(75.0)
                .finalizedAt(LocalDateTime.now())
                .build();
    }

    // ───────────────────────────────────────────────────────────────────────
    //  analyzeProgress() — guardadas previas
    // ───────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("analyzeProgress() — validaciones previas")
    class Validations {

        @Test
        @DisplayName("evaluación no encontrada → BusinessException EVALUATION_NOT_FOUND 404")
        void evaluationNotFound() {
            when(evaluationRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.analyzeProgress(99L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo("EVALUATION_NOT_FOUND");
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("evaluación no FINALIZED → BusinessException EVALUATION_NOT_FINALIZED 400")
        void evaluationNotFinalized() {
            Evaluation inProgress = Evaluation.builder()
                    .id(20L).family(family)
                    .status(EvaluationStatus.IN_PROGRESS)
                    .icf(60.0).build();

            when(evaluationRepository.findById(20L)).thenReturn(Optional.of(inProgress));

            assertThatThrownBy(() -> service.analyzeProgress(20L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo("EVALUATION_NOT_FINALIZED");
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    });
        }
    }

    // ───────────────────────────────────────────────────────────────────────
    //  analyzeProgress() — primera evaluación
    // ───────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("analyzeProgress() — primera evaluación de la familia")
    class FirstEvaluation {

        @Test
        @DisplayName("solo 1 evaluación finalizada → classification = INICIAL")
        void firstEval_classificationIsInicial() {
            when(evaluationRepository.findById(10L)).thenReturn(Optional.of(current));
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(1L)).thenReturn(List.of(current));
            when(progressSnapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            FamilyProgressResponse result = service.analyzeProgress(10L);

            assertThat(result.classification()).isEqualTo("INICIAL");
            assertThat(result.currentIcf()).isEqualTo(75.0);
            assertThat(result.previousIcf()).isNull();
            assertThat(result.deltaIcf()).isNull();
            assertThat(result.familyId()).isEqualTo(1L);
            verify(progressSnapshotRepository).save(any(ProgressSnapshot.class));
        }
    }

    // ───────────────────────────────────────────────────────────────────────
    //  analyzeProgress() — clasificación por delta ICF
    // ───────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("analyzeProgress() — clasificación por deltaIcf")
    class Classification {

        @ParameterizedTest(name = "deltaIcf={0} → {1}")
        @CsvSource({
            "10.0, MEJORA_FUERTE",
            "15.0, MEJORA_FUERTE",
            " 3.0, MEJORA_LEVE",
            " 8.0, MEJORA_LEVE",
            " 0.0, ESTANCAMIENTO",
            "-2.0, ESTANCAMIENTO",
            "-3.0, DETERIORO",
            "-15.0, DETERIORO"
        })
        @DisplayName("clasificación correcta en todos los rangos y fronteras")
        void classificationByDelta(double delta, String expectedClassification) {
            double prevIcf = 65.0;
            double currIcf = prevIcf + delta;

            Evaluation previous = Evaluation.builder()
                    .id(5L).family(family)
                    .status(EvaluationStatus.FINALIZED)
                    .icf(prevIcf)
                    .finalizedAt(LocalDateTime.now().minusDays(30))
                    .build();

            Evaluation curr = Evaluation.builder()
                    .id(10L).family(family)
                    .status(EvaluationStatus.FINALIZED)
                    .icf(currIcf)
                    .milestoneKey("W1")
                    .finalizedAt(LocalDateTime.now())
                    .build();

            when(evaluationRepository.findById(10L)).thenReturn(Optional.of(curr));
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(1L))
                    .thenReturn(List.of(previous, curr));
            when(progressSnapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            FamilyProgressResponse result = service.analyzeProgress(10L);

            assertThat(result.classification()).isEqualTo(expectedClassification);
            assertThat(result.deltaIcf()).isEqualTo(currIcf - prevIcf);
            assertThat(result.previousIcf()).isEqualTo(prevIcf);
            assertThat(result.currentIcf()).isEqualTo(currIcf);
        }

        @Test
        @DisplayName("evaluación actual es la primera en la lista → no hay anterior → " +
                     "BusinessException EVALUATION_NO_PREVIOUS")
        void currentIsFirstInList_noPrevious() {
            // current está en la lista pero en posición 0 → no hay anterior
            when(evaluationRepository.findById(10L)).thenReturn(Optional.of(current));
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(1L))
                    .thenReturn(List.of(current, /* otra eval posterior con diferente ID */
                            Evaluation.builder().id(11L).family(family)
                                    .status(EvaluationStatus.FINALIZED).icf(80.0)
                                    .finalizedAt(LocalDateTime.now().plusDays(1)).build()));

            assertThatThrownBy(() -> service.analyzeProgress(10L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo("EVALUATION_NO_PREVIOUS");
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }
    }

    // ───────────────────────────────────────────────────────────────────────
    //  getLatestProgress()
    // ───────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getLatestProgress() — último snapshot de progreso")
    class GetLatestProgress {

        @Test
        @DisplayName("snapshot existe → retorna Optional con el DTO mapeado")
        void found_returnsMappedDto() {
            ProgressSnapshot snap = ProgressSnapshot.builder()
                    .id(1L).family(family)
                    .currentEvaluation(current)
                    .milestoneCode("W1")
                    .currentIcf(75.0).previousIcf(65.0).deltaIcf(10.0)
                    .classification("MEJORA_FUERTE")
                    .interpretation("Mejora consolidada")
                    .recommendedAction("Mantener plan")
                    .build();

            when(progressSnapshotRepository.findFirstByFamilyIdOrderByCreatedAtDesc(1L))
                    .thenReturn(Optional.of(snap));

            Optional<FamilyProgressResponse> result = service.getLatestProgress(1L);

            assertThat(result).isPresent();
            assertThat(result.get().classification()).isEqualTo("MEJORA_FUERTE");
            assertThat(result.get().currentIcf()).isEqualTo(75.0);
            assertThat(result.get().deltaIcf()).isEqualTo(10.0);
        }

        @Test
        @DisplayName("sin snapshots para la familia → retorna Optional vacío")
        void notFound_returnsEmpty() {
            when(progressSnapshotRepository.findFirstByFamilyIdOrderByCreatedAtDesc(1L))
                    .thenReturn(Optional.empty());

            assertThat(service.getLatestProgress(1L)).isEmpty();
        }
    }
}
