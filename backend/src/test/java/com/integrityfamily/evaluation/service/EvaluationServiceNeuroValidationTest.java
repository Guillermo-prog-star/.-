package com.integrityfamily.evaluation.service;

import com.integrityfamily.assessment.service.AssessmentAnswerService;
import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.Question;
import com.integrityfamily.domain.repository.EvaluationAnswerRepository;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.repository.QuestionRepository;
import com.integrityfamily.dto.EvaluationDtos;
import com.integrityfamily.risk.service.RiskAlgoV1Engine;
import com.integrityfamily.scanner.service.DeterministicExplanationPipeline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class EvaluationServiceNeuroValidationTest {

    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private EvaluationAnswerRepository evaluationAnswerRepository;
    @Mock
    private RiskAlgoV1Engine riskAlgoV1Engine;
    @Mock
    private AssessmentAnswerService assessmentAnswerService;
    @Mock
    private EvaluationRepository evaluationRepository;
    @Mock
    private DeterministicExplanationPipeline explanationPipeline;

    @InjectMocks
    private EvaluationService evaluationService;

    @BeforeEach
    void setUp() {
        Evaluation ev = new Evaluation();
        ev.setId(1L);
        Family f = new Family();
        f.setCurrentMilestone("MILESTONE_1");
        ev.setFamily(f);

        lenient().when(evaluationRepository.findById(1L)).thenReturn(Optional.of(ev));
        // finalize() persiste vía save() antes de leer saved.getFamily()/getId() en
        // processPostFinalization() — sin este stub, saved queda null y NPEa fuera
        // de los bloques try/catch "no bloqueante" que protegen el resto del método.
        lenient().when(evaluationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void coreQuestionWithZero_ShouldThrowException() {
        Question q = new Question();
        q.setId(1L);
        q.setType("CORE");
        q.setPhase(null);
        q.setQuestionKey("Q-CORE-1");

        when(questionRepository.findAllById(anyList())).thenReturn(List.of(q));
        when(evaluationAnswerRepository.countByEvaluationId(1L)).thenReturn(0L);

        EvaluationDtos.EvaluationFinalizeRequest request = new EvaluationDtos.EvaluationFinalizeRequest(
                List.of(new EvaluationDtos.AnswerDto(1L, 0, null)),
                null, null, null
        );

        assertThatThrownBy(() -> evaluationService.finalize(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El valor 0 solo está permitido");
    }

    @Test
    void neuroAwarenessTimingWithZero_ShouldThrowException() {
        Question q = new Question();
        q.setId(2L);
        q.setType("NEURO_AWARENESS");
        q.setPhase("TIMING");
        q.setQuestionKey("Q-NEURO-TIMING");

        when(questionRepository.findAllById(anyList())).thenReturn(List.of(q));
        when(evaluationAnswerRepository.countByEvaluationId(1L)).thenReturn(0L);

        EvaluationDtos.EvaluationFinalizeRequest request = new EvaluationDtos.EvaluationFinalizeRequest(
                List.of(new EvaluationDtos.AnswerDto(2L, 0, null)),
                null, null, null
        );

        assertThatThrownBy(() -> evaluationService.finalize(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El valor 0 solo está permitido");
    }

    @Test
    void neuroAwarenessActionWithZero_ShouldThrowException() {
        Question q = new Question();
        q.setId(3L);
        q.setType("NEURO_AWARENESS");
        q.setPhase("ACTION");
        q.setQuestionKey("Q-NEURO-ACTION");

        when(questionRepository.findAllById(anyList())).thenReturn(List.of(q));
        when(evaluationAnswerRepository.countByEvaluationId(1L)).thenReturn(0L);

        EvaluationDtos.EvaluationFinalizeRequest request = new EvaluationDtos.EvaluationFinalizeRequest(
                List.of(new EvaluationDtos.AnswerDto(3L, 0, null)),
                null, null, null
        );

        assertThatThrownBy(() -> evaluationService.finalize(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El valor 0 solo está permitido");
    }

    @Test
    void neuroAwarenessEntryWithZero_ShouldAccept() {
        Question q = new Question();
        q.setId(4L);
        q.setType("NEURO_AWARENESS");
        q.setPhase("ENTRY");
        q.setQuestionKey("Q-NEURO-ENTRY");

        when(questionRepository.findAllById(anyList())).thenReturn(List.of(q));
        when(evaluationAnswerRepository.countByEvaluationId(1L)).thenReturn(0L);
        when(riskAlgoV1Engine.compute(anyList(), any())).thenReturn(
                new RiskAlgoV1Engine.AlgoResult(Map.of("emociones", 100.0), 100.0, 0.0, null, "BAJO", "emociones", false, false, "ESTABLE", "Agencia", 5, List.of(), List.of(), null)
        );

        EvaluationDtos.EvaluationFinalizeRequest request = new EvaluationDtos.EvaluationFinalizeRequest(
                List.of(new EvaluationDtos.AnswerDto(4L, 0, null)),
                null, null, null
        );

        EvaluationDtos.FinalizeResult result = evaluationService.finalize(1L, request);
        
        assertThat(result).isNotNull();
    }
}
