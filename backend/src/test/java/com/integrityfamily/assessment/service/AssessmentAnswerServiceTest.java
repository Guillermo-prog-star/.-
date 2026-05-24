package com.integrityfamily.assessment.service;

import com.integrityfamily.common.exception.NotFoundException;
import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.EvaluationAnswer;
import com.integrityfamily.domain.EvaluationStatus;
import com.integrityfamily.domain.Question;
import com.integrityfamily.domain.repository.EvaluationAnswerRepository;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.repository.QuestionRepository;
import com.integrityfamily.dto.EvaluationDtos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para {@link AssessmentAnswerService}.
 *
 * Cubre el flujo mobile-first de guardado incremental (upsert idempotente)
 * y verifica que el refactor batch no introduzca regresiones.
 */
@ExtendWith(MockitoExtension.class)
class AssessmentAnswerServiceTest {

    @Mock private EvaluationRepository       evaluationRepository;
    @Mock private EvaluationAnswerRepository  answerRepository;
    @Mock private QuestionRepository          questionRepository;

    @InjectMocks
    private AssessmentAnswerService service;

    // ─── Fixtures compartidos ─────────────────────────────────────────────────

    private Evaluation activeEval;
    private Question   q1, q2;

    @BeforeEach
    void setUp() {
        activeEval = Evaluation.builder()
                .id(10L)
                .status(EvaluationStatus.STARTED)
                .build();

        q1 = Question.builder()
                .id(1L)
                .questionKey("Q-EMO-001")
                .dimension("emociones")
                .build();

        q2 = Question.builder()
                .id(2L)
                .questionKey("Q-COM-001")
                .dimension("comunicacion")
                .build();
    }

    // ─── saveAnswers ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("INSERT: respuesta nueva se persiste con los campos correctos")
    void saveAnswers_newAnswer_insertsCorrectly() {
        // Arrange
        when(evaluationRepository.findById(10L)).thenReturn(Optional.of(activeEval));
        when(questionRepository.findAllById(anyList())).thenReturn(List.of(q1));
        when(answerRepository.findByEvaluationIdOrderByAnsweredAtAsc(10L)).thenReturn(List.of());
        when(answerRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(answerRepository.countByEvaluationId(10L)).thenReturn(1L);
        when(answerRepository.findByEvaluationIdOrderByAnsweredAtAsc(10L)).thenReturn(List.of());

        var request = List.of(new EvaluationDtos.SaveAnswerRequest(1L, 4, null));

        // Act
        EvaluationDtos.AnswerProgressResponse result = service.saveAnswers(10L, request);

        // Assert: saveAll fue llamado con exactamente 1 respuesta
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EvaluationAnswer>> captor = ArgumentCaptor.forClass(List.class);
        verify(answerRepository).saveAll(captor.capture());

        List<EvaluationAnswer> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getScore()).isEqualTo(4);
        assertThat(saved.get(0).getQuestionKey()).isEqualTo("Q-EMO-001");
        assertThat(saved.get(0).getQuestionId()).isEqualTo(1L);

        // Verificar que NO hubo N+1 (findAllById llamado una vez, no por pregunta)
        verify(questionRepository, times(1)).findAllById(anyList());
        assertThat(result.answered()).isEqualTo(1);
        assertThat(result.canFinalize()).isFalse();
    }

    @Test
    @DisplayName("UPSERT: respuesta ya existente se actualiza, no se inserta de nuevo")
    void saveAnswers_existingAnswer_updatesScore() {
        // Arrange
        EvaluationAnswer existing = EvaluationAnswer.builder()
                .id(99L)
                .questionKey("Q-EMO-001")
                .questionId(1L)
                .score(2)
                .build();

        when(evaluationRepository.findById(10L)).thenReturn(Optional.of(activeEval));
        when(questionRepository.findAllById(anyList())).thenReturn(List.of(q1));
        when(answerRepository.findByEvaluationIdOrderByAnsweredAtAsc(10L))
                .thenReturn(List.of(existing))
                .thenReturn(List.of(existing));
        when(answerRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(answerRepository.countByEvaluationId(10L)).thenReturn(1L);

        var request = List.of(new EvaluationDtos.SaveAnswerRequest(1L, 5, null));

        // Act
        service.saveAnswers(10L, request);

        // Assert: la respuesta existente (id=99) se actualizó a score=5
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EvaluationAnswer>> captor = ArgumentCaptor.forClass(List.class);
        verify(answerRepository).saveAll(captor.capture());

        List<EvaluationAnswer> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getId()).isEqualTo(99L);   // misma fila
        assertThat(saved.get(0).getScore()).isEqualTo(5);  // score actualizado

        // No se crearon filas nuevas
        verify(answerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Batch: múltiples respuestas → findAllById y saveAll son llamados exactamente una vez (no N veces)")
    void saveAnswers_batch_usesOnlySingleBatchCalls() {
        // Arrange
        when(evaluationRepository.findById(10L)).thenReturn(Optional.of(activeEval));
        when(questionRepository.findAllById(anyList())).thenReturn(List.of(q1, q2));
        // findByEvaluationIdOrderByAnsweredAtAsc se llama 2 veces:
        //   1) para cargar el mapa de upsert (saveAnswers)
        //   2) para construir la lista del response (buildProgress)
        when(answerRepository.findByEvaluationIdOrderByAnsweredAtAsc(10L)).thenReturn(List.of());
        when(answerRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(answerRepository.countByEvaluationId(10L)).thenReturn(2L);

        var requests = List.of(
                new EvaluationDtos.SaveAnswerRequest(1L, 4, null),
                new EvaluationDtos.SaveAnswerRequest(2L, 3, null)
        );

        // Act
        EvaluationDtos.AnswerProgressResponse result = service.saveAnswers(10L, requests);

        // ── Verificaciones del refactor batch ────────────────────────────────────
        // findAllById llamado UNA sola vez (no una vez por pregunta = N veces)
        verify(questionRepository, times(1)).findAllById(anyList());

        // saveAll llamado UNA sola vez con 2 entradas (no save() individual × N)
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EvaluationAnswer>> captor = ArgumentCaptor.forClass(List.class);
        verify(answerRepository, times(1)).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);

        // Ningún save() individual (el patrón N+1 que se eliminó)
        verify(answerRepository, never()).save(any());

        assertThat(result.answered()).isEqualTo(2);
    }

    @Test
    @DisplayName("QuestionId desconocido se omite sin interrumpir el batch")
    void saveAnswers_unknownQuestionId_isSkippedGracefully() {
        // q2 no está en el mapa → debe ignorarse, q1 sigue procesándose
        when(evaluationRepository.findById(10L)).thenReturn(Optional.of(activeEval));
        when(questionRepository.findAllById(anyList())).thenReturn(List.of(q1)); // solo devuelve q1
        when(answerRepository.findByEvaluationIdOrderByAnsweredAtAsc(10L)).thenReturn(List.of());
        when(answerRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(answerRepository.countByEvaluationId(10L)).thenReturn(1L);

        var requests = List.of(
                new EvaluationDtos.SaveAnswerRequest(1L, 4, null),
                new EvaluationDtos.SaveAnswerRequest(999L, 5, null)  // ID inexistente
        );

        // Act
        service.saveAnswers(10L, requests);

        // Assert: solo 1 respuesta guardada (la de q1)
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EvaluationAnswer>> captor = ArgumentCaptor.forClass(List.class);
        verify(answerRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getQuestionId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Respuesta booleana: true → score=5, false → score=1")
    void saveAnswers_booleanAnswer_mapsToScore() {
        when(evaluationRepository.findById(10L)).thenReturn(Optional.of(activeEval));
        when(questionRepository.findAllById(anyList())).thenReturn(List.of(q1, q2));
        when(answerRepository.findByEvaluationIdOrderByAnsweredAtAsc(10L)).thenReturn(List.of());
        when(answerRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(answerRepository.countByEvaluationId(10L)).thenReturn(2L);

        var requests = List.of(
                new EvaluationDtos.SaveAnswerRequest(1L, null, true),   // true  → 5
                new EvaluationDtos.SaveAnswerRequest(2L, null, false)   // false → 1
        );

        service.saveAnswers(10L, requests);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EvaluationAnswer>> captor = ArgumentCaptor.forClass(List.class);
        verify(answerRepository).saveAll(captor.capture());

        List<EvaluationAnswer> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        int scoreForQ1 = saved.stream().filter(a -> "Q-EMO-001".equals(a.getQuestionKey()))
                .findFirst().map(EvaluationAnswer::getScore).orElse(-1);
        int scoreForQ2 = saved.stream().filter(a -> "Q-COM-001".equals(a.getQuestionKey()))
                .findFirst().map(EvaluationAnswer::getScore).orElse(-1);

        assertThat(scoreForQ1).isEqualTo(5);
        assertThat(scoreForQ2).isEqualTo(1);
    }

    @Test
    @DisplayName("Evaluación FINALIZED lanza IllegalStateException")
    void saveAnswers_finalizedEval_throwsException() {
        Evaluation finalized = Evaluation.builder()
                .id(10L)
                .status(EvaluationStatus.FINALIZED)
                .build();
        when(evaluationRepository.findById(10L)).thenReturn(Optional.of(finalized));

        var request = List.of(new EvaluationDtos.SaveAnswerRequest(1L, 4, null));

        assertThatThrownBy(() -> service.saveAnswers(10L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya fue finalizada");
    }

    @Test
    @DisplayName("Evaluación inexistente lanza NotFoundException")
    void saveAnswers_evalNotFound_throwsNotFoundException() {
        when(evaluationRepository.findById(99L)).thenReturn(Optional.empty());

        var request = List.of(new EvaluationDtos.SaveAnswerRequest(1L, 4, null));

        assertThatThrownBy(() -> service.saveAnswers(99L, request))
                .isInstanceOf(NotFoundException.class);
    }

    // ─── getProgress ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getProgress retorna canFinalize=false con menos de 10 respuestas")
    void getProgress_lessThan10_canFinalizeFalse() {
        when(evaluationRepository.findById(10L)).thenReturn(Optional.of(activeEval));
        when(answerRepository.countByEvaluationId(10L)).thenReturn(5L);
        when(answerRepository.findByEvaluationIdOrderByAnsweredAtAsc(10L)).thenReturn(List.of());

        var result = service.getProgress(10L);

        assertThat(result.answered()).isEqualTo(5);
        assertThat(result.canFinalize()).isFalse();
    }

    @Test
    @DisplayName("getProgress retorna canFinalize=true con 10+ respuestas")
    void getProgress_10orMore_canFinalizeTrue() {
        when(evaluationRepository.findById(10L)).thenReturn(Optional.of(activeEval));
        when(answerRepository.countByEvaluationId(10L)).thenReturn(10L);
        when(answerRepository.findByEvaluationIdOrderByAnsweredAtAsc(10L)).thenReturn(List.of());

        var result = service.getProgress(10L);

        assertThat(result.answered()).isEqualTo(10);
        assertThat(result.canFinalize()).isTrue();
        assertThat(result.totalExpected()).isEqualTo(20);
    }

    // ─── loadAnswersAsDto ─────────────────────────────────────────────────────

    @Test
    @DisplayName("loadAnswersAsDto omite respuestas sin questionId")
    void loadAnswersAsDto_filtersNullQuestionId() {
        EvaluationAnswer withId = EvaluationAnswer.builder()
                .id(1L).questionId(10L).questionKey("Q-EMO-001").score(4).build();
        EvaluationAnswer withoutId = EvaluationAnswer.builder()
                .id(2L).questionId(null).questionKey("Q-OLD-KEY").score(3).build();

        when(answerRepository.findByEvaluationIdOrderByAnsweredAtAsc(10L))
                .thenReturn(List.of(withId, withoutId));

        List<EvaluationDtos.AnswerDto> result = service.loadAnswersAsDto(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).questionId()).isEqualTo(10L);
        assertThat(result.get(0).value()).isEqualTo(4);
    }
}
