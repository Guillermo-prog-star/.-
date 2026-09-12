package com.integrityfamily.evaluation.service;

import com.integrityfamily.ai.service.AiService;
import com.integrityfamily.analytics.service.FamilyProgressAnalyticsService;
import com.integrityfamily.assessment.service.AssessmentAnswerService;
import com.integrityfamily.cognitive.service.*;
import com.integrityfamily.common.service.UserNotificationService;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import com.integrityfamily.dto.EvaluationDtos;
import com.integrityfamily.milestone.service.MilestoneService;
import com.integrityfamily.plan.service.PlanGenerationService;
import com.integrityfamily.plan.service.PlanTaskService;
import com.integrityfamily.risk.service.LongitudinalStateService;
import com.integrityfamily.risk.service.RiskAlgoV1Engine;
import com.integrityfamily.risk.service.RiskService;
import com.integrityfamily.scanner.repository.InferenceRecordRepository;
import com.integrityfamily.scanner.service.AlertEngine;
import com.integrityfamily.scanner.service.DeterministicExplanationPipeline;
import com.integrityfamily.scanner.service.InferenceRecordService;
import com.integrityfamily.scanner.service.RuleExecutionEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de EvaluationService.
 *
 * Cubre: findAll(), findByFamilyId(), findById() (existente y not-found),
 * create(), start() (familia no encontrada, sin memberId, con memberId,
 * member no encontrado → null asignado).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EvaluationService — Unit Tests")
class EvaluationServiceTest {

    // ── Repositories ──────────────────────────────────────────────────
    @Mock EvaluationRepository            evaluationRepository;
    @Mock EvaluationAnswerRepository      evaluationAnswerRepository;
    @Mock FamilyRepository                familyRepository;
    @Mock MemberRepository                memberRepository;
    @Mock QuestionRepository              questionRepository;

    // ── Services ──────────────────────────────────────────────────────
    @Mock AssessmentAnswerService         assessmentAnswerService;
    @Mock RiskAlgoV1Engine                riskAlgoV1Engine;
    @Mock RiskService                     riskService;
    @Mock RabbitTemplate                  rabbitTemplate;
    @Mock MilestoneService                milestoneService;
    @Mock AiService                       aiService;
    @Mock PlanTaskService                 planTaskService;
    @Mock PlanGenerationService           planGenerationService;
    @Mock FamilyProgressAnalyticsService  familyProgressAnalyticsService;
    @Mock FamilyMemoryService             familyMemoryService;
    @Mock FamilySkillEngine               familySkillEngine;
    @Mock FamilyReflectionService         familyReflectionService;
    @Mock NarrativeEvolutionEngine        narrativeEvolutionEngine;
    @Mock FamilyIdentityGraphService      familyIdentityGraphService;
    @Mock DeterministicExplanationPipeline explanationPipeline;
    @Mock InferenceRecordService          inferenceRecordService;
    @Mock InferenceRecordRepository       inferenceRecordRepository;
    @Mock RuleExecutionEngine             ruleExecutionEngine;
    @Mock AlertEngine                     alertEngine;
    @Mock UserNotificationService         userNotificationService;
    @Mock LongitudinalStateService        longitudinalStateService;
    @Mock HypothesisEvidenceRepository    hypothesisEvidenceRepository;

    @InjectMocks
    EvaluationService evaluationService;

    private Family family;

    @BeforeEach
    void setUp() {
        family = Family.builder()
                .id(1L)
                .name("Familia López")
                .familyCode("IF-2026-TEST")
                .members(new ArrayList<>())
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  findAll()
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("Delega en el repositorio y retorna su lista")
        void shouldReturnAllEvaluations() {
            Evaluation e1 = Evaluation.builder().id(1L).family(family).build();
            Evaluation e2 = Evaluation.builder().id(2L).family(family).build();
            when(evaluationRepository.findAll()).thenReturn(List.of(e1, e2));

            List<Evaluation> result = evaluationService.findAll();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getId()).isEqualTo(1L);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  findByFamilyId()
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findByFamilyId()")
    class FindByFamilyId {

        @Test
        @DisplayName("Filtra por familyId y retorna la lista del repositorio")
        void shouldReturnEvaluationsForFamily() {
            Evaluation e = Evaluation.builder().id(5L).family(family).build();
            when(evaluationRepository.findByFamilyId(1L)).thenReturn(List.of(e));

            List<Evaluation> result = evaluationService.findByFamilyId(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(5L);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  findById()
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("ID existente → retorna la entidad")
        void shouldReturnEvaluation_whenExists() {
            Evaluation e = Evaluation.builder().id(10L).family(family).build();
            when(evaluationRepository.findById(10L)).thenReturn(Optional.of(e));

            assertThat(evaluationService.findById(10L).getId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("ID inexistente → NoSuchElementException (orElseThrow sin mensaje)")
        void shouldThrow_whenNotFound() {
            when(evaluationRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> evaluationService.findById(99L))
                    .isInstanceOf(java.util.NoSuchElementException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  create()
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("Guarda y retorna la evaluación")
        void shouldSaveAndReturn() {
            Evaluation e = Evaluation.builder().family(family).build();
            when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(i -> {
                Evaluation saved = i.getArgument(0);
                saved.setId(77L);
                return saved;
            });

            Evaluation result = evaluationService.create(e);
            assertThat(result.getId()).isEqualTo(77L);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  start()
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("start()")
    class Start {

        @Test
        @DisplayName("Familia no encontrada → RuntimeException")
        void shouldThrow_whenFamilyNotFound() {
            when(familyRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    evaluationService.start(new EvaluationDtos.EvaluationStartRequest(99L, null)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Familia no encontrada");
        }

        @Test
        @DisplayName("Sin memberId → guarda evaluación con status=STARTED y member=null")
        void shouldStart_withoutMember() {
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(i -> i.getArgument(0));

            Evaluation result = evaluationService.start(
                    new EvaluationDtos.EvaluationStartRequest(1L, null));

            assertThat(result.getStatus()).isEqualTo(EvaluationStatus.STARTED);
            assertThat(result.getMember()).isNull();
            assertThat(result.getAlgorithmVersion()).isEqualTo("RISK_ALGO_V1");
        }

        @Test
        @DisplayName("Con memberId → asigna el miembro a la evaluación")
        void shouldStart_withMember() {
            FamilyMember member = new FamilyMember();
            member.setId(3L);
            member.setFullName("Ana López");
            member.setRole("MADRE");

            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(memberRepository.findById(3L)).thenReturn(Optional.of(member));
            when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(i -> i.getArgument(0));

            Evaluation result = evaluationService.start(
                    new EvaluationDtos.EvaluationStartRequest(1L, 3L));

            assertThat(result.getMember()).isEqualTo(member);
            assertThat(result.getStatus()).isEqualTo(EvaluationStatus.STARTED);
        }

        @Test
        @DisplayName("memberId presente pero no encontrado → member asignado como null")
        void shouldStart_withNullMember_whenMemberNotFound() {
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(memberRepository.findById(77L)).thenReturn(Optional.empty());
            when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(i -> i.getArgument(0));

            Evaluation result = evaluationService.start(
                    new EvaluationDtos.EvaluationStartRequest(1L, 77L));

            assertThat(result.getMember()).isNull();
        }

        @Test
        @DisplayName("startedAt se asigna y no es null")
        void shouldSetStartedAt() {
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(i -> i.getArgument(0));

            Evaluation result = evaluationService.start(
                    new EvaluationDtos.EvaluationStartRequest(1L, null));

            assertThat(result.getStartedAt()).isNotNull();
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  finalize() — evidencia de Interrupción Deliberativa (ADR-007)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("finalize() — hypothesis_evidence de DELIBERATIVE_INTERRUPTION_HYPOTHESIS")
    class FinalizePauseCapacityEvidence {

        private Evaluation existing;
        private EvaluationDtos.EvaluationFinalizeRequest request;

        @BeforeEach
        void setUpFinalize() {
            existing = Evaluation.builder().id(50L).family(family).build();
            when(evaluationRepository.findById(50L)).thenReturn(Optional.of(existing));
            when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(i -> i.getArgument(0));

            request = new EvaluationDtos.EvaluationFinalizeRequest(
                    List.of(new EvaluationDtos.AnswerDto(1L, 3, null)),
                    null, null, null);
        }

        private RiskAlgoV1Engine.AlgoResult algoResultWithNeuroProfile(NeuroProfile neuroProfile) {
            return new RiskAlgoV1Engine.AlgoResult(
                    Map.of("emociones", 80.0, "comunicacion", 75.0, "habitos", 70.0, "tiempos", 65.0),
                    75.0,
                    0.0,
                    neuroProfile,
                    "MODERADO",
                    "comunicacion",
                    false,
                    false,
                    null,
                    null,
                    3,
                    List.of(),
                    List.of(),
                    null);
        }

        @Test
        @DisplayName("neuroProfile != null → escribe una fila con measurementType=PAUSE_CAPACITY")
        void shouldRecordPauseCapacityEvidence_whenNeuroProfilePresent() {
            NeuroProfile neuroProfile = NeuroProfile.builder().pauseCapacity(72.5).build();
            when(riskAlgoV1Engine.compute(any(), any())).thenReturn(algoResultWithNeuroProfile(neuroProfile));

            evaluationService.finalize(50L, request);

            ArgumentCaptor<HypothesisEvidence> captor = ArgumentCaptor.forClass(HypothesisEvidence.class);
            verify(hypothesisEvidenceRepository).save(captor.capture());

            HypothesisEvidence evidence = captor.getValue();
            assertThat(evidence.getHypothesis()).isEqualTo("DELIBERATIVE_INTERRUPTION_HYPOTHESIS");
            assertThat(evidence.getHypothesisVersion()).isEqualTo("v1");
            assertThat(evidence.getSubjectType()).isEqualTo("FAMILY");
            assertThat(evidence.getSubjectId()).isEqualTo(1L);
            assertThat(evidence.getMeasurementType()).isEqualTo("PAUSE_CAPACITY");
            assertThat(evidence.getMeasurementValue()).isEqualTo(72.5);
            assertThat(evidence.getInstrument()).isEqualTo("RISK_ALGO_V1");
            assertThat(evidence.getInstrumentVersion()).isEqualTo("1");
            assertThat(evidence.getSource()).isEqualTo(EvidenceSource.AUTOMATIC);
            assertThat(evidence.getObservedAt()).isNotNull().isBeforeOrEqualTo(LocalDateTime.now());
        }

        @Test
        @DisplayName("neuroProfile == null → no escribe ninguna fila (evita falso 0.0 por ausencia de dato)")
        void shouldNotRecordEvidence_whenNeuroProfileAbsent() {
            when(riskAlgoV1Engine.compute(any(), any())).thenReturn(algoResultWithNeuroProfile(null));

            evaluationService.finalize(50L, request);

            verify(hypothesisEvidenceRepository, never()).save(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  finalize() — evidencia de precisión anticipatoria proxy (ADR-008)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("finalize() — hypothesis_evidence de PROXY_PREDICTIVE_ACCURACY_HYPOTHESIS")
    class FinalizePredictiveAccuracyEvidence {

        private Evaluation existing;
        private EvaluationDtos.EvaluationFinalizeRequest request;

        @BeforeEach
        void setUpFinalize() {
            existing = Evaluation.builder().id(50L).family(family).build();
            when(evaluationRepository.findById(50L)).thenReturn(Optional.of(existing));
            when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(i -> i.getArgument(0));

            request = new EvaluationDtos.EvaluationFinalizeRequest(
                    List.of(new EvaluationDtos.AnswerDto(1L, 3, null)),
                    null, null, null);

            // Sin NeuroProfile — aisla las aserciones de PROXY_PREDICTIVE_ACCURACY_HYPOTHESIS
            // de la evidencia de DELIBERATIVE_INTERRUPTION_HYPOTHESIS (ADR-007), que solo se
            // escribe cuando neuroProfile != null.
            when(riskAlgoV1Engine.compute(any(), any())).thenReturn(new RiskAlgoV1Engine.AlgoResult(
                    Map.of("emociones", 80.0, "comunicacion", 75.0, "habitos", 70.0, "tiempos", 65.0),
                    75.0,
                    0.0,
                    null,
                    "MODERADO",
                    "comunicacion",
                    false,
                    false,
                    null,
                    null,
                    3,
                    List.of(),
                    List.of(),
                    null));
        }

        private Question question(Long id, String parentKey, String phase) {
            return Question.builder().id(id).questionKey("Q-" + id).parentKey(parentKey).phase(phase).build();
        }

        private EvaluationAnswer answer(Long questionId, Integer score) {
            return EvaluationAnswer.builder().questionId(questionId).score(score).build();
        }

        @Test
        @DisplayName("Par THINK+AFTERMATH completo → escribe exactamente 2 filas con los campos correctos")
        void shouldRecordBothRows_whenPairComplete() {
            Question think = question(101L, "M-POC-S1", "THINK");
            Question aftermath = question(102L, "M-POC-S1", "AFTERMATH");
            when(questionRepository.findAllById(any())).thenReturn(List.of(think, aftermath));
            when(evaluationAnswerRepository.findByEvaluationIdOrderByAnsweredAtAsc(50L))
                    .thenReturn(List.of(answer(101L, 2), answer(102L, 4)));

            evaluationService.finalize(50L, request);

            ArgumentCaptor<HypothesisEvidence> captor = ArgumentCaptor.forClass(HypothesisEvidence.class);
            verify(hypothesisEvidenceRepository, times(2)).save(captor.capture());

            List<HypothesisEvidence> saved = captor.getAllValues();
            assertThat(saved).hasSize(2);
            assertThat(saved).allSatisfy(e -> {
                assertThat(e.getHypothesis()).isEqualTo("PROXY_PREDICTIVE_ACCURACY_HYPOTHESIS");
                assertThat(e.getHypothesisVersion()).isEqualTo("v1");
                assertThat(e.getSubjectType()).isEqualTo("FAMILY");
                assertThat(e.getSubjectId()).isEqualTo(1L);
                assertThat(e.getInstrument()).isEqualTo("M-POC-S1");
                assertThat(e.getInstrumentVersion()).isEqualTo("1.2.0");
                assertThat(e.getSource()).isEqualTo(EvidenceSource.AUTOMATIC);
                assertThat(e.getObservedAt()).isNotNull();
            });

            HypothesisEvidence anticipated = saved.stream()
                    .filter(e -> e.getMeasurementType().equals("ANTICIPATED_THREAT_LEVEL"))
                    .findFirst().orElseThrow();
            assertThat(anticipated.getMeasurementValue()).isEqualTo(2.0);

            HypothesisEvidence outcome = saved.stream()
                    .filter(e -> e.getMeasurementType().equals("OUTCOME_SEVERITY_LEVEL"))
                    .findFirst().orElseThrow();
            assertThat(outcome.getMeasurementValue()).isEqualTo(4.0);
        }

        @Test
        @DisplayName("Solo THINK sin AFTERMATH → no escribe ninguna fila (par incompleto)")
        void shouldNotRecordEvidence_whenPairIncomplete() {
            Question think = question(201L, "M-POC-S2", "THINK");
            when(questionRepository.findAllById(any())).thenReturn(List.of(think));
            when(evaluationAnswerRepository.findByEvaluationIdOrderByAnsweredAtAsc(50L))
                    .thenReturn(List.of(answer(201L, 1)));

            evaluationService.finalize(50L, request);

            verify(hypothesisEvidenceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Dos escenarios completos en la misma evaluación → 4 filas, cada una atribuida a su parent_key")
        void shouldRecordIndependentPairs_forMultipleScenarios() {
            Question think1 = question(101L, "M-POC-S1", "THINK");
            Question aftermath1 = question(102L, "M-POC-S1", "AFTERMATH");
            Question think2 = question(201L, "M-POC-S2", "THINK");
            Question aftermath2 = question(202L, "M-POC-S2", "AFTERMATH");
            when(questionRepository.findAllById(any()))
                    .thenReturn(List.of(think1, aftermath1, think2, aftermath2));
            when(evaluationAnswerRepository.findByEvaluationIdOrderByAnsweredAtAsc(50L))
                    .thenReturn(List.of(answer(101L, 5), answer(102L, 1), answer(201L, 3), answer(202L, 3)));

            evaluationService.finalize(50L, request);

            ArgumentCaptor<HypothesisEvidence> captor = ArgumentCaptor.forClass(HypothesisEvidence.class);
            verify(hypothesisEvidenceRepository, times(4)).save(captor.capture());

            List<String> instruments = captor.getAllValues().stream()
                    .map(HypothesisEvidence::getInstrument)
                    .toList();
            assertThat(instruments).containsExactlyInAnyOrder("M-POC-S1", "M-POC-S1", "M-POC-S2", "M-POC-S2");
        }
    }
}
