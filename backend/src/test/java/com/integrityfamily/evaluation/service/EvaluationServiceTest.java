package com.integrityfamily.evaluation.service;

import com.integrityfamily.ai.service.AiService;
import com.integrityfamily.analytics.service.FamilyProgressAnalyticsService;
import com.integrityfamily.assessment.service.AssessmentAnswerService;
import com.integrityfamily.cognitive.service.*;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import com.integrityfamily.dto.EvaluationDtos;
import com.integrityfamily.milestone.service.MilestoneService;
import com.integrityfamily.plan.service.PlanGenerationService;
import com.integrityfamily.plan.service.PlanTaskService;
import com.integrityfamily.risk.service.RiskAlgoV1Engine;
import com.integrityfamily.risk.service.RiskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.ArrayList;
import java.util.List;
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
}
