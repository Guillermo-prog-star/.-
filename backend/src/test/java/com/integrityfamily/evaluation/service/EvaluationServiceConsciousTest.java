package com.integrityfamily.evaluation.service;

import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import com.integrityfamily.plan.service.PlanTaskService;
import com.integrityfamily.risk.service.RiskService;
import com.integrityfamily.milestone.service.MilestoneService;
import com.integrityfamily.ai.service.AiService;
import com.integrityfamily.dto.EvaluationDtos;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class EvaluationServiceConsciousTest {

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
    @Mock
    private PlanTaskService planTaskService;

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
        Mockito.when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Corregido: Se pasan los 4 argumentos requeridos por el record
        EvaluationDtos.EvaluationFinalizeRequest request = new EvaluationDtos.EvaluationFinalizeRequest(
                new ArrayList<>(),
                100.0,
                false,
                new HashMap<>()
        );

        Evaluation result = evaluationService.finalize(100L, request);

        assertNotNull(result);
        assertNotNull(result.getSpiritualSynthesis());
        assertTrue(result.getSpiritualSynthesis().contains("[DIAGNÓSTICO CONSCIENTE]"));
        assertTrue(result.getSpiritualSynthesis().contains("Foco: Liderazgo emocional"));

        // Verificar que se llamó a PlanTaskService
        verify(planTaskService, times(1)).generateTasksFromDiagnosis(any(Evaluation.class));
    }
}
