package com.integrityfamily.assessment.service;

import com.integrityfamily.domain.Question;
import com.integrityfamily.domain.repository.QuestionRepository;
import com.integrityfamily.dto.EvaluationDtos;
import com.integrityfamily.risk.service.RiskAlgoV1Engine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

/**
 * Tests del motor RISK_ALGO_V1.
 *
 * Se testea {@link RiskAlgoV1Engine#compute} directamente, no a través de
 * {@code EvaluationService.finalize()}, para aislar el álgebra de scoring
 * de los efectos secundarios del servicio de evaluación.
 */
@ExtendWith(MockitoExtension.class)
public class AssessmentScoringTest {

    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private RiskAlgoV1Engine riskAlgoV1Engine;

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /** Configura el mock del repositorio para devolver las preguntas indicadas. */
    private void mockQuestions(Question... questions) {
        Mockito.when(questionRepository.findAllById(any())).thenReturn(List.of(questions));
    }

    // ─── Casos ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Caso 1: Todo Excelente (valor 5, POSITIVE) -> ICF=100, Riesgo BAJO")
    void shouldScoreAllExcellent() {
        Question q1 = Question.builder().id(1L).dimension("emociones").direction("POSITIVE").build();
        Question q2 = Question.builder().id(2L).dimension("comunicacion").direction("POSITIVE").build();
        Question q3 = Question.builder().id(3L).dimension("habitos").direction("POSITIVE").build();
        Question q4 = Question.builder().id(4L).dimension("tiempos").direction("POSITIVE").build();
        mockQuestions(q1, q2, q3, q4);

        List<EvaluationDtos.AnswerDto> answers = Arrays.asList(
                new EvaluationDtos.AnswerDto(1L, 5, null),
                new EvaluationDtos.AnswerDto(2L, 5, null),
                new EvaluationDtos.AnswerDto(3L, 5, null),
                new EvaluationDtos.AnswerDto(4L, 5, null)
        );

        RiskAlgoV1Engine.AlgoResult result = riskAlgoV1Engine.compute(answers, "W1");

        assertEquals(100.0, result.healthyIndex(), 0.01);
        assertEquals("BAJO", result.riskLevel());
    }

    @Test
    @DisplayName("Caso 2: Todo Crítico (valor 1, POSITIVE) -> ICF=0, Riesgo CRITICO")
    void shouldScoreAllCritical() {
        Question q1 = Question.builder().id(1L).dimension("emociones").direction("POSITIVE").build();
        Question q2 = Question.builder().id(2L).dimension("comunicacion").direction("POSITIVE").build();
        Question q3 = Question.builder().id(3L).dimension("habitos").direction("POSITIVE").build();
        Question q4 = Question.builder().id(4L).dimension("tiempos").direction("POSITIVE").build();
        mockQuestions(q1, q2, q3, q4);

        List<EvaluationDtos.AnswerDto> answers = Arrays.asList(
                new EvaluationDtos.AnswerDto(1L, 1, null),
                new EvaluationDtos.AnswerDto(2L, 1, null),
                new EvaluationDtos.AnswerDto(3L, 1, null),
                new EvaluationDtos.AnswerDto(4L, 1, null)
        );

        RiskAlgoV1Engine.AlgoResult result = riskAlgoV1Engine.compute(answers, "W1");

        assertEquals(0.0, result.healthyIndex(), 0.01);
        assertEquals("CRITICO", result.riskLevel());
        assertTrue(result.hasCrisis());
    }

    @Test
    @DisplayName("Caso 3: Dimensión crítica oculta (Emociones=1, resto=5) -> Regla de seguridad -> CRITICO")
    void shouldTriggerCriticalSecurityRule() {
        Question q1 = Question.builder().id(1L).dimension("emociones").direction("POSITIVE").build();
        Question q2 = Question.builder().id(2L).dimension("comunicacion").direction("POSITIVE").build();
        Question q3 = Question.builder().id(3L).dimension("habitos").direction("POSITIVE").build();
        Question q4 = Question.builder().id(4L).dimension("tiempos").direction("POSITIVE").build();
        mockQuestions(q1, q2, q3, q4);

        // emociones=0%, resto=100%.  ICF = 0×0.3 + 100×0.3 + 100×0.2 + 100×0.2 = 70
        // Pero emociones < 25 → regla de seguridad → CRITICO
        List<EvaluationDtos.AnswerDto> answers = Arrays.asList(
                new EvaluationDtos.AnswerDto(1L, 1, null),
                new EvaluationDtos.AnswerDto(2L, 5, null),
                new EvaluationDtos.AnswerDto(3L, 5, null),
                new EvaluationDtos.AnswerDto(4L, 5, null)
        );

        RiskAlgoV1Engine.AlgoResult result = riskAlgoV1Engine.compute(answers, "W1");

        assertEquals(70.0, result.healthyIndex(), 0.01);
        assertEquals("CRITICO", result.riskLevel());
        assertEquals("emociones", result.criticalDimension());
    }

    @Test
    @DisplayName("Caso 4: Riesgo Moderado con preguntas de dirección NEGATIVE (escala invertida)")
    void shouldScoreModerateWithNegativeQuestions() {
        // q1 NEGATIVE: valor 2 -> (5-2)/4 * 100 = 75.0
        // q2 POSITIVE: valor 3 -> (3-1)/4 * 100 = 50.0
        // q3 POSITIVE: valor 4 -> (4-1)/4 * 100 = 75.0
        // q4 POSITIVE: valor 3 -> (3-1)/4 * 100 = 50.0
        // ICF = 75×0.3 + 50×0.3 + 75×0.2 + 50×0.2 = 22.5 + 15 + 15 + 10 = 62.5 -> MODERADO (W1: ≥40)
        Question q1 = Question.builder().id(1L).dimension("emociones").direction("NEGATIVE").build();
        Question q2 = Question.builder().id(2L).dimension("comunicacion").direction("POSITIVE").build();
        Question q3 = Question.builder().id(3L).dimension("habitos").direction("POSITIVE").build();
        Question q4 = Question.builder().id(4L).dimension("tiempos").direction("POSITIVE").build();
        mockQuestions(q1, q2, q3, q4);

        List<EvaluationDtos.AnswerDto> answers = Arrays.asList(
                new EvaluationDtos.AnswerDto(1L, 2, null),
                new EvaluationDtos.AnswerDto(2L, 3, null),
                new EvaluationDtos.AnswerDto(3L, 4, null),
                new EvaluationDtos.AnswerDto(4L, 3, null)
        );

        RiskAlgoV1Engine.AlgoResult result = riskAlgoV1Engine.compute(answers, "W1");

        assertEquals(62.5, result.healthyIndex(), 0.01);
        assertEquals("MODERADO", result.riskLevel());
        // empate comunicacion(50) y tiempos(50); stream itera DIMENSIONS en orden → comunicacion primero
        assertEquals("comunicacion", result.criticalDimension());
    }
}
