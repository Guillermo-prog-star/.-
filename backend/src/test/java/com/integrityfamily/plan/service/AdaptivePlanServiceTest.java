package com.integrityfamily.plan.service;

import com.integrityfamily.analytics.dto.ConvivenceAnalyticsDto.OperativeDashboardResponse;
import com.integrityfamily.analytics.service.ConvivenceAnalyticsService;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import com.integrityfamily.plan.dto.AdaptivePlanDtos.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class AdaptivePlanServiceTest {

    @Mock
    private ImprovementPlanRepository planRepository;

    @Mock
    private PlanAdjustmentRepository planAdjustmentRepository;

    @Mock
    private PlanTaskRepository planTaskRepository;

    @Mock
    private TaskEvidenceRepository taskEvidenceRepository;

    @Mock
    private AiInferenceRepository aiInferenceRepository;

    @Mock
    private FamilyMetricsSnapshotRepository snapshotRepository;

    @Mock
    private ConvivenceAnalyticsService analyticsService;

    @InjectMocks
    private AdaptivePlanService adaptivePlanService;

    private Family family;
    private ImprovementPlan plan;
    private PlanTask task1;

    @BeforeEach
    void setUp() {
        family = Family.builder().id(1L).name("Familia Lopez").build();
        task1 = PlanTask.builder()
                .id(10L)
                .title("Misión de Escucha")
                .description("Actividad base")
                .periodicityMonths(1)
                .dueDate(LocalDateTime.now().plusDays(5))
                .completed(false)
                .impactoIcf(4) // Tarea no crítica (< 5)
                .build();

        List<PlanTask> tasks = new ArrayList<>();
        tasks.add(task1);

        plan = ImprovementPlan.builder()
                .id(100L)
                .family(family)
                .title("Plan Familiar")
                .tasks(tasks)
                .build();
    }

    @Test
    @DisplayName("Regla 1: Proponer ajuste por baja adherencia (< 40%) reduciendo la carga al 50%")
    void shouldProposeLoadReductionWhenAdherenceIsLow() {
        Mockito.when(planRepository.findByFamilyId(1L)).thenReturn(List.of(plan));

        OperativeDashboardResponse dash = OperativeDashboardResponse.builder()
                .familyId(1L)
                .adherenceRate(35.0) // < 40%
                .communicationScore(75.0)
                .build();
        Mockito.when(analyticsService.getOperativeDashboard(1L)).thenReturn(dash);

        Mockito.when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(1L)).thenReturn(List.of());
        Mockito.when(taskEvidenceRepository.findAll()).thenReturn(List.of());
        Mockito.when(aiInferenceRepository.findFirstByFamilyIdOrderByCreatedAtDesc(1L)).thenReturn(null);

        Mockito.when(planAdjustmentRepository.save(any(PlanAdjustment.class))).thenAnswer(invocation -> {
            PlanAdjustment adj = invocation.getArgument(0);
            adj.setId(500L);
            return adj;
        });

        ProposeAdjustmentRequest req = new ProposeAdjustmentRequest("ALERTA_ADHERENCIA", null);
        PlanAdjustmentResponse response = adaptivePlanService.proposeAdjustment(1L, req);

        assertNotNull(response);
        assertEquals(500L, response.id());
        assertEquals("REDUCE_LOAD", response.adjustmentType());
        assertTrue(response.reason().contains("Adherencia crítica"));
        assertEquals("PROPOSED", response.status());
    }

    @Test
    @DisplayName("Regla 2: Proponer reinicio suave ante inactividad prolongada (>= 14 días)")
    void shouldProposeSoftResetWhenInactivityExceeds14Days() {
        Mockito.when(planRepository.findByFamilyId(1L)).thenReturn(List.of(plan));

        OperativeDashboardResponse dash = OperativeDashboardResponse.builder()
                .familyId(1L)
                .adherenceRate(85.0)
                .communicationScore(80.0)
                .build();
        Mockito.when(analyticsService.getOperativeDashboard(1L)).thenReturn(dash);

        Mockito.when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(1L)).thenReturn(List.of());

        // Simular evidencia antigua de hace 20 días
        TaskEvidence oldEvidence = TaskEvidence.builder()
                .id(1L)
                .family(family)
                .createdAt(LocalDateTime.now().minusDays(20))
                .build();
        Mockito.when(taskEvidenceRepository.findAll()).thenReturn(List.of(oldEvidence));

        Mockito.when(aiInferenceRepository.findFirstByFamilyIdOrderByCreatedAtDesc(1L)).thenReturn(null);
        Mockito.when(planAdjustmentRepository.save(any(PlanAdjustment.class))).thenAnswer(i -> {
            PlanAdjustment adj = i.getArgument(0);
            adj.setId(501L);
            return adj;
        });

        PlanAdjustmentResponse response = adaptivePlanService.proposeAdjustment(1L, new ProposeAdjustmentRequest("ALERTA_INACTIVIDAD", null));

        assertNotNull(response);
        assertEquals(501L, response.id());
        assertEquals("SOFT_RESET", response.adjustmentType());
        assertTrue(response.reason().contains("Inactividad prolongada"));
    }

    @Test
    @DisplayName("Regla 3: Proponer escucha breve guiada ante caída histórica en comunicación (> 15 pts)")
    void shouldProposeGuidedListeningWhenCommunicationDropsOver15Points() {
        Mockito.when(planRepository.findByFamilyId(1L)).thenReturn(List.of(plan));

        OperativeDashboardResponse dash = OperativeDashboardResponse.builder()
                .familyId(1L)
                .adherenceRate(90.0)
                .communicationScore(60.0) // Puntaje actual 60
                .build();
        Mockito.when(analyticsService.getOperativeDashboard(1L)).thenReturn(dash);

        // Simular historial anterior donde la comunicación estaba en 80 (caída de 20 pts > 15)
        FamilyMetricsSnapshot pastSnap = FamilyMetricsSnapshot.builder()
                .familyId(1L)
                .snapshotDate(LocalDate.now().minusDays(10))
                .communicationScore(80.0)
                .build();
        Mockito.when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(1L)).thenReturn(List.of(pastSnap));

        // Simular evidencia reciente para no disparar inactividad
        TaskEvidence recentEvidence = TaskEvidence.builder()
                .id(2L)
                .family(family)
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();
        Mockito.when(taskEvidenceRepository.findAll()).thenReturn(List.of(recentEvidence));

        Mockito.when(aiInferenceRepository.findFirstByFamilyIdOrderByCreatedAtDesc(1L)).thenReturn(null);
        Mockito.when(planAdjustmentRepository.save(any(PlanAdjustment.class))).thenAnswer(i -> {
            PlanAdjustment adj = i.getArgument(0);
            adj.setId(502L);
            return adj;
        });

        PlanAdjustmentResponse response = adaptivePlanService.proposeAdjustment(1L, new ProposeAdjustmentRequest("ALERTA_DETERIORO", null));

        assertNotNull(response);
        assertEquals(502L, response.id());
        assertEquals("GUIDED_LISTENING", response.adjustmentType());
        assertTrue(response.reason().contains("Caída severa en comunicación familiar"));
    }

    @Test
    @DisplayName("Regla 4: Proponer pausa de tareas no críticas ante más del 50% de tareas vencidas")
    void shouldProposePauseNonCriticalWhenOverdueExceeds50Percent() {
        // Configurar tarea como vencida
        task1.setDueDate(LocalDateTime.now().minusDays(3)); // 100% de tareas vencidas

        Mockito.when(planRepository.findByFamilyId(1L)).thenReturn(List.of(plan));

        OperativeDashboardResponse dash = OperativeDashboardResponse.builder()
                .familyId(1L)
                .adherenceRate(90.0)
                .communicationScore(85.0)
                .build();
        Mockito.when(analyticsService.getOperativeDashboard(1L)).thenReturn(dash);

        Mockito.when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(1L)).thenReturn(List.of());

        TaskEvidence recentEvidence = TaskEvidence.builder()
                .id(3L)
                .family(family)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();
        Mockito.when(taskEvidenceRepository.findAll()).thenReturn(List.of(recentEvidence));

        Mockito.when(aiInferenceRepository.findFirstByFamilyIdOrderByCreatedAtDesc(1L)).thenReturn(null);
        Mockito.when(planAdjustmentRepository.save(any(PlanAdjustment.class))).thenAnswer(i -> {
            PlanAdjustment adj = i.getArgument(0);
            adj.setId(503L);
            return adj;
        });

        PlanAdjustmentResponse response = adaptivePlanService.proposeAdjustment(1L, new ProposeAdjustmentRequest("ALERTA_VENCIMIENTOS", null));

        assertNotNull(response);
        assertEquals(503L, response.id());
        assertEquals("PAUSE_NON_CRITICAL", response.adjustmentType());
        assertTrue(response.reason().contains("Saturación operativa"));
    }

    @Test
    @DisplayName("Flujo Aprobación: Aprobar ajuste propuesto exitosamente")
    void shouldApproveAdjustmentSuccessfully() {
        PlanAdjustment prop = PlanAdjustment.builder()
                .id(500L)
                .familyPlan(plan)
                .adjustmentType("REDUCE_LOAD")
                .reason("Razón inicial")
                .status(AdjustmentStatus.PROPOSED)
                .createdAt(LocalDateTime.now())
                .build();

        Mockito.when(planAdjustmentRepository.findById(500L)).thenReturn(Optional.of(prop));
        Mockito.when(planAdjustmentRepository.save(any(PlanAdjustment.class))).thenAnswer(i -> i.getArgument(0));

        AdjustmentApprovalRequest req = new AdjustmentApprovalRequest("Consejo Familiar", "Aprobado por unanimidad");
        PlanAdjustmentResponse response = adaptivePlanService.approveAdjustment(500L, req);

        assertNotNull(response);
        assertEquals("APPROVED", response.status());
        assertEquals("Consejo Familiar", response.approvedBy());
    }

    @Test
    @DisplayName("Flujo Aplicación: Aplicar mutaciones en el plan activo (pausa de tareas no críticas)")
    void shouldApplyAdjustmentSuccessfully() {
        MissionAdjustment ma = MissionAdjustment.builder()
                .id(1L)
                .task(task1)
                .action("PAUSE_NON_CRITICAL")
                .build();

        PlanAdjustment app = PlanAdjustment.builder()
                .id(500L)
                .familyPlan(plan)
                .adjustmentType("PAUSE_NON_CRITICAL")
                .status(AdjustmentStatus.APPROVED)
                .missionAdjustments(List.of(ma))
                .build();

        Mockito.when(planAdjustmentRepository.findById(500L)).thenReturn(Optional.of(app));
        Mockito.when(planTaskRepository.save(any(PlanTask.class))).thenAnswer(i -> i.getArgument(0));
        Mockito.when(planAdjustmentRepository.save(any(PlanAdjustment.class))).thenAnswer(i -> i.getArgument(0));

        PlanAdjustmentResponse response = adaptivePlanService.applyAdjustment(500L);

        assertNotNull(response);
        assertEquals("APPLIED", response.status());
        assertTrue(task1.getDescription().contains("[PAUSADA: Tarea no crítica suspendida"));
    }
}
