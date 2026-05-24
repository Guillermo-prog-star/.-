package com.integrityfamily.plan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.ai.service.AiService;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.ImprovementPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de PlanTaskService.
 *
 * Cubre: generateTasksFromDiagnosis() — guard null, AI success, fallback
 * por rol (PADRE, MADRE, ADOLESCENTE, NIÑO/NINO, default) y
 * resolveFaseFromMilestone() invocado indirectamente.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlanTaskService — Unit Tests")
class PlanTaskServiceTest {

    @Mock ImprovementPlanRepository planRepository;
    @Mock FamilyRepository          familyRepository;
    @Mock AiService                 aiService;
    @Spy  ObjectMapper              objectMapper = new ObjectMapper();

    @InjectMocks
    PlanTaskService planTaskService;

    private Family family;
    private ImprovementPlan plan;

    @BeforeEach
    void setUp() {
        family = Family.builder()
                .id(1L)
                .name("Familia López")
                .currentMilestone("W1")
                .members(new ArrayList<>())
                .build();

        plan = ImprovementPlan.builder()
                .id(10L)
                .family(family)
                .title("Plan Activo")
                .build();
    }

    // ───────────────────────────────────────────────────────────────────
    //  Helper: builds a minimal Evaluation with a role and an existing plan
    // ───────────────────────────────────────────────────────────────────

    private Evaluation evaluation(String role) {
        FamilyMember member = new FamilyMember();
        member.setId(5L);
        member.setFullName("Test Member");
        member.setRole(role);
        member.setFamily(family);

        return Evaluation.builder()
                .id(99L)
                .family(family)
                .member(member)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Guard conditions
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Guard conditions")
    class Guards {

        @Test
        @DisplayName("null evaluation → retorna sin lanzar excepción ni llamar al repositorio")
        void shouldReturn_whenEvaluationIsNull() {
            planTaskService.generateTasksFromDiagnosis(null);
            verify(planRepository, never()).save(any());
        }

        @Test
        @DisplayName("evaluation con member null → retorna sin lanzar excepción")
        void shouldReturn_whenMemberIsNull() {
            Evaluation eval = Evaluation.builder()
                    .id(1L)
                    .family(family)
                    .member(null)
                    .build();

            planTaskService.generateTasksFromDiagnosis(eval);
            verify(planRepository, never()).save(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  AI success path
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("AI success path")
    class AiSuccessPath {

        @BeforeEach
        void stubPlan() {
            when(planRepository.findByFamilyId(1L)).thenReturn(List.of(plan));
        }

        @Test
        @DisplayName("IA retorna JSON válido → persiste misiones con título y dimensión del JSON")
        void shouldPersistAiMissions_whenAiReturnsValidJson() {
            String json = """
                    [
                      {
                        "title": "Misión IA A",
                        "description": "Descripción A",
                        "dimension": "COMUNICACION",
                        "objective": "Obj A",
                        "successMetric": "Métrica A",
                        "estimatedDuration": 30
                      }
                    ]
                    """;
            when(aiService.generateDiagnosticMissions(any())).thenReturn(json);

            planTaskService.generateTasksFromDiagnosis(evaluation("PADRE"));

            ArgumentCaptor<ImprovementPlan> captor = ArgumentCaptor.forClass(ImprovementPlan.class);
            verify(planRepository).save(captor.capture());

            List<PlanTask> tasks = captor.getValue().getTasks();
            assertThat(tasks).hasSize(1);
            assertThat(tasks.get(0).getTitle()).isEqualTo("Misión IA A");
            assertThat(tasks.get(0).getDimension()).isEqualTo("COMUNICACION");
        }

        @Test
        @DisplayName("JSON envuelto en ```json → se limpia el bloque markdown antes de parsear")
        void shouldStripMarkdownFence_beforeParsing() {
            String json = "```json\n[{\"title\":\"T\",\"description\":\"D\",\"dimension\":\"EMOCIONES\",\"objective\":\"O\",\"successMetric\":\"M\",\"estimatedDuration\":15}]\n```";
            when(aiService.generateDiagnosticMissions(any())).thenReturn(json);

            planTaskService.generateTasksFromDiagnosis(evaluation("MADRE"));

            ArgumentCaptor<ImprovementPlan> captor = ArgumentCaptor.forClass(ImprovementPlan.class);
            verify(planRepository).save(captor.capture());
            assertThat(captor.getValue().getTasks()).hasSize(1);
            assertThat(captor.getValue().getTasks().get(0).getTitle()).isEqualTo("T");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  AI failure → static fallback by role
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Fallback estático por rol")
    class StaticFallback {

        @BeforeEach
        void stubPlanAndAiFail() {
            when(planRepository.findByFamilyId(1L)).thenReturn(List.of(plan));
            when(aiService.generateDiagnosticMissions(any()))
                    .thenThrow(new RuntimeException("AI unavailable"));
        }

        @Test
        @DisplayName("PADRE → 2 tareas (Escucha Activa + Liderazgo sin Pantallas)")
        void shouldGeneratePadreTasks_whenAiFails() {
            planTaskService.generateTasksFromDiagnosis(evaluation("PADRE"));

            ArgumentCaptor<ImprovementPlan> captor = ArgumentCaptor.forClass(ImprovementPlan.class);
            verify(planRepository).save(captor.capture());
            List<PlanTask> tasks = captor.getValue().getTasks();

            assertThat(tasks).hasSize(2);
            assertThat(tasks.get(0).getTitle()).isEqualTo("Escucha Activa Consciente");
            assertThat(tasks.get(1).getTitle()).isEqualTo("Liderazgo sin Pantallas");
        }

        @Test
        @DisplayName("MADRE → 2 tareas (Distribución de Carga + Espacio de Autocuidado)")
        void shouldGenerateMadreTasks_whenAiFails() {
            planTaskService.generateTasksFromDiagnosis(evaluation("MADRE"));

            ArgumentCaptor<ImprovementPlan> captor = ArgumentCaptor.forClass(ImprovementPlan.class);
            verify(planRepository).save(captor.capture());
            List<PlanTask> tasks = captor.getValue().getTasks();

            assertThat(tasks).hasSize(2);
            assertThat(tasks.get(0).getTitle()).isEqualTo("Distribución de Carga");
            assertThat(tasks.get(1).getTitle()).isEqualTo("Espacio de Autocuidado");
        }

        @Test
        @DisplayName("ADOLESCENTE → 2 tareas (Expresión Segura + Propuesta de Conexión)")
        void shouldGenerateAdolescenteTasks_whenAiFails() {
            planTaskService.generateTasksFromDiagnosis(evaluation("ADOLESCENTE"));

            ArgumentCaptor<ImprovementPlan> captor = ArgumentCaptor.forClass(ImprovementPlan.class);
            verify(planRepository).save(captor.capture());
            List<PlanTask> tasks = captor.getValue().getTasks();

            assertThat(tasks).hasSize(2);
            assertThat(tasks.get(0).getTitle()).isEqualTo("Expresión Segura");
            assertThat(tasks.get(1).getTitle()).isEqualTo("Propuesta de Conexión");
        }

        @Test
        @DisplayName("NINO → 2 tareas (Rutina Divertida + Juego Consciente)")
        void shouldGenerateNinoTasks_whenAiFails() {
            planTaskService.generateTasksFromDiagnosis(evaluation("NINO"));

            ArgumentCaptor<ImprovementPlan> captor = ArgumentCaptor.forClass(ImprovementPlan.class);
            verify(planRepository).save(captor.capture());
            List<PlanTask> tasks = captor.getValue().getTasks();

            assertThat(tasks).hasSize(2);
            assertThat(tasks.get(0).getTitle()).isEqualTo("Rutina Divertida");
            assertThat(tasks.get(1).getTitle()).isEqualTo("Juego Consciente");
        }

        @Test
        @DisplayName("NIÑO (con tilde) → mismas tareas que NINO")
        void shouldGenerateNiñoTasks_whenAiFails() {
            planTaskService.generateTasksFromDiagnosis(evaluation("NIÑO"));

            ArgumentCaptor<ImprovementPlan> captor = ArgumentCaptor.forClass(ImprovementPlan.class);
            verify(planRepository).save(captor.capture());
            List<PlanTask> tasks = captor.getValue().getTasks();

            assertThat(tasks).hasSize(2);
            assertThat(tasks.get(0).getTitle()).isEqualTo("Rutina Divertida");
        }

        @Test
        @DisplayName("Rol desconocido → 1 tarea default (Misión de Conexión)")
        void shouldGenerateDefaultTask_forUnknownRole() {
            planTaskService.generateTasksFromDiagnosis(evaluation("ABUELO"));

            ArgumentCaptor<ImprovementPlan> captor = ArgumentCaptor.forClass(ImprovementPlan.class);
            verify(planRepository).save(captor.capture());
            List<PlanTask> tasks = captor.getValue().getTasks();

            assertThat(tasks).hasSize(1);
            assertThat(tasks.get(0).getTitle()).isEqualTo("Misión de Conexión");
        }

        @Test
        @DisplayName("Fallback — role es null → guarda el plan sin tareas nuevas (guard if(role!=null))")
        void shouldSavePlanEmpty_whenRoleIsNull() {
            planTaskService.generateTasksFromDiagnosis(evaluation(null));

            // La guarda `if (role != null)` evita el switch → newTasks vacío
            // pero planRepository.save() igual se llama con el plan
            ArgumentCaptor<ImprovementPlan> captor = ArgumentCaptor.forClass(ImprovementPlan.class);
            verify(planRepository).save(captor.capture());
            assertThat(captor.getValue().getTasks()).isEmpty();
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  resolveFaseFromMilestone — via PADRE fallback (milestone influence)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("resolveFaseFromMilestone() — influencia en las tareas generadas")
    class ResolveFase {

        @ParameterizedTest(name = "{0} → fase {1}")
        @CsvSource({
            "W1,   RECONOCIMIENTO",
            "M1,   RECONOCIMIENTO",
            "M3,   RECONOCIMIENTO",
            "M6,   AMOR",
            "M12,  AMOR",
            "M18,  ENTREGA",
            "M36,  ENTREGA",
            "M99,  RECONOCIMIENTO"   // código desconocido → default
        })
        void shouldResolveFase_fromMilestone(String milestone, String expectedFase) {
            family.setCurrentMilestone(milestone);
            when(planRepository.findByFamilyId(1L)).thenReturn(List.of(plan));
            when(aiService.generateDiagnosticMissions(any()))
                    .thenThrow(new RuntimeException("AI down"));

            planTaskService.generateTasksFromDiagnosis(evaluation("PADRE"));

            ArgumentCaptor<ImprovementPlan> captor = ArgumentCaptor.forClass(ImprovementPlan.class);
            verify(planRepository).save(captor.capture());
            assertThat(captor.getValue().getTasks())
                    .allMatch(t -> t.getFase().equals(expectedFase));

            // Reset para el siguiente parámetro
            TestBed_reset();
        }

        void TestBed_reset() {
            // clearInvocations resets call count but retains stub — suitable for @ParameterizedTest
            clearInvocations(planRepository, aiService);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  createTasksFromAi()
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createTasksFromAi()")
    class CreateTasksFromAi {

        @Test
        @DisplayName("Lista vacía → plan guardado sin tareas nuevas")
        void shouldSavePlan_withEmptyTaskList() {
            when(planRepository.findByFamilyId(1L)).thenReturn(List.of(plan));

            planTaskService.createTasksFromAi(1L, List.of());

            verify(planRepository).save(plan);
            assertThat(plan.getTasks()).isEmpty();
        }

        @Test
        @DisplayName("Propuesta con riskLevel=HIGH → impactoIcf=20")
        void shouldSetHighImpact_whenRiskLevelHigh() {
            when(planRepository.findByFamilyId(1L)).thenReturn(List.of(plan));

            // AiMissionProposal(dimension, riskLevel, problemDetected, objective,
            //                   missionType, targetMembers, frequency, estimatedDuration,
            //                   successMetric, adaptiveReason, title, description)
            var proposal = new com.integrityfamily.plan.dto.PlanDtos.AiMissionProposal(
                    "EMOCIONES", "HIGH", "brecha emocional", "Objetivo",
                    "RUTINA", List.of("PADRE"), "DIARIA", 15,
                    "Métrica", "razón", "Misión Alta", "Descripción"
            );

            planTaskService.createTasksFromAi(1L, List.of(proposal));

            assertThat(plan.getTasks()).hasSize(1);
            assertThat(plan.getTasks().get(0).getImpactoIcf()).isEqualTo(20);
        }

        @Test
        @DisplayName("Propuesta con riskLevel bajo → impactoIcf=10")
        void shouldSetLowImpact_whenRiskLevelNotHigh() {
            when(planRepository.findByFamilyId(1L)).thenReturn(List.of(plan));

            var proposal = new com.integrityfamily.plan.dto.PlanDtos.AiMissionProposal(
                    "HABITOS", "LOW", "brecha de hábito", "Objetivo",
                    "ACCION", List.of(), "SEMANAL", 20,
                    "Métrica", "razón", "Misión Baja", "Descripción"
            );

            planTaskService.createTasksFromAi(1L, List.of(proposal));

            assertThat(plan.getTasks().get(0).getImpactoIcf()).isEqualTo(10);
        }
    }
}
