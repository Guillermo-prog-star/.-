package com.integrityfamily.milestone.service;

import com.integrityfamily.common.exception.NotFoundException;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link MilestoneService}.
 *
 * No levanta contexto Spring — usa Mockito strict stubs.
 * Documenta:
 *   - CRUD básico de hitos
 *   - Ordenamiento por orderIndex en findAll()
 *   - getCurrentMilestoneLabel: familia no encontrada, hito con label, fallback a título, fallback a código
 *   - evaluate(): terminal M36, criterios de avance (tiempo, ICF, tareas)
 *   - advanceMilestone(): terminal, bloqueado, avance efectivo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MilestoneService — Unit Tests")
class MilestoneServiceTest {

    @Mock MilestoneRepository    milestoneRepository;
    @Mock FamilyRepository       familyRepository;
    @Mock EvaluationRepository   evaluationRepository;
    @Mock PlanTaskRepository     planTaskRepository;

    @InjectMocks MilestoneService service;

    private Family family;
    private Milestone milestoneW1;

    @BeforeEach
    void setUp() {
        family = Family.builder()
                .id(1L)
                .name("Los García")
                .currentMilestone("W1")
                .milestoneStartedAt(LocalDateTime.now().minusDays(10))
                .build();

        milestoneW1 = Milestone.builder()
                .id(1L).code("W1").label("Semana 1").durationDays(7).orderIndex(1)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  findById()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("retorna el hito cuando existe")
        void findById_found() {
            when(milestoneRepository.findById(1L)).thenReturn(Optional.of(milestoneW1));

            Milestone result = service.findById(1L);

            assertThat(result.getCode()).isEqualTo("W1");
        }

        @Test
        @DisplayName("lanza NotFoundException cuando no existe")
        void findById_notFound() {
            when(milestoneRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(99L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  create() / update() / delete()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("delega al repositorio y retorna el objeto guardado")
        void create_delegatesToRepo() {
            when(milestoneRepository.save(milestoneW1)).thenReturn(milestoneW1);

            Milestone result = service.create(milestoneW1);

            assertThat(result).isSameAs(milestoneW1);
            verify(milestoneRepository).save(milestoneW1);
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("actualiza todos los campos en la entidad existente y persiste")
        void update_modifiesFields() {
            Milestone updated = Milestone.builder()
                    .title("Nuevo Título").label("Semana 1 - v2")
                    .durationDays(14).orderIndex(2).description("Nueva descripción")
                    .build();

            when(milestoneRepository.findById(1L)).thenReturn(Optional.of(milestoneW1));
            when(milestoneRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Milestone result = service.update(1L, updated);

            assertThat(result.getLabel()).isEqualTo("Semana 1 - v2");
            assertThat(result.getDurationDays()).isEqualTo(14);
            assertThat(result.getOrderIndex()).isEqualTo(2);
            assertThat(result.getDescription()).isEqualTo("Nueva descripción");
            verify(milestoneRepository).save(milestoneW1);
        }

        @Test
        @DisplayName("lanza NotFoundException si el hito no existe")
        void update_notFound() {
            when(milestoneRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(99L, milestoneW1))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("elimina el hito cuando existe")
        void delete_success() {
            when(milestoneRepository.existsById(1L)).thenReturn(true);
            doNothing().when(milestoneRepository).deleteById(1L);

            service.delete(1L);

            verify(milestoneRepository).deleteById(1L);
        }

        @Test
        @DisplayName("lanza NotFoundException si no existe")
        void delete_notFound() {
            when(milestoneRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> service.delete(99L))
                    .isInstanceOf(NotFoundException.class);

            verify(milestoneRepository, never()).deleteById(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  findAll()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findAll() — ordenado por orderIndex")
    class FindAll {

        @Test
        @DisplayName("retorna lista ordenada ascendente por orderIndex")
        void findAll_sortedByOrderIndex() {
            Milestone m3 = Milestone.builder().id(3L).code("M3").label("Mes 3").orderIndex(3).build();
            Milestone m1 = Milestone.builder().id(2L).code("M1").label("Mes 1").orderIndex(2).build();
            // W1 has orderIndex=1

            // Repo devuelve desordenados
            when(milestoneRepository.findAll()).thenReturn(List.of(m3, milestoneW1, m1));

            List<Milestone> result = service.findAll();

            assertThat(result).extracting(Milestone::getCode)
                    .containsExactly("W1", "M1", "M3");
        }

        @Test
        @DisplayName("nulls de orderIndex van al final (99)")
        void findAll_nullOrderIndexLast() {
            Milestone noOrder = Milestone.builder().id(99L).code("XX").label("Sin orden").orderIndex(null).build();

            when(milestoneRepository.findAll()).thenReturn(List.of(noOrder, milestoneW1));

            List<Milestone> result = service.findAll();

            assertThat(result.get(0).getCode()).isEqualTo("W1");
            assertThat(result.get(1).getCode()).isEqualTo("XX");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  getCurrentMilestoneLabel()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getCurrentMilestoneLabel()")
    class GetCurrentMilestoneLabel {

        @Test
        @DisplayName("retorna el label del hito cuando existe")
        void returnsLabel() {
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(milestoneRepository.findByCode("W1")).thenReturn(Optional.of(milestoneW1));

            String label = service.getCurrentMilestoneLabel(1L);

            assertThat(label).isEqualTo("Semana 1");
        }

        @Test
        @DisplayName("usa el título si el label es null")
        void fallsBackToTitle() {
            Milestone noLabel = Milestone.builder()
                    .id(2L).code("W1").label(null).title("Semana Uno").build();

            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(milestoneRepository.findByCode("W1")).thenReturn(Optional.of(noLabel));

            assertThat(service.getCurrentMilestoneLabel(1L)).isEqualTo("Semana Uno");
        }

        @Test
        @DisplayName("fallback a 'Hito <code>' si el hito no está en el catálogo")
        void fallsBackToCode() {
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(milestoneRepository.findByCode("W1")).thenReturn(Optional.empty());

            assertThat(service.getCurrentMilestoneLabel(1L)).isEqualTo("Hito W1");
        }

        @Test
        @DisplayName("lanza NotFoundException si la familia no existe")
        void familyNotFound() {
            when(familyRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getCurrentMilestoneLabel(99L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  evaluate() — criterios de avance de hito
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("evaluate() — criterios de avance")
    class Evaluate {

        /** Construye una evaluación FINALIZED con ICF y riesgo dados */
        private Evaluation eval(double icf, String risk) {
            return Evaluation.builder()
                    .id(1L)
                    .status(EvaluationStatus.FINALIZED)
                    .milestoneKey("W1")
                    .icf(icf)
                    .riskLevel(risk)
                    .finalizedAt(LocalDateTime.now().minusDays(1))
                    .build();
        }

        @Test
        @DisplayName("hito M36 → terminal = true, canAdvance = false")
        void evaluate_m36IsTerminal() {
            Family m36Family = Family.builder()
                    .id(2L).name("Terminal").currentMilestone("M36").build();

            when(familyRepository.findById(2L)).thenReturn(Optional.of(m36Family));

            MilestoneService.AdvancementEvaluation result = service.evaluate(2L);

            assertThat(result.terminal()).isTrue();
            assertThat(result.canAdvance()).isFalse();
        }

        @Test
        @DisplayName("tiempo insuficiente → canAdvance = false, timeMet = false")
        void evaluate_timeNotMet() {
            // milestoneStartedAt = now - 2 días; W1 requiere 7 × 0.85 = 5.95 → 5 min días
            family.setMilestoneStartedAt(LocalDateTime.now().minusDays(2));

            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(milestoneRepository.findByCode("W1")).thenReturn(Optional.of(milestoneW1));
            when(evaluationRepository.findByFamilyIdAndMilestoneKeyAndStatus(1L, "W1", EvaluationStatus.FINALIZED))
                    .thenReturn(List.of(eval(60.0, "MODERADO")));
            when(planTaskRepository.countByFamilyIdAndMilestoneCode(1L, "W1")).thenReturn(5L);
            when(planTaskRepository.countCompletedByFamilyIdAndMilestoneCode(1L, "W1")).thenReturn(4L);

            MilestoneService.AdvancementEvaluation result = service.evaluate(1L);

            assertThat(result.timeMet()).isFalse();
            assertThat(result.canAdvance()).isFalse();
        }

        @Test
        @DisplayName("sin evaluaciones finalizadas → icfMet = false, canAdvance = false")
        void evaluate_noEvaluations_icfNotMet() {
            // Tiempo suficiente para W1 (10 días > 5.95)
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(milestoneRepository.findByCode("W1")).thenReturn(Optional.of(milestoneW1));
            when(evaluationRepository.findByFamilyIdAndMilestoneKeyAndStatus(1L, "W1", EvaluationStatus.FINALIZED))
                    .thenReturn(List.of());
            when(planTaskRepository.countByFamilyIdAndMilestoneCode(1L, "W1")).thenReturn(0L);
            when(planTaskRepository.countCompletedByFamilyIdAndMilestoneCode(1L, "W1")).thenReturn(0L);

            MilestoneService.AdvancementEvaluation result = service.evaluate(1L);

            assertThat(result.timeMet()).isTrue();
            assertThat(result.icfMet()).isFalse();
            assertThat(result.canAdvance()).isFalse();
        }

        @Test
        @DisplayName("ICF por debajo del umbral W1 (40.0) → icfMet = false")
        void evaluate_icfBelowThreshold() {
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(milestoneRepository.findByCode("W1")).thenReturn(Optional.of(milestoneW1));
            when(evaluationRepository.findByFamilyIdAndMilestoneKeyAndStatus(1L, "W1", EvaluationStatus.FINALIZED))
                    .thenReturn(List.of(eval(35.0, "ALTO"))); // ICF 35 < 40
            when(planTaskRepository.countByFamilyIdAndMilestoneCode(1L, "W1")).thenReturn(0L);
            when(planTaskRepository.countCompletedByFamilyIdAndMilestoneCode(1L, "W1")).thenReturn(0L);

            MilestoneService.AdvancementEvaluation result = service.evaluate(1L);

            assertThat(result.icfMet()).isFalse();
            assertThat(result.canAdvance()).isFalse();
        }

        @Test
        @DisplayName("tareas insuficientes (riesgo MODERADO, umbral 60%) → tasksMet = false")
        void evaluate_tasksNotMet() {
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(milestoneRepository.findByCode("W1")).thenReturn(Optional.of(milestoneW1));
            when(evaluationRepository.findByFamilyIdAndMilestoneKeyAndStatus(1L, "W1", EvaluationStatus.FINALIZED))
                    .thenReturn(List.of(eval(50.0, "MODERADO")));
            when(planTaskRepository.countByFamilyIdAndMilestoneCode(1L, "W1")).thenReturn(10L);
            when(planTaskRepository.countCompletedByFamilyIdAndMilestoneCode(1L, "W1")).thenReturn(4L); // 40% < 60%

            MilestoneService.AdvancementEvaluation result = service.evaluate(1L);

            assertThat(result.tasksMet()).isFalse();
            assertThat(result.canAdvance()).isFalse();
        }

        @Test
        @DisplayName("todos los criterios cumplidos → canAdvance = true")
        void evaluate_allCriteriaMet() {
            // 10 días > 5.95 (tiempo OK); ICF 55 > 40 (umbral W1); 8/10 tareas = 80% > 60%
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(milestoneRepository.findByCode("W1")).thenReturn(Optional.of(milestoneW1));
            when(evaluationRepository.findByFamilyIdAndMilestoneKeyAndStatus(1L, "W1", EvaluationStatus.FINALIZED))
                    .thenReturn(List.of(eval(55.0, "MODERADO")));
            when(planTaskRepository.countByFamilyIdAndMilestoneCode(1L, "W1")).thenReturn(10L);
            when(planTaskRepository.countCompletedByFamilyIdAndMilestoneCode(1L, "W1")).thenReturn(8L);

            MilestoneService.AdvancementEvaluation result = service.evaluate(1L);

            assertThat(result.timeMet()).isTrue();
            assertThat(result.icfMet()).isTrue();
            assertThat(result.tasksMet()).isTrue();
            assertThat(result.canAdvance()).isTrue();
            assertThat(result.currentMilestone()).isEqualTo("W1");
        }

        @Test
        @DisplayName("sin tareas en el hito → ratio = 1.0, tasksMet = true")
        void evaluate_noTasks_tasksMetIsTrue() {
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(milestoneRepository.findByCode("W1")).thenReturn(Optional.of(milestoneW1));
            when(evaluationRepository.findByFamilyIdAndMilestoneKeyAndStatus(1L, "W1", EvaluationStatus.FINALIZED))
                    .thenReturn(List.of(eval(55.0, "BAJO")));
            when(planTaskRepository.countByFamilyIdAndMilestoneCode(1L, "W1")).thenReturn(0L);
            when(planTaskRepository.countCompletedByFamilyIdAndMilestoneCode(1L, "W1")).thenReturn(0L);

            MilestoneService.AdvancementEvaluation result = service.evaluate(1L);

            assertThat(result.tasksMet()).isTrue();
            assertThat(result.completionRatio()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("hito legado nulo → se normaliza a W1")
        void evaluate_nullMilestoneNormalizesToW1() {
            family.setCurrentMilestone(null);

            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(milestoneRepository.findByCode("W1")).thenReturn(Optional.of(milestoneW1));
            when(evaluationRepository.findByFamilyIdAndMilestoneKeyAndStatus(1L, "W1", EvaluationStatus.FINALIZED))
                    .thenReturn(List.of());
            when(planTaskRepository.countByFamilyIdAndMilestoneCode(1L, "W1")).thenReturn(0L);
            when(planTaskRepository.countCompletedByFamilyIdAndMilestoneCode(1L, "W1")).thenReturn(0L);

            MilestoneService.AdvancementEvaluation result = service.evaluate(1L);

            assertThat(result.currentMilestone()).isEqualTo("W1");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  advanceMilestone()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("advanceMilestone() — avance efectivo de hito")
    class AdvanceMilestone {

        @Test
        @DisplayName("familia en M36 → retorna M36 sin hacer cambios")
        void advanceMilestone_terminal_noChange() {
            Family m36 = Family.builder()
                    .id(2L).name("Terminal").currentMilestone("M36").build();

            when(familyRepository.findById(2L)).thenReturn(Optional.of(m36));

            String result = service.advanceMilestone(2L);

            assertThat(result).isEqualTo("M36");
            verify(familyRepository, never()).save(any());
        }

        @Test
        @DisplayName("criterios no cumplidos → retorna el hito actual sin cambios")
        void advanceMilestone_blocked_noChange() {
            // Tiempo insuficiente (2 días) → no avanza
            family.setMilestoneStartedAt(LocalDateTime.now().minusDays(2));

            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(milestoneRepository.findByCode("W1")).thenReturn(Optional.of(milestoneW1));
            when(evaluationRepository.findByFamilyIdAndMilestoneKeyAndStatus(1L, "W1", EvaluationStatus.FINALIZED))
                    .thenReturn(List.of());
            when(planTaskRepository.countByFamilyIdAndMilestoneCode(1L, "W1")).thenReturn(0L);
            when(planTaskRepository.countCompletedByFamilyIdAndMilestoneCode(1L, "W1")).thenReturn(0L);

            String result = service.advanceMilestone(1L);

            assertThat(result).isEqualTo("W1");
            verify(familyRepository, never()).save(any());
        }

        @Test
        @DisplayName("todos los criterios cumplidos → avanza W1 → M1 y persiste la familia")
        void advanceMilestone_advances_w1_to_m1() {
            // 10 días OK; ICF 55 > 40; 8/10 tareas 80% > 60%
            when(familyRepository.findById(1L))
                    .thenReturn(Optional.of(family))  // 1ª llamada en evaluate()
                    .thenReturn(Optional.of(family));  // 2ª llamada en doAdvance()
            when(milestoneRepository.findByCode("W1")).thenReturn(Optional.of(milestoneW1));
            when(evaluationRepository.findByFamilyIdAndMilestoneKeyAndStatus(1L, "W1", EvaluationStatus.FINALIZED))
                    .thenReturn(List.of(
                            Evaluation.builder().id(1L).status(EvaluationStatus.FINALIZED)
                                    .milestoneKey("W1").icf(55.0).riskLevel("MODERADO")
                                    .finalizedAt(LocalDateTime.now().minusDays(1)).build()
                    ));
            when(planTaskRepository.countByFamilyIdAndMilestoneCode(1L, "W1")).thenReturn(10L);
            when(planTaskRepository.countCompletedByFamilyIdAndMilestoneCode(1L, "W1")).thenReturn(8L);
            when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));

            String result = service.advanceMilestone(1L);

            assertThat(result).isEqualTo("M1");
            assertThat(family.getCurrentMilestone()).isEqualTo("M1");
            assertThat(family.getMilestoneStartedAt()).isNotNull();
            verify(familyRepository).save(family);
        }
    }
}
