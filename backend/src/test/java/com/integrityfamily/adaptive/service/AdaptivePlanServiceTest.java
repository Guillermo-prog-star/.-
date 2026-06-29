package com.integrityfamily.adaptive.service;

import com.integrityfamily.adaptive.AdaptiveAdjustment;
import com.integrityfamily.adaptive.AdaptiveAdjustmentEntity;
import com.integrityfamily.adaptive.AdaptiveAdjustmentRepository;
import com.integrityfamily.adaptive.AdaptivePlanContext;
import com.integrityfamily.adaptive.AdaptivePlanService;
import com.integrityfamily.adaptive.AdaptiveRuleType;
import com.integrityfamily.adaptive.AdjustmentStatus;
import com.integrityfamily.analytics.dto.ConvivenceAnalyticsDto.OperativeDashboardResponse;
import com.integrityfamily.analytics.service.ConvivenceAnalyticsService;
import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AdaptivePlanService — Unit Tests")
class AdaptivePlanServiceTest {

    @Mock FamilyRepository familyRepository;
    @Mock ImprovementPlanRepository planRepository;
    @Mock PlanTaskRepository planTaskRepository;
    @Mock TaskEvidenceRepository taskEvidenceRepository;
    @Mock FamilyMetricsSnapshotRepository snapshotRepository;
    @Mock JournalEntryRepository journalEntryRepository;
    @Mock AdaptiveAdjustmentRepository adaptiveAdjustmentRepository;
    @Mock ConvivenceAnalyticsService analyticsService;

    @InjectMocks AdaptivePlanService service;

    private static final Long FAM_ID = 7L;

    // ── helpers ──────────────────────────────────────────────────────────────

    private AdaptivePlanContext ctx(double adherence, int inactivity, int prevComm, int currComm, double overdue) {
        return new AdaptivePlanContext(FAM_ID, adherence, inactivity, prevComm, currComm, overdue);
    }

    private AdaptivePlanContext healthy() {
        return ctx(75.0, 3, 60, 62, 20.0);  // nada dispara
    }

    private AdaptiveAdjustmentEntity proposedEntity(AdaptiveRuleType ruleType) {
        return AdaptiveAdjustmentEntity.builder()
                .id(UUID.randomUUID())
                .familyId(FAM_ID)
                .ruleType(ruleType)
                .reason("test reason")
                .status(AdjustmentStatus.PROPOSED)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private AdaptiveAdjustmentEntity approvedEntity(AdaptiveRuleType ruleType) {
        AdaptiveAdjustmentEntity e = proposedEntity(ruleType);
        e.setStatus(AdjustmentStatus.APPROVED);
        e.setApprovedAt(LocalDateTime.now());
        return e;
    }

    private Family stubFamily() {
        Family family = new Family();
        family.setId(FAM_ID);
        when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
        return family;
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("evaluate() — motor determinístico")
    class EvaluateMethod {

        @Test
        @DisplayName("familia saludable no dispara ningún ajuste")
        void healthyContextProducesNoAdjustments() {
            assertThat(service.evaluate(healthy())).isEmpty();
        }

        @Test
        @DisplayName("adherencia < 40% dispara REDUCE_LOAD")
        void lowAdherenceTriggersReduceLoad() {
            List<AdaptiveAdjustment> result = service.evaluate(ctx(35.0, 3, 60, 60, 10.0));
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRuleType()).isEqualTo(AdaptiveRuleType.REDUCE_LOAD);
        }

        @Test
        @DisplayName("exactamente 40% de adherencia NO dispara REDUCE_LOAD")
        void adherenceAt40DoesNotTrigger() {
            List<AdaptiveAdjustment> result = service.evaluate(ctx(40.0, 3, 60, 60, 10.0));
            assertThat(result).noneMatch(a -> a.getRuleType() == AdaptiveRuleType.REDUCE_LOAD);
        }

        @Test
        @DisplayName("inactividad ≥ 14 días dispara SOFT_RESET")
        void inactivity14DaysTriggersReset() {
            List<AdaptiveAdjustment> result = service.evaluate(ctx(50.0, 14, 60, 60, 10.0));
            assertThat(result).anyMatch(a -> a.getRuleType() == AdaptiveRuleType.SOFT_RESET);
        }

        @Test
        @DisplayName("inactividad de 13 días NO dispara SOFT_RESET")
        void inactivity13DaysDoesNotTrigger() {
            List<AdaptiveAdjustment> result = service.evaluate(ctx(50.0, 13, 60, 60, 10.0));
            assertThat(result).noneMatch(a -> a.getRuleType() == AdaptiveRuleType.SOFT_RESET);
        }

        @Test
        @DisplayName("caída de comunicación > 15 puntos dispara GUIDED_LISTENING")
        void commDropOver15TriggersListening() {
            // prevComm=80, currComm=60 → drop=20
            List<AdaptiveAdjustment> result = service.evaluate(ctx(50.0, 3, 80, 60, 10.0));
            assertThat(result).anyMatch(a -> a.getRuleType() == AdaptiveRuleType.GUIDED_LISTENING);
        }

        @Test
        @DisplayName("caída de comunicación exactamente 15 NO dispara GUIDED_LISTENING")
        void commDropAt15DoesNotTrigger() {
            // drop=15, rule is > 15
            List<AdaptiveAdjustment> result = service.evaluate(ctx(50.0, 3, 75, 60, 10.0));
            assertThat(result).noneMatch(a -> a.getRuleType() == AdaptiveRuleType.GUIDED_LISTENING);
        }

        @Test
        @DisplayName("tareas vencidas > 50% dispara PAUSE_NON_CRITICAL")
        void overdueOver50TriggersPause() {
            List<AdaptiveAdjustment> result = service.evaluate(ctx(50.0, 3, 60, 60, 51.0));
            assertThat(result).anyMatch(a -> a.getRuleType() == AdaptiveRuleType.PAUSE_NON_CRITICAL);
        }

        @Test
        @DisplayName("tareas vencidas exactamente 50% NO dispara PAUSE_NON_CRITICAL")
        void overdueAt50DoesNotTrigger() {
            List<AdaptiveAdjustment> result = service.evaluate(ctx(50.0, 3, 60, 60, 50.0));
            assertThat(result).noneMatch(a -> a.getRuleType() == AdaptiveRuleType.PAUSE_NON_CRITICAL);
        }

        @Test
        @DisplayName("múltiples condiciones disparan múltiples ajustes")
        void multipleConditionsMultipleAdjustments() {
            // adherencia baja + inactividad + comm drop + overdue
            List<AdaptiveAdjustment> result = service.evaluate(ctx(30.0, 20, 90, 70, 60.0));
            assertThat(result).hasSize(4);
        }

        @Test
        @DisplayName("cada ajuste generado tiene familyId correcto")
        void adjustmentsHaveCorrectFamilyId() {
            List<AdaptiveAdjustment> result = service.evaluate(ctx(30.0, 3, 60, 60, 10.0));
            assertThat(result).allMatch(a -> FAM_ID.equals(a.getFamilyId()));
        }

        @Test
        @DisplayName("cada ajuste generado inicia en estado PROPOSED")
        void newAdjustmentsStartProposed() {
            List<AdaptiveAdjustment> result = service.evaluate(ctx(30.0, 3, 60, 60, 10.0));
            assertThat(result).allMatch(a -> a.getStatus() == AdjustmentStatus.PROPOSED);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("AdaptivePlanContext — validaciones del record")
    class ContextValidation {

        @Test
        @DisplayName("familyId null lanza IllegalArgumentException")
        void nullFamilyIdThrows() {
            assertThatThrownBy(() -> new AdaptivePlanContext(null, 50, 5, 60, 60, 20))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("adherencia fuera de rango lanza excepción")
        void adherenceOutOfRangeThrows() {
            assertThatThrownBy(() -> new AdaptivePlanContext(FAM_ID, 110, 5, 60, 60, 20))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("communicationDrop calcula correctamente la caída")
        void communicationDropCalculated() {
            AdaptivePlanContext ctx = ctx(50, 3, 80, 65, 10);
            assertThat(ctx.communicationDrop()).isEqualTo(15);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("AdaptiveAdjustment — máquina de estados en memoria")
    class InMemoryStateMachine {

        @Test
        @DisplayName("approve() cambia estado a APPROVED")
        void approveChangesState() {
            AdaptiveAdjustment adj = new AdaptiveAdjustment(FAM_ID, AdaptiveRuleType.REDUCE_LOAD, "test");
            service.approve(adj);
            assertThat(adj.getStatus()).isEqualTo(AdjustmentStatus.APPROVED);
        }

        @Test
        @DisplayName("approve() en APPROVED lanza IllegalStateException")
        void approveAlreadyApprovedThrows() {
            AdaptiveAdjustment adj = new AdaptiveAdjustment(FAM_ID, AdaptiveRuleType.REDUCE_LOAD, "test");
            adj.approve();
            assertThatThrownBy(adj::approve).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("apply() tras approve() cambia estado a APPLIED")
        void applyAfterApproveChangesState() {
            AdaptiveAdjustment adj = new AdaptiveAdjustment(FAM_ID, AdaptiveRuleType.SOFT_RESET, "test");
            adj.approve();
            service.apply(adj);
            assertThat(adj.getStatus()).isEqualTo(AdjustmentStatus.APPLIED);
        }

        @Test
        @DisplayName("apply() en estado PROPOSED lanza IllegalStateException")
        void applyWithoutApprovingThrows() {
            AdaptiveAdjustment adj = new AdaptiveAdjustment(FAM_ID, AdaptiveRuleType.SOFT_RESET, "test");
            assertThatThrownBy(adj::apply).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("reject() cambia estado a REJECTED")
        void rejectChangesState() {
            AdaptiveAdjustment adj = new AdaptiveAdjustment(FAM_ID, AdaptiveRuleType.PAUSE_NON_CRITICAL, "test");
            adj.reject();
            assertThat(adj.getStatus()).isEqualTo(AdjustmentStatus.REJECTED);
        }

        @Test
        @DisplayName("reject() en estado APPROVED lanza IllegalStateException")
        void rejectApprovedThrows() {
            AdaptiveAdjustment adj = new AdaptiveAdjustment(FAM_ID, AdaptiveRuleType.PAUSE_NON_CRITICAL, "test");
            adj.approve();
            assertThatThrownBy(adj::reject).isInstanceOf(IllegalStateException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("approveAdjustment() — flujo persistido")
    class ApproveAdjustmentMethod {

        @Test
        @DisplayName("aprueba ajuste PROPOSED y lo guarda con approvedBy")
        void approvesProposedEntity() {
            AdaptiveAdjustmentEntity entity = proposedEntity(AdaptiveRuleType.REDUCE_LOAD);
            when(adaptiveAdjustmentRepository.findById(entity.getId())).thenReturn(Optional.of(entity));
            when(adaptiveAdjustmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            stubFamily();
            when(journalEntryRepository.save(any())).thenReturn(null);

            AdaptiveAdjustmentEntity result = service.approveAdjustment(entity.getId(), "Mamá");

            assertThat(result.getStatus()).isEqualTo(AdjustmentStatus.APPROVED);
            assertThat(result.getApprovedBy()).isEqualTo("Mamá");
            assertThat(result.getApprovedAt()).isNotNull();
        }

        @Test
        @DisplayName("approvedBy null usa 'Consejo de Familia'")
        void nullApprovedByUsesDefault() {
            AdaptiveAdjustmentEntity entity = proposedEntity(AdaptiveRuleType.REDUCE_LOAD);
            when(adaptiveAdjustmentRepository.findById(entity.getId())).thenReturn(Optional.of(entity));
            when(adaptiveAdjustmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            stubFamily();
            when(journalEntryRepository.save(any())).thenReturn(null);

            AdaptiveAdjustmentEntity result = service.approveAdjustment(entity.getId(), null);

            assertThat(result.getApprovedBy()).isEqualTo("Consejo de Familia");
        }

        @Test
        @DisplayName("ajuste no encontrado lanza BusinessException")
        void notFoundThrowsBusinessException() {
            UUID id = UUID.randomUUID();
            when(adaptiveAdjustmentRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.approveAdjustment(id, "user"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("aprobar un ajuste ya APPROVED lanza BusinessException")
        void approveAlreadyApprovedThrows() {
            AdaptiveAdjustmentEntity entity = approvedEntity(AdaptiveRuleType.REDUCE_LOAD);
            when(adaptiveAdjustmentRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> service.approveAdjustment(entity.getId(), "user"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("genera entrada en bitácora al aprobar")
        void savesJournalEntry() {
            AdaptiveAdjustmentEntity entity = proposedEntity(AdaptiveRuleType.GUIDED_LISTENING);
            when(adaptiveAdjustmentRepository.findById(entity.getId())).thenReturn(Optional.of(entity));
            when(adaptiveAdjustmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            stubFamily();
            when(journalEntryRepository.save(any())).thenReturn(null);

            service.approveAdjustment(entity.getId(), "Papá");

            verify(journalEntryRepository).save(any(JournalEntry.class));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("rejectAdjustment() — rechazo de propuestas")
    class RejectAdjustmentMethod {

        @Test
        @DisplayName("rechaza ajuste PROPOSED y lo guarda con estado REJECTED")
        void rejectsProposedEntity() {
            AdaptiveAdjustmentEntity entity = proposedEntity(AdaptiveRuleType.SOFT_RESET);
            when(adaptiveAdjustmentRepository.findById(entity.getId())).thenReturn(Optional.of(entity));
            when(adaptiveAdjustmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            AdaptiveAdjustmentEntity result = service.rejectAdjustment(entity.getId());

            assertThat(result.getStatus()).isEqualTo(AdjustmentStatus.REJECTED);
        }

        @Test
        @DisplayName("rechazar un ajuste APPROVED lanza BusinessException")
        void rejectApprovedThrows() {
            AdaptiveAdjustmentEntity entity = approvedEntity(AdaptiveRuleType.SOFT_RESET);
            when(adaptiveAdjustmentRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> service.rejectAdjustment(entity.getId()))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("ID no encontrado lanza BusinessException")
        void notFoundThrows() {
            UUID id = UUID.randomUUID();
            when(adaptiveAdjustmentRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.rejectAdjustment(id))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("applyAdjustment() — aplicación de mutaciones")
    class ApplyAdjustmentMethod {

        private ImprovementPlan planWith(List<PlanTask> tasks) {
            ImprovementPlan plan = new ImprovementPlan();
            plan.setTasks(tasks);
            return plan;
        }

        private PlanTask taskWith(boolean completed, Integer impactoIcf, LocalDateTime dueDate) {
            PlanTask t = new PlanTask();
            t.setCompleted(completed);
            t.setImpactoIcf(impactoIcf);
            t.setDueDate(dueDate);
            t.setDescription("original");
            t.setPeriodicityMonths(1);
            return t;
        }

        @Test
        @DisplayName("aplica APPROVED y cambia estado a APPLIED")
        void appliesApprovedEntity() {
            AdaptiveAdjustmentEntity entity = approvedEntity(AdaptiveRuleType.REDUCE_LOAD);
            when(adaptiveAdjustmentRepository.findById(entity.getId())).thenReturn(Optional.of(entity));
            when(adaptiveAdjustmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            stubFamily();
            when(planRepository.findByFamilyId(FAM_ID)).thenReturn(List.of());
            when(journalEntryRepository.save(any())).thenReturn(null);

            AdaptiveAdjustmentEntity result = service.applyAdjustment(entity.getId());

            assertThat(result.getStatus()).isEqualTo(AdjustmentStatus.APPLIED);
            assertThat(result.getAppliedAt()).isNotNull();
        }

        @Test
        @DisplayName("aplicar ajuste PROPOSED lanza BusinessException")
        void applyProposedThrows() {
            AdaptiveAdjustmentEntity entity = proposedEntity(AdaptiveRuleType.REDUCE_LOAD);
            when(adaptiveAdjustmentRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> service.applyAdjustment(entity.getId()))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("REDUCE_LOAD duplica periodicidad y extiende dueDate en 7 días")
        void reduceLoadMutatesTasks() {
            AdaptiveAdjustmentEntity entity = approvedEntity(AdaptiveRuleType.REDUCE_LOAD);
            when(adaptiveAdjustmentRepository.findById(entity.getId())).thenReturn(Optional.of(entity));
            when(adaptiveAdjustmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            stubFamily();

            LocalDateTime due = LocalDateTime.now().plusDays(10);
            PlanTask task = taskWith(false, 3, due);
            task.setPeriodicityMonths(1);

            when(planRepository.findByFamilyId(FAM_ID)).thenReturn(List.of(planWith(List.of(task))));
            when(planTaskRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(journalEntryRepository.save(any())).thenReturn(null);

            service.applyAdjustment(entity.getId());

            assertThat(task.getPeriodicityMonths()).isEqualTo(2);
            assertThat(task.getDueDate()).isAfter(due);
            assertThat(task.getDescription()).contains("[CARGA REDUCIDA");
        }

        @Test
        @DisplayName("SOFT_RESET crea nueva PlanTask introductoria")
        void softResetCreatesIntroTask() {
            AdaptiveAdjustmentEntity entity = approvedEntity(AdaptiveRuleType.SOFT_RESET);
            when(adaptiveAdjustmentRepository.findById(entity.getId())).thenReturn(Optional.of(entity));
            when(adaptiveAdjustmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            stubFamily();
            when(planRepository.findByFamilyId(FAM_ID)).thenReturn(List.of(planWith(List.of())));
            when(planTaskRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(journalEntryRepository.save(any())).thenReturn(null);

            service.applyAdjustment(entity.getId());

            verify(planTaskRepository, atLeastOnce()).save(argThat(t ->
                    t.getTitle().contains("Reconexión") || t.getTitle().contains("Introductoria")
            ));
        }

        @Test
        @DisplayName("PAUSE_NON_CRITICAL solo pausa tareas con impacto < 5")
        void pauseNonCriticalSkipsCriticalTasks() {
            AdaptiveAdjustmentEntity entity = approvedEntity(AdaptiveRuleType.PAUSE_NON_CRITICAL);
            when(adaptiveAdjustmentRepository.findById(entity.getId())).thenReturn(Optional.of(entity));
            when(adaptiveAdjustmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            stubFamily();

            PlanTask critical  = taskWith(false, 5, null);
            PlanTask nonCritical = taskWith(false, 3, null);
            critical.setDescription("critica");
            nonCritical.setDescription("no critica");

            when(planRepository.findByFamilyId(FAM_ID))
                    .thenReturn(List.of(planWith(List.of(critical, nonCritical))));
            when(planTaskRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(journalEntryRepository.save(any())).thenReturn(null);

            service.applyAdjustment(entity.getId());

            assertThat(critical.getDescription()).doesNotContain("PAUSADA");
            assertThat(nonCritical.getDescription()).contains("PAUSADA");
        }

        @Test
        @DisplayName("siempre genera entrada en bitácora al aplicar")
        void alwaysSavesBitacoraEntry() {
            AdaptiveAdjustmentEntity entity = approvedEntity(AdaptiveRuleType.GUIDED_LISTENING);
            when(adaptiveAdjustmentRepository.findById(entity.getId())).thenReturn(Optional.of(entity));
            when(adaptiveAdjustmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            stubFamily();
            when(planRepository.findByFamilyId(FAM_ID)).thenReturn(List.of());
            when(journalEntryRepository.save(any())).thenReturn(null);

            service.applyAdjustment(entity.getId());

            verify(journalEntryRepository, atLeastOnce()).save(any(JournalEntry.class));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("listForFamily() — consulta de ajustes")
    class ListForFamily {

        @Test
        @DisplayName("delega en el repositorio y devuelve la lista")
        void delegatesToRepository() {
            AdaptiveAdjustmentEntity e1 = proposedEntity(AdaptiveRuleType.REDUCE_LOAD);
            AdaptiveAdjustmentEntity e2 = proposedEntity(AdaptiveRuleType.SOFT_RESET);
            when(adaptiveAdjustmentRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID))
                    .thenReturn(List.of(e1, e2));

            List<AdaptiveAdjustmentEntity> result = service.listForFamily(FAM_ID);

            assertThat(result).containsExactly(e1, e2);
        }

        @Test
        @DisplayName("devuelve lista vacía cuando no hay ajustes")
        void emptyListWhenNone() {
            when(adaptiveAdjustmentRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID))
                    .thenReturn(List.of());

            assertThat(service.listForFamily(FAM_ID)).isEmpty();
        }
    }
}
