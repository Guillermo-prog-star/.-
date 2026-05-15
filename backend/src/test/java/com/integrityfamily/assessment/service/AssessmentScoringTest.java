package com.integrityfamily.assessment.service;

import com.integrityfamily.ai.service.AiService;
import com.integrityfamily.domain.*;
import com.integrityfamily.dto.EvaluationDtos;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.MemberRepository;
import com.integrityfamily.domain.repository.QuestionRepository;
import com.integrityfamily.evaluation.service.EvaluationService;
import com.integrityfamily.risk.service.RiskService;
import com.integrityfamily.milestone.service.MilestoneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class AssessmentScoringTest {

    @Mock
    private EvaluationRepository evaluationRepository;

    @Mock
    private FamilyRepository familyRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private RiskService riskService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private MilestoneService milestoneService;

    @Mock
    private AiService aiService;

    @InjectMocks
    private EvaluationService evaluationService;

    private Family family;
    private Evaluation evaluation;

    @BeforeEach
    void setUp() {
        family = Family.builder().id(1L).name("Familia Lopez").build();
        evaluation = Evaluation.builder()
                .id(100L)
                .family(family)
                .status(EvaluationStatus.STARTED)
                .startedAt(LocalDateTime.now())
                .answers(new ArrayList<>())
                .dimensionScores(new ArrayList<>())
                .build();

        Mockito.lenient().when(evaluationRepository.findById(100L)).thenReturn(Optional.of(evaluation));
        Mockito.lenient().when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Caso 1: Todo Excelente (100) -> Riesgo BAJO")
    void shouldScoreAllExcellent() {
        Question q1 = Question.builder().id(1L).dimension("emociones").direction("POSITIVE").build();
        Question q2 = Question.builder().id(2L).dimension("comunicacion").direction("POSITIVE").build();
        Question q3 = Question.builder().id(3L).dimension("habitos").direction("POSITIVE").build();
        Question q4 = Question.builder().id(4L).dimension("tiempos").direction("POSITIVE").build();

        Mockito.when(questionRepository.findById(1L)).thenReturn(Optional.of(q1));
        Mockito.when(questionRepository.findById(2L)).thenReturn(Optional.of(q2));
        Mockito.when(questionRepository.findById(3L)).thenReturn(Optional.of(q3));
        Mockito.when(questionRepository.findById(4L)).thenReturn(Optional.of(q4));

        List<EvaluationDtos.AnswerDto> answers = Arrays.asList(
                new EvaluationDtos.AnswerDto(1L, 5, null),
                new EvaluationDtos.AnswerDto(2L, 5, null),
                new EvaluationDtos.AnswerDto(3L, 5, null),
                new EvaluationDtos.AnswerDto(4L, 5, null)
        );

        Evaluation finalized = evaluationService.finalize(100L, new EvaluationDtos.EvaluationFinalizeRequest(answers, null, null, null));

        assertEquals(100.0, finalized.getIcf(), 0.01);
        assertEquals("BAJO", finalized.getRiskLevel());
    }

    @Test
    @DisplayName("Caso 2: Todo Crítico (1, positivas) -> Riesgo CRITICO")
    void shouldScoreAllCritical() {
        Question q1 = Question.builder().id(1L).dimension("emociones").direction("POSITIVE").build();
        Question q2 = Question.builder().id(2L).dimension("comunicacion").direction("POSITIVE").build();
        Question q3 = Question.builder().id(3L).dimension("habitos").direction("POSITIVE").build();
        Question q4 = Question.builder().id(4L).dimension("tiempos").direction("POSITIVE").build();

        Mockito.when(questionRepository.findById(1L)).thenReturn(Optional.of(q1));
        Mockito.when(questionRepository.findById(2L)).thenReturn(Optional.of(q2));
        Mockito.when(questionRepository.findById(3L)).thenReturn(Optional.of(q3));
        Mockito.when(questionRepository.findById(4L)).thenReturn(Optional.of(q4));

        List<EvaluationDtos.AnswerDto> answers = Arrays.asList(
                new EvaluationDtos.AnswerDto(1L, 1, null),
                new EvaluationDtos.AnswerDto(2L, 1, null),
                new EvaluationDtos.AnswerDto(3L, 1, null),
                new EvaluationDtos.AnswerDto(4L, 1, null)
        );

        Evaluation finalized = evaluationService.finalize(100L, new EvaluationDtos.EvaluationFinalizeRequest(answers, null, null, null));

        assertEquals(0.0, finalized.getIcf(), 0.01);
        assertEquals("CRITICO", finalized.getRiskLevel());
        assertTrue(finalized.getHasCrisis());
    }

    @Test
    @DisplayName("Caso 3: Dimensión crítica oculta (Emociones 1, el resto 5) -> Dispara Regla de Seguridad y clasifica CRITICO")
    void shouldTriggerCriticalSecurityRule() {
        Question q1 = Question.builder().id(1L).dimension("emociones").direction("POSITIVE").build();
        Question q2 = Question.builder().id(2L).dimension("comunicacion").direction("POSITIVE").build();
        Question q3 = Question.builder().id(3L).dimension("habitos").direction("POSITIVE").build();
        Question q4 = Question.builder().id(4L).dimension("tiempos").direction("POSITIVE").build();

        Mockito.when(questionRepository.findById(1L)).thenReturn(Optional.of(q1));
        Mockito.when(questionRepository.findById(2L)).thenReturn(Optional.of(q2));
        Mockito.when(questionRepository.findById(3L)).thenReturn(Optional.of(q3));
        Mockito.when(questionRepository.findById(4L)).thenReturn(Optional.of(q4));

        // Emociones = 1 (score = 0.0), el resto = 5 (score = 100.0)
        // healthyIndex = (0*0.3) + (100*0.3) + (100*0.2) + (100*0.2) = 70.0 (Modo normal sería MODERADO)
        // Pero al tener una dimensión en 0 (< 25), dispara CRITICO.
        List<EvaluationDtos.AnswerDto> answers = Arrays.asList(
                new EvaluationDtos.AnswerDto(1L, 1, null),
                new EvaluationDtos.AnswerDto(2L, 5, null),
                new EvaluationDtos.AnswerDto(3L, 5, null),
                new EvaluationDtos.AnswerDto(4L, 5, null)
        );

        Evaluation finalized = evaluationService.finalize(100L, new EvaluationDtos.EvaluationFinalizeRequest(answers, null, null, null));

        assertEquals(70.0, finalized.getIcf(), 0.01);
        assertEquals("CRITICO", finalized.getRiskLevel());
        assertEquals("emociones", finalized.getCriticalDimension());
    }

    @Test
    @DisplayName("Caso 4: Riesgo Moderado con preguntas negativas normalizadas")
    void shouldScoreModerateWithNegativeQuestions() {
        Question q1 = Question.builder().id(1L).dimension("emociones").direction("NEGATIVE").build(); // Respuesta 2 -> (5-2)/4 = 75%
        Question q2 = Question.builder().id(2L).dimension("comunicacion").direction("POSITIVE").build(); // Respuesta 3 -> (3-1)/4 = 50%
        Question q3 = Question.builder().id(3L).dimension("habitos").direction("POSITIVE").build(); // Respuesta 4 -> (4-1)/4 = 75%
        Question q4 = Question.builder().id(4L).dimension("tiempos").direction("POSITIVE").build(); // Respuesta 3 -> (3-1)/4 = 50%

        Mockito.when(questionRepository.findById(1L)).thenReturn(Optional.of(q1));
        Mockito.when(questionRepository.findById(2L)).thenReturn(Optional.of(q2));
        Mockito.when(questionRepository.findById(3L)).thenReturn(Optional.of(q3));
        Mockito.when(questionRepository.findById(4L)).thenReturn(Optional.of(q4));

        // healthyIndex = (75*0.3) + (50*0.3) + (75*0.2) + (50*0.2) = 22.5 + 15 + 15 + 10 = 62.5 -> MODERADO
        List<EvaluationDtos.AnswerDto> answers = Arrays.asList(
                new EvaluationDtos.AnswerDto(1L, 2, null),
                new EvaluationDtos.AnswerDto(2L, 3, null),
                new EvaluationDtos.AnswerDto(3L, 4, null),
                new EvaluationDtos.AnswerDto(4L, 3, null)
        );

        Evaluation finalized = evaluationService.finalize(100L, new EvaluationDtos.EvaluationFinalizeRequest(answers, null, null, null));

        assertEquals(62.5, finalized.getIcf(), 0.01);
        assertEquals("MODERADO", finalized.getRiskLevel());
        assertEquals("comunicacion", finalized.getCriticalDimension()); // empate 50, toma el primero
    }
}
