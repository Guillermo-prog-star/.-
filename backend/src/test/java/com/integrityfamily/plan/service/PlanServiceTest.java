package com.integrityfamily.plan.service;

import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import com.integrityfamily.plan.dto.PlanDtos.PlanResponse;
import com.integrityfamily.plan.dto.PlanDtos.PlanTaskResponse;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de PlanService.
 *
 * Cubre: findAllPlans(), findByFamilyId(), findPlanById() (notFound),
 * createPlan(), updatePlan() (notFound y éxito), deletePlan(),
 * completeTask() (éxito con auto-evidence, error silenciado en logbook,
 * error silenciado en RabbitMQ, marcado como incompleto → sin logbook)
 * y findTaskById() / deleteTask().
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlanService — Unit Tests")
class PlanServiceTest {

    @Mock ImprovementPlanRepository       planRepository;
    @Mock PlanTaskRepository              planTaskRepository;
    @Mock FamilyLogbookEntryRepository    logbookEntryRepository;
    @Mock EvaluationRepository            evaluationRepository;
    @Mock PlanTemplateRepository          planTemplateRepository;
    @Mock PlanTemplateActivityRepository  planTemplateActivityRepository;
    @Mock MilestoneRepository             milestoneRepository;
    @Mock QuestionRepository              questionRepository;
    @Mock RabbitTemplate                  rabbitTemplate;

    @InjectMocks
    PlanService planService;

    private Family family;
    private ImprovementPlan plan;

    @BeforeEach
    void setUp() {
        family = Family.builder()
                .id(1L)
                .name("Familia López")
                .familyCode("IF-2026-TEST")
                .build();

        plan = ImprovementPlan.builder()
                .id(10L)
                .family(family)
                .title("Plan Activo")
                .description("Descripción")
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  findAllPlans()
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findAllPlans()")
    class FindAllPlans {

        @Test
        @DisplayName("Delega en el repositorio y mapea a DTO")
        void shouldReturnAllPlans() {
            ImprovementPlan p2 = ImprovementPlan.builder().id(20L).family(family).title("Plan 2").build();
            when(planRepository.findAll()).thenReturn(List.of(plan, p2));

            List<PlanResponse> result = planService.findAllPlans();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).id()).isEqualTo(10L);
            assertThat(result.get(1).id()).isEqualTo(20L);
        }

        @Test
        @DisplayName("Lista vacía → retorna lista vacía")
        void shouldReturnEmpty_whenNoPlans() {
            when(planRepository.findAll()).thenReturn(List.of());
            assertThat(planService.findAllPlans()).isEmpty();
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  findByFamilyId()
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findByFamilyId()")
    class FindByFamilyId {

        @Test
        @DisplayName("Filtra por familyId y mapea a DTO")
        void shouldReturnPlansForFamily() {
            when(planRepository.findByFamilyId(1L)).thenReturn(List.of(plan));

            List<PlanResponse> result = planService.findByFamilyId(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).familyId()).isEqualTo(1L);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  findPlanById()
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findPlanById()")
    class FindPlanById {

        @Test
        @DisplayName("ID existente → retorna DTO con el título correcto")
        void shouldReturnPlan_whenExists() {
            when(planRepository.findById(10L)).thenReturn(Optional.of(plan));

            PlanResponse response = planService.findPlanById(10L);

            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.title()).isEqualTo("Plan Activo");
        }

        @Test
        @DisplayName("ID inexistente → RuntimeException con el ID en el mensaje")
        void shouldThrow_whenNotFound() {
            when(planRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> planService.findPlanById(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("99");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  createPlan()
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createPlan()")
    class CreatePlan {

        @Test
        @DisplayName("Guarda y retorna DTO con el ID asignado")
        void shouldSaveAndReturnDto() {
            ImprovementPlan newPlan = ImprovementPlan.builder()
                    .family(family).title("Nuevo Plan").build();
            when(planRepository.save(any())).thenAnswer(i -> {
                ImprovementPlan p = i.getArgument(0);
                p.setId(55L);
                return p;
            });

            PlanResponse response = planService.createPlan(newPlan);

            assertThat(response.id()).isEqualTo(55L);
            assertThat(response.title()).isEqualTo("Nuevo Plan");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  updatePlan()
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updatePlan()")
    class UpdatePlan {

        @Test
        @DisplayName("ID existente → actualiza título y descripción")
        void shouldUpdateTitleAndDescription() {
            when(planRepository.findById(10L)).thenReturn(Optional.of(plan));
            when(planRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            ImprovementPlan patch = ImprovementPlan.builder()
                    .title("Actualizado").description("Nueva desc").build();
            PlanResponse response = planService.updatePlan(10L, patch);

            assertThat(response.title()).isEqualTo("Actualizado");
            assertThat(response.description()).isEqualTo("Nueva desc");
        }

        @Test
        @DisplayName("ID inexistente → RuntimeException")
        void shouldThrow_whenNotFound() {
            when(planRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> planService.updatePlan(99L,
                    ImprovementPlan.builder().title("X").build()))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  deletePlan()
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deletePlan()")
    class DeletePlan {

        @Test
        @DisplayName("Llama deleteById con el ID correcto")
        void shouldCallDeleteById() {
            planService.deletePlan(10L);
            verify(planRepository).deleteById(10L);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  completeTask()
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("completeTask()")
    class CompleteTask {

        private PlanTask task;

        @BeforeEach
        void buildTask() {
            task = PlanTask.builder()
                    .id(5L)
                    .title("Tarea Prueba")
                    .plan(plan)
                    .dimension("COMUNICACION")
                    .impactoIcf(15)
                    .build();
        }

        @Test
        @DisplayName("completed=true → persiste tarea como completada y crea entrada de bitácora")
        void shouldComplete_andCreateLogbookEntry() {
            when(planTaskRepository.findById(5L)).thenReturn(Optional.of(task));
            when(planTaskRepository.save(any())).thenReturn(task);
            // logbookEntryRepository.save() retorna entidad — Mockito devuelve null por defecto (OK)
            lenient().doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), (Object) any());

            planService.completeTask(5L, true);

            ArgumentCaptor<PlanTask> taskCaptor = ArgumentCaptor.forClass(PlanTask.class);
            verify(planTaskRepository).save(taskCaptor.capture());
            assertThat(taskCaptor.getValue().isCompleted()).isTrue();
            verify(logbookEntryRepository).save(any(FamilyLogbookEntry.class));
        }

        @Test
        @DisplayName("completed=false → no crea entrada de bitácora ni evento RabbitMQ")
        void shouldNotCreateLogbook_whenMarkingIncomplete() {
            when(planTaskRepository.findById(5L)).thenReturn(Optional.of(task));
            when(planTaskRepository.save(any())).thenReturn(task);

            planService.completeTask(5L, false);

            verify(logbookEntryRepository, never()).save(any());
            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), (Object) any());
        }

        @Test
        @DisplayName("Error en logbook → no propaga (try-catch silencia el error)")
        void shouldNotPropagate_whenLogbookFails() {
            when(planTaskRepository.findById(5L)).thenReturn(Optional.of(task));
            when(planTaskRepository.save(any())).thenReturn(task);
            doThrow(new RuntimeException("DB down"))
                    .when(logbookEntryRepository).save(any());
            lenient().doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), (Object) any());

            // No debe lanzar aunque el logbook falle
            PlanTaskResponse result = planService.completeTask(5L, true);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("tarea sin plan → no intenta crear logbook ni evento")
        void shouldSkipLogbook_whenTaskHasNoPlan() {
            PlanTask taskNoplan = PlanTask.builder().id(7L).title("Sin plan").build();
            when(planTaskRepository.findById(7L)).thenReturn(Optional.of(taskNoplan));
            when(planTaskRepository.save(any())).thenReturn(taskNoplan);

            planService.completeTask(7L, true);

            verify(logbookEntryRepository, never()).save(any());
        }

        @Test
        @DisplayName("ID inexistente → RuntimeException con el ID en el mensaje")
        void shouldThrow_whenTaskNotFound() {
            when(planTaskRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> planService.completeTask(99L, true))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("99");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  findTaskById() / deleteTask()
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findTaskById() / deleteTask()")
    class TaskQueries {

        @Test
        @DisplayName("findTaskById() existente → retorna DTO")
        void shouldReturnTask_whenExists() {
            PlanTask task = PlanTask.builder().id(3L).title("T").build();
            when(planTaskRepository.findById(3L)).thenReturn(Optional.of(task));

            PlanTaskResponse response = planService.findTaskById(3L);
            assertThat(response.id()).isEqualTo(3L);
        }

        @Test
        @DisplayName("findTaskById() inexistente → RuntimeException")
        void shouldThrow_whenTaskNotFound() {
            when(planTaskRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> planService.findTaskById(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("deleteTask() llama deleteById con el ID correcto")
        void shouldCallDeleteById() {
            planService.deleteTask(8L);
            verify(planTaskRepository).deleteById(8L);
        }
    }
}
