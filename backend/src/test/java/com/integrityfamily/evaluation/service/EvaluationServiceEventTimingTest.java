package com.integrityfamily.evaluation.service;

import com.integrityfamily.assessment.service.AssessmentAnswerService;
import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.EvaluationStatus;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.repository.EvaluationAnswerRepository;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.MemberRepository;
import com.integrityfamily.domain.repository.QuestionRepository;
import com.integrityfamily.dto.EvaluationDtos;
import com.integrityfamily.evaluation.service.post.CompletedEventStep;
import com.integrityfamily.evaluation.service.post.PostFinalizationPipeline;
import com.integrityfamily.risk.service.RiskAlgoV1Engine;
import com.integrityfamily.scanner.service.DeterministicExplanationPipeline;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * ADR-020, cortes A y B — criterios AC1 y AC2.
 *
 * Ejecuta finalize() con el pipeline REAL (solo {@link CompletedEventStep}) y simula la
 * transacción con TransactionSynchronizationManager: el evento "evaluation.completed" no
 * debe salir antes del commit y no debe salir nunca si la transacción termina en rollback.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EvaluationService — momento de publicación de evaluation.completed (ADR-020)")
class EvaluationServiceEventTimingTest {

    @Mock private EvaluationRepository       evaluationRepository;
    @Mock private EvaluationAnswerRepository evaluationAnswerRepository;
    @Mock private FamilyRepository           familyRepository;
    @Mock private MemberRepository           memberRepository;
    @Mock private QuestionRepository         questionRepository;
    @Mock private AssessmentAnswerService    assessmentAnswerService;
    @Mock private RiskAlgoV1Engine           riskAlgoV1Engine;
    @Mock private RabbitTemplate             rabbitTemplate;
    @Spy  private DeterministicExplanationPipeline explanationPipeline = new DeterministicExplanationPipeline();

    @InjectMocks
    private EvaluationService evaluationService;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();

        // Pipeline real con el único paso que publica el evento
        ReflectionTestUtils.setField(evaluationService, "postFinalizationPipeline",
                new PostFinalizationPipeline(List.of(new CompletedEventStep(rabbitTemplate))));

        Family family = Family.builder().id(1L).name("Familia Lopez").build();
        FamilyMember member = FamilyMember.builder().id(1L).fullName("Juan").role("PADRE").family(family).build();
        Evaluation evaluation = Evaluation.builder()
                .id(100L)
                .family(family)
                .member(member)
                .status(EvaluationStatus.STARTED)
                .startedAt(LocalDateTime.now())
                .answers(new ArrayList<>())
                .dimensionScores(new ArrayList<>())
                .build();

        Mockito.when(evaluationRepository.findById(100L)).thenReturn(Optional.of(evaluation));
        Mockito.when(evaluationRepository.save(any(Evaluation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(riskAlgoV1Engine.compute(any(), any())).thenReturn(
                new RiskAlgoV1Engine.AlgoResult(
                        Map.of("emociones", 50.0, "comunicacion", 50.0, "habitos", 50.0, "tiempos", 50.0),
                        50.0, "MODERADO", "comunicacion", false, false,
                        "COMUNICACION_CONSCIENTE", "Reactiva", 4, List.of(), List.of(),
                        new RiskAlgoV1Engine.UncertaintyVector(0.05, 0.20, 0.10, 0.05, 0.05, 0.09)));
    }

    @AfterEach
    void cleanUp() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private void finalizeEvaluation() {
        evaluationService.finalize(100L, new EvaluationDtos.EvaluationFinalizeRequest(
                new ArrayList<>(), 100.0, false, new HashMap<>()));
    }

    @Test
    @DisplayName("AC2 — antes del commit no se publica; tras el commit se publica exactamente una vez")
    void publishesOnlyAfterCommit() {
        finalizeEvaluation();

        assertThat(TransactionSynchronizationManager.getSynchronizations())
                .as("finalize() debe haber registrado la publicación para después del commit")
                .isNotEmpty();
        verify(rabbitTemplate, never()).convertAndSend(anyString(), eq("evaluation.completed"), any(Object.class));

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), eq("evaluation.completed"), any(Object.class));
    }

    @Test
    @DisplayName("AC1 — si la transacción hace rollback, el evento nunca se publica")
    void neverPublishes_whenTransactionRollsBack() {
        finalizeEvaluation();

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(rabbitTemplate, never()).convertAndSend(anyString(), eq("evaluation.completed"), any(Object.class));
    }

    @Test
    @DisplayName("Un fallo de RabbitMQ tras el commit no se propaga (resiliencia preservada)")
    void rabbitFailureAfterCommit_isNotPropagated() {
        Mockito.doThrow(new RuntimeException("broker caído"))
                .when(rabbitTemplate).convertAndSend(anyString(), eq("evaluation.completed"), any(Object.class));

        finalizeEvaluation();

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);   // no debe lanzar

        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), eq("evaluation.completed"), any(Object.class));
    }
}
