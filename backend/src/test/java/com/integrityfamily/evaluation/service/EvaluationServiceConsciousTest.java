package com.integrityfamily.evaluation.service;

import com.integrityfamily.assessment.service.AssessmentAnswerService;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import com.integrityfamily.dto.EvaluationDtos;
import com.integrityfamily.plan.service.PlanGenerationService;
import com.integrityfamily.plan.service.PlanTaskService;
import com.integrityfamily.risk.service.RiskAlgoV1Engine;
import com.integrityfamily.risk.service.RiskService;
import com.integrityfamily.milestone.service.MilestoneService;
import com.integrityfamily.ai.service.AiService;
import com.integrityfamily.scanner.service.DeterministicExplanationPipeline;
import com.integrityfamily.scanner.service.InferenceRecordService;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class EvaluationServiceConsciousTest {

    @Mock private EvaluationRepository           evaluationRepository;
    @Mock private EvaluationAnswerRepository     evaluationAnswerRepository;
    @Mock private FamilyRepository               familyRepository;
    @Mock private MemberRepository               memberRepository;
    @Mock private QuestionRepository             questionRepository;
    @Mock private AssessmentAnswerService        assessmentAnswerService;
    @Mock private RiskAlgoV1Engine               riskAlgoV1Engine;
    @Mock private RiskService                    riskService;
    @Mock private RabbitTemplate                 rabbitTemplate;
    @Mock private MilestoneService               milestoneService;
    @Mock private AiService                      aiService;
    @Mock private PlanTaskService                planTaskService;
    @Mock private PlanGenerationService          planGenerationService;
    @Spy  private DeterministicExplanationPipeline explanationPipeline = new DeterministicExplanationPipeline();
    @Mock private InferenceRecordService         inferenceRecordService;

    @InjectMocks
    private EvaluationService evaluationService;

    private Family family;
    private FamilyMember member;
    private Evaluation evaluation;

    @BeforeEach
    void setUp() {
        family = Family.builder().id(1L).name("Familia Lopez").build();
        member = FamilyMember.builder().id(1L).fullName("Juan").role("PADRE").family(family).build();
        evaluation = Evaluation.builder()
                .id(100L)
                .family(family)
                .member(member)
                .status(EvaluationStatus.STARTED)
                .startedAt(LocalDateTime.now())
                .answers(new ArrayList<>())
                .dimensionScores(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Debe generar interpretación consciente y misiones automáticas al finalizar evaluación")
    void shouldGenerateInterpretationAndMissions() {
        Mockito.when(evaluationRepository.findById(100L)).thenReturn(Optional.of(evaluation));
        Mockito.when(evaluationRepository.save(any(Evaluation.class)))
               .thenAnswer(invocation -> invocation.getArgument(0));

        // AlgoResult con ICF < 60 para activar la recomendación en la síntesis del rol PADRE
        RiskAlgoV1Engine.UncertaintyVector testUncert =
                new RiskAlgoV1Engine.UncertaintyVector(0.05, 0.20, 0.10, 0.05, 0.05, 0.09);
        Mockito.when(riskAlgoV1Engine.compute(any(), any())).thenReturn(
                new RiskAlgoV1Engine.AlgoResult(
                        Map.of("emociones", 50.0, "comunicacion", 50.0, "habitos", 50.0, "tiempos", 50.0),
                        50.0,           // healthyIndex (ICF < 60)
                        "MODERADO",     // riskLevel
                        "comunicacion", // criticalDimension
                        false,          // simulationSuspected
                        false,          // relapseDetected
                        "COMUNICACION_CONSCIENTE", // suggestedMissionGenerator
                        "Reactiva",     // consciousnessLabel
                        4,              // consciousnessLevel
                        List.of(),      // relapseFlags
                        List.of(),      // mirrorFlags
                        testUncert      // uncertainty (IF-SUM)
                )
        );

        // Flujo mobile-first: body sin respuestas; el servicio carga desde BD (lista vacía por defecto del mock)
        EvaluationDtos.EvaluationFinalizeRequest request = new EvaluationDtos.EvaluationFinalizeRequest(
                new ArrayList<>(),
                100.0,
                false,
                new HashMap<>()
        );

        EvaluationDtos.FinalizeResult finalizeResult = evaluationService.finalize(100L, request);
        Evaluation result = finalizeResult.evaluation();

        assertNotNull(result);
        assertNotNull(result.getSpiritualSynthesis());
        // IF-DEP: verificar estructura determinística del pipeline
        assertTrue(result.getSpiritualSynthesis().contains("[DIAGNÓSTICO CONSCIENTE]"));
        assertTrue(result.getSpiritualSynthesis().contains("Observación:"));
        assertTrue(result.getSpiritualSynthesis().contains("Misión activada:"));
        // PADRE + MODERADO → plantilla específica con "tensiones intermitentes"
        assertTrue(result.getSpiritualSynthesis().contains("tensiones intermitentes"));

        // Verificar que se llamó a PlanTaskService (misiones automáticas)
        verify(planTaskService, times(1)).generateTasksFromDiagnosis(any(Evaluation.class));
    }
}
