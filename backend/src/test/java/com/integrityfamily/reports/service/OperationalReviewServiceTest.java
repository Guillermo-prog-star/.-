package com.integrityfamily.reports.service;

import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.ImprovementPlanRepository;
import com.integrityfamily.domain.repository.TaskEvidenceRepository;
import com.integrityfamily.reports.service.OperationalReviewService.FamilyReviewRow;
import com.integrityfamily.reports.service.OperationalReviewService.OperationalReview;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OperationalReviewService — Unit Tests")
class OperationalReviewServiceTest {

    @Mock
    ImprovementPlanRepository planRepository;

    @Mock
    TaskEvidenceRepository taskEvidenceRepository;

    @InjectMocks
    OperationalReviewService service;

    private static final LocalDate PERIOD_START = LocalDate.of(2026, 7, 27);
    private static final LocalDate PERIOD_END = LocalDate.of(2026, 8, 2);

    private Family family(Long id, String code) {
        return Family.builder().id(id).familyCode(code).name("Familia " + code).build();
    }

    private PlanTask task(Long id, LocalDateTime dueDate, boolean completed) {
        return PlanTask.builder().id(id).title("Mision " + id).dueDate(dueDate).completed(completed).build();
    }

    private ImprovementPlan planWith(Family family, List<PlanTask> tasks) {
        ImprovementPlan plan = ImprovementPlan.builder()
                .id(1L)
                .family(family)
                .acceptanceStatus(PlanAcceptanceStatus.ACCEPTED)
                .tasks(new ArrayList<>(tasks))
                .build();
        return plan;
    }

    @Nested
    @DisplayName("generate()")
    class Generate {

        @Test
        @DisplayName("solo incluye familias con plan ACCEPTED")
        void onlyIncludesAcceptedFamilies() {
            Family family = family(1L, "FAM-001");
            ImprovementPlan plan = planWith(family, List.of());
            when(planRepository.findByAcceptanceStatus(PlanAcceptanceStatus.ACCEPTED)).thenReturn(List.of(plan));
            when(taskEvidenceRepository.findByFamilyId(1L)).thenReturn(List.of());

            OperationalReview review = service.generate(PERIOD_START, PERIOD_END);

            assertThat(review.getFamilies()).hasSize(1);
            assertThat(review.getFamilies().get(0).getFamilyId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("adherencia del periodo actual = completadas / exigibles dentro del rango, ignora tareas fuera del periodo")
        void adherenceScopedToPeriod() {
            Family family = family(1L, "FAM-001");
            List<PlanTask> tasks = List.of(
                    task(1L, PERIOD_START.atStartOfDay().plusDays(1), true),
                    task(2L, PERIOD_START.atStartOfDay().plusDays(2), false),
                    task(3L, PERIOD_START.minusDays(30).atStartOfDay(), true) // fuera del periodo, no debe contar
            );
            ImprovementPlan plan = planWith(family, tasks);
            when(planRepository.findByAcceptanceStatus(PlanAcceptanceStatus.ACCEPTED)).thenReturn(List.of(plan));
            when(taskEvidenceRepository.findByFamilyId(1L)).thenReturn(List.of());

            OperationalReview review = service.generate(PERIOD_START, PERIOD_END);

            FamilyReviewRow row = review.getFamilies().get(0);
            assertThat(row.getMissionsPlanned()).isEqualTo(2);
            assertThat(row.getMissionsCompleted()).isEqualTo(1);
            assertThat(row.getAdherenceCurrent()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("adherencia null (no NaN/0) cuando no hay tareas exigibles en el periodo")
        void adherenceNullWhenNoTasksInWindow() {
            Family family = family(1L, "FAM-001");
            ImprovementPlan plan = planWith(family, List.of());
            when(planRepository.findByAcceptanceStatus(PlanAcceptanceStatus.ACCEPTED)).thenReturn(List.of(plan));
            when(taskEvidenceRepository.findByFamilyId(1L)).thenReturn(List.of());

            OperationalReview review = service.generate(PERIOD_START, PERIOD_END);

            assertThat(review.getFamilies().get(0).getAdherenceCurrent()).isNull();
        }

        @Test
        @DisplayName("calcula delta pp entre periodo actual y anterior de la misma duracion")
        void computesAdherenceDeltaAgainstPreviousPeriodOfSameDuration() {
            Family family = family(1L, "FAM-001");
            // Periodo actual (2026-07-27..2026-08-02): 1/2 completadas = 50%
            // Periodo anterior (misma duracion, 7 dias, termina el dia antes de PERIOD_START): 2/2 completadas = 100%
            LocalDateTime prevPeriodDay = PERIOD_START.minusDays(3).atStartOfDay();
            List<PlanTask> tasks = List.of(
                    task(1L, PERIOD_START.atStartOfDay().plusDays(1), true),
                    task(2L, PERIOD_START.atStartOfDay().plusDays(2), false),
                    task(3L, prevPeriodDay, true),
                    task(4L, prevPeriodDay.plusHours(1), true)
            );
            ImprovementPlan plan = planWith(family, tasks);
            when(planRepository.findByAcceptanceStatus(PlanAcceptanceStatus.ACCEPTED)).thenReturn(List.of(plan));
            when(taskEvidenceRepository.findByFamilyId(1L)).thenReturn(List.of());

            OperationalReview review = service.generate(PERIOD_START, PERIOD_END);

            FamilyReviewRow row = review.getFamilies().get(0);
            assertThat(row.getAdherenceCurrent()).isEqualTo(50.0);
            assertThat(row.getAdherencePrevious()).isEqualTo(100.0);
            assertThat(row.getAdherenceDeltaPp()).isEqualTo(-50.0);
            assertThat(review.getPreviousPeriodStart()).isEqualTo(PERIOD_START.minusDays(7));
            assertThat(review.getPreviousPeriodEnd()).isEqualTo(PERIOD_START.minusDays(1));
        }

        @Test
        @DisplayName("flags: NO_ACTIVE_MISSIONS y NO_RECENT_EVIDENCE cuando la familia no tiene ninguna en el periodo")
        void flagsNoMissionsAndNoEvidence() {
            Family family = family(1L, "FAM-001");
            ImprovementPlan plan = planWith(family, List.of());
            when(planRepository.findByAcceptanceStatus(PlanAcceptanceStatus.ACCEPTED)).thenReturn(List.of(plan));
            when(taskEvidenceRepository.findByFamilyId(1L)).thenReturn(List.of());

            OperationalReview review = service.generate(PERIOD_START, PERIOD_END);

            assertThat(review.getFamilies().get(0).getFlags())
                    .contains("NO_ACTIVE_MISSIONS", "NO_RECENT_EVIDENCE");
        }

        @Test
        @DisplayName("flags: LOW_ADHERENCE cuando adherencia actual < 50%")
        void flagsLowAdherence() {
            Family family = family(1L, "FAM-001");
            List<PlanTask> tasks = List.of(
                    task(1L, PERIOD_START.atStartOfDay().plusDays(1), false),
                    task(2L, PERIOD_START.atStartOfDay().plusDays(2), false),
                    task(3L, PERIOD_START.atStartOfDay().plusDays(3), true)
            );
            ImprovementPlan plan = planWith(family, tasks);
            when(planRepository.findByAcceptanceStatus(PlanAcceptanceStatus.ACCEPTED)).thenReturn(List.of(plan));
            when(taskEvidenceRepository.findByFamilyId(1L)).thenReturn(List.of());

            OperationalReview review = service.generate(PERIOD_START, PERIOD_END);

            FamilyReviewRow row = review.getFamilies().get(0);
            assertThat(row.getAdherenceCurrent()).isCloseTo(33.3, org.assertj.core.data.Offset.offset(0.1));
            assertThat(row.getFlags()).contains("LOW_ADHERENCE");
        }

        @Test
        @DisplayName("flags: OVERDUE_MISSIONS cuando hay tareas no completadas con fecha vencida")
        void flagsOverdueMissions() {
            Family family = family(1L, "FAM-001");
            // Tarea vencida: dueDate en el pasado respecto a "ahora", pero dentro del periodo de la prueba
            // usamos un periodo que incluye el pasado reciente para forzar overdue=true
            LocalDate start = LocalDate.now().minusDays(6);
            LocalDate end = LocalDate.now();
            List<PlanTask> tasks = List.of(
                    task(1L, LocalDateTime.now().minusDays(1), false)
            );
            ImprovementPlan plan = planWith(family, tasks);
            when(planRepository.findByAcceptanceStatus(PlanAcceptanceStatus.ACCEPTED)).thenReturn(List.of(plan));
            when(taskEvidenceRepository.findByFamilyId(1L)).thenReturn(List.of());

            OperationalReview review = service.generate(start, end);

            FamilyReviewRow row = review.getFamilies().get(0);
            assertThat(row.getMissionsOverdue()).isEqualTo(1);
            assertThat(row.getFlags()).contains("OVERDUE_MISSIONS");
        }

        @Test
        @DisplayName("evidencias recientes: solo cuenta las creadas dentro del periodo, ordenadas, con ultima fecha correcta")
        void recentEvidenceScopedToPeriod() {
            Family family = family(1L, "FAM-001");
            ImprovementPlan plan = planWith(family, List.of());
            when(planRepository.findByAcceptanceStatus(PlanAcceptanceStatus.ACCEPTED)).thenReturn(List.of(plan));

            LocalDateTime inPeriodEarly = PERIOD_START.atStartOfDay().plusDays(1);
            LocalDateTime inPeriodLate = PERIOD_START.atStartOfDay().plusDays(4);
            LocalDateTime outOfPeriod = PERIOD_START.minusDays(10).atStartOfDay();

            TaskEvidence e1 = TaskEvidence.builder().id(1L).family(family).createdAt(inPeriodEarly)
                    .evidenceType(EvidenceType.PHOTO).status(EvidenceStatus.VALIDATED).build();
            TaskEvidence e2 = TaskEvidence.builder().id(2L).family(family).createdAt(inPeriodLate)
                    .evidenceType(EvidenceType.DOCUMENT).status(EvidenceStatus.PENDING).build();
            TaskEvidence e3 = TaskEvidence.builder().id(3L).family(family).createdAt(outOfPeriod)
                    .evidenceType(EvidenceType.PHOTO).status(EvidenceStatus.VALIDATED).build();

            when(taskEvidenceRepository.findByFamilyId(1L)).thenReturn(List.of(e3, e1, e2));

            OperationalReview review = service.generate(PERIOD_START, PERIOD_END);

            FamilyReviewRow row = review.getFamilies().get(0);
            assertThat(row.getEvidenceCount()).isEqualTo(2);
            assertThat(row.getLastEvidenceAt()).isEqualTo(inPeriodLate);
            assertThat(review.getEvidences()).hasSize(2);
            assertThat(row.getFlags()).doesNotContain("NO_RECENT_EVIDENCE");
        }
    }
}
