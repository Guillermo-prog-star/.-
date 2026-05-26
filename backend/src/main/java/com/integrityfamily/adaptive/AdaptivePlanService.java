package com.integrityfamily.adaptive;

import com.integrityfamily.analytics.dto.ConvivenceAnalyticsDto.OperativeDashboardResponse;
import com.integrityfamily.analytics.service.ConvivenceAnalyticsService;
import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SDD Sprint 8: Servicio de Integración Adaptativa Real (AdaptivePlanService).
 * Conecta el motor determinístico con adaptive_adjustments, mutaciones de PlanTask y auditoría en Bitácora.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdaptivePlanService {

    private final FamilyRepository familyRepository;
    private final ImprovementPlanRepository planRepository;
    private final PlanTaskRepository planTaskRepository;
    private final TaskEvidenceRepository taskEvidenceRepository;
    private final FamilyMetricsSnapshotRepository snapshotRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final AdaptiveAdjustmentRepository adaptiveAdjustmentRepository;
    private final ConvivenceAnalyticsService analyticsService;

    /**
     * Motor determinístico puro en memoria.
     */
    public List<AdaptiveAdjustment> evaluate(AdaptivePlanContext context) {
        List<AdaptiveAdjustment> adjustments = new ArrayList<>();

        if (context.adherencePercent() < 40.0) {
            adjustments.add(new AdaptiveAdjustment(
                    context.familyId(),
                    AdaptiveRuleType.REDUCE_LOAD,
                    "Adherencia menor al 40%. Se propone reducir la carga aumentando el espaciamiento de las misiones."
            ));
        }

        if (context.inactivityDays() >= 14) {
            adjustments.add(new AdaptiveAdjustment(
                    context.familyId(),
                    AdaptiveRuleType.SOFT_RESET,
                    "Inactividad igual o superior a 14 días. Se propone reinicio suave con tareas introductorias."
            ));
        }

        if (context.communicationDrop() > 15) {
            adjustments.add(new AdaptiveAdjustment(
                    context.familyId(),
                    AdaptiveRuleType.GUIDED_LISTENING,
                    "Caída de comunicación superior a 15 puntos. Se propone escucha breve guiada."
            ));
        }

        if (context.overdueTasksPercent() > 50.0) {
            adjustments.add(new AdaptiveAdjustment(
                    context.familyId(),
                    AdaptiveRuleType.PAUSE_NON_CRITICAL,
                    "Más del 50% de tareas vencidas. Se propone pausar tareas no críticas."
            ));
        }

        return adjustments;
    }

    /**
     * Construye el contexto inmutable consultando la base de datos real.
     */
    @Transactional(readOnly = true)
    public AdaptivePlanContext buildContext(Long familyId) {
        log.info("📊 [ADAPTIVE ENGINE] Construyendo contexto inmutable para familia ID: {}", familyId);
        
        OperativeDashboardResponse dash = analyticsService.getOperativeDashboard(familyId);
        double adherence = dash.adherenceRate();
        double currentCommScore = dash.communicationScore();

        List<FamilyMetricsSnapshot> history = snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(familyId);
        double maxPastCommScore = currentCommScore;
        for (FamilyMetricsSnapshot snap : history) {
            if (snap.getCommunicationScore() != null && snap.getCommunicationScore() > maxPastCommScore) {
                maxPastCommScore = snap.getCommunicationScore();
            }
        }

        List<TaskEvidence> evidences = taskEvidenceRepository.findAll().stream()
                .filter(e -> e.getFamily() != null && e.getFamily().getId().equals(familyId))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();

        int inactiveDays = 15;
        if (!evidences.isEmpty()) {
            inactiveDays = (int) ChronoUnit.DAYS.between(evidences.get(0).getCreatedAt(), LocalDateTime.now());
        }

        List<ImprovementPlan> plans = planRepository.findByFamilyId(familyId);
        long totalTasks = 0;
        long overdueTasks = 0;

        if (!plans.isEmpty()) {
            ImprovementPlan activePlan = plans.get(plans.size() - 1);
            List<PlanTask> tasks = activePlan.getTasks() != null ? activePlan.getTasks() : new ArrayList<>();
            totalTasks = tasks.size();
            overdueTasks = tasks.stream()
                    .filter(t -> !t.isCompleted() && t.getDueDate() != null && t.getDueDate().isBefore(LocalDateTime.now()))
                    .count();
        }

        double overduePercentage = totalTasks > 0 ? ((double) overdueTasks / totalTasks) * 100.0 : 0.0;

        return new AdaptivePlanContext(
                familyId,
                adherence,
                inactiveDays,
                (int) Math.round(maxPastCommScore),
                (int) Math.round(currentCommScore),
                overduePercentage
        );
    }

    /**
     * Integración Real: Evalúa para la familia y persiste las propuestas en adaptive_adjustments.
     */
    @Transactional
    public List<AdaptiveAdjustmentEntity> evaluateAndProposeForFamily(Long familyId) {
        log.info("🤖 [ADAPTIVE ENGINE] Evaluando y persistiendo propuestas para familia ID: {}", familyId);
        AdaptivePlanContext context = buildContext(familyId);
        List<AdaptiveAdjustment> proposals = evaluate(context);

        List<AdaptiveAdjustmentEntity> entities = new ArrayList<>();
        for (AdaptiveAdjustment adj : proposals) {
            AdaptiveAdjustmentEntity entity = AdaptiveAdjustmentEntity.builder()
                    .familyId(familyId)
                    .ruleType(adj.getRuleType())
                    .reason(adj.getReason())
                    .status(AdjustmentStatus.PROPOSED)
                    .createdAt(LocalDateTime.now())
                    .build();

            entities.add(adaptiveAdjustmentRepository.save(entity));
        }

        log.info("✅ Generadas {} propuestas en adaptive_adjustments.", entities.size());
        return entities;
    }

    /**
     * Integración Real: Aprueba un ajuste persistido.
     */
    @Transactional
    public AdaptiveAdjustmentEntity approveAdjustment(UUID adjustmentId, String approvedBy) {
        log.info("👍 [ADAPTIVE ENGINE] Aprobando ajuste ID: {} por {}", adjustmentId, approvedBy);
        AdaptiveAdjustmentEntity entity = adaptiveAdjustmentRepository.findById(adjustmentId)
                .orElseThrow(() -> new BusinessException("Ajuste no encontrado: " + adjustmentId, "ADJUSTMENT_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (entity.getStatus() != AdjustmentStatus.PROPOSED) {
            throw new BusinessException("Solo los ajustes en estado PROPOSED pueden ser aprobados. Estado actual: " + entity.getStatus(), "ADJUSTMENT_INVALID_STATE", HttpStatus.CONFLICT);
        }

        entity.setStatus(AdjustmentStatus.APPROVED);
        entity.setApprovedAt(LocalDateTime.now());
        entity.setApprovedBy(approvedBy != null && !approvedBy.isEmpty() ? approvedBy : "Consejo de Familia");

        Family family = familyRepository.findById(entity.getFamilyId())
                .orElseThrow(() -> new BusinessException("Familia no encontrada: " + entity.getFamilyId(), "FAMILY_NOT_FOUND", HttpStatus.NOT_FOUND));

        JournalEntry auditEntry = JournalEntry.builder()
                .family(family)
                .origin(JournalOrigin.PLAN)
                .title("Aprobación Adaptativa: " + entity.getRuleType())
                .reflection("Aprobado ajuste al plan familiar. Razón: " + entity.getReason())
                .learning("La familia toma el control de sus ritmos de transformación.")
                .status(JournalStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        journalEntryRepository.save(auditEntry);
        return adaptiveAdjustmentRepository.save(entity);
    }

    /**
     * Integración Real: Aplica un ajuste aprobado sobre las misiones (PlanTask) y Bitácora.
     */
    @Transactional
    public AdaptiveAdjustmentEntity applyAdjustment(UUID adjustmentId) {
        log.info("⚡ [ADAPTIVE ENGINE] Aplicando mutaciones de ajuste ID: {}", adjustmentId);
        AdaptiveAdjustmentEntity entity = adaptiveAdjustmentRepository.findById(adjustmentId)
                .orElseThrow(() -> new BusinessException("Ajuste no encontrado: " + adjustmentId, "ADJUSTMENT_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (entity.getStatus() != AdjustmentStatus.APPROVED) {
            throw new BusinessException("Solo los ajustes en estado APPROVED pueden ser aplicados. Estado actual: " + entity.getStatus(), "ADJUSTMENT_INVALID_STATE", HttpStatus.CONFLICT);
        }

        entity.setStatus(AdjustmentStatus.APPLIED);
        entity.setAppliedAt(LocalDateTime.now());

        Family family = familyRepository.findById(entity.getFamilyId())
                .orElseThrow(() -> new BusinessException("Familia no encontrada: " + entity.getFamilyId(), "FAMILY_NOT_FOUND", HttpStatus.NOT_FOUND));

        List<ImprovementPlan> plans = planRepository.findByFamilyId(entity.getFamilyId());
        if (!plans.isEmpty()) {
            ImprovementPlan activePlan = plans.get(plans.size() - 1);

            // Conexión exacta con planes (SDD SPEC)
            if (entity.getRuleType() == AdaptiveRuleType.REDUCE_LOAD) {
                if (activePlan.getTasks() != null) {
                    for (PlanTask t : activePlan.getTasks()) {
                        t.setPeriodicityMonths(t.getPeriodicityMonths() * 2);
                        if (t.getDueDate() != null) {
                            t.setDueDate(t.getDueDate().plusDays(7));
                        }
                        t.setDescription(t.getDescription() + " [CARGA REDUCIDA: Frecuencia espaciada]");
                        planTaskRepository.save(t);
                    }
                }
            } else if (entity.getRuleType() == AdaptiveRuleType.SOFT_RESET) {
                PlanTask introTask = PlanTask.builder()
                        .plan(activePlan)
                        .title("Misión Introductoria: Reconexión Familiar")
                        .description("Actividad base de reinicio suave tras inactividad prolongada.")
                        .dueDate(LocalDateTime.now().plusDays(7))
                        .periodicityMonths(1)
                        .completed(false)
                        .impactoIcf(5)
                        .fase("1 semana")
                        .build();
                planTaskRepository.save(introTask);
            } else if (entity.getRuleType() == AdaptiveRuleType.GUIDED_LISTENING) {
                PlanTask listeningTask = PlanTask.builder()
                        .plan(activePlan)
                        .title("Misión de Escucha Guiada")
                        .description("Protocolo inmediato de escucha activa de 10 minutos sin interrupciones.")
                        .dueDate(LocalDateTime.now().plusDays(3))
                        .periodicityMonths(1)
                        .completed(false)
                        .impactoIcf(5)
                        .fase("1 semana")
                        .build();
                planTaskRepository.save(listeningTask);
            } else if (entity.getRuleType() == AdaptiveRuleType.PAUSE_NON_CRITICAL) {
                if (activePlan.getTasks() != null) {
                    for (PlanTask t : activePlan.getTasks()) {
                        boolean isCritical = t.getImpactoIcf() != null && t.getImpactoIcf() >= 5;
                        if (!isCritical) {
                            t.setDescription(t.getDescription() + " [PAUSADA TEMPORALMENTE: Tarea no crítica suspendida]");
                            planTaskRepository.save(t);
                        }
                    }
                }
            }
        }

        // Conexión exacta con bitácora (SDD SPEC)
        JournalEntry bitacoraEntry = JournalEntry.builder()
                .family(family)
                .origin(JournalOrigin.PLAN)
                .title("Ajuste Adaptativo Aplicado")
                .reflection("El sistema detectó baja adherencia y propuso reducir la carga del plan familiar. [Regla Ejecutada: " + entity.getRuleType() + "]")
                .learning("Optimización operativa del plan de transformación.")
                .status(JournalStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        journalEntryRepository.save(bitacoraEntry);
        log.info("✅ Mutación aplicada y entrada automática en bitácora registrada.");

        return adaptiveAdjustmentRepository.save(entity);
    }

    // Métodos de compatibilidad para pruebas unitarias en memoria
    public AdaptiveAdjustment approve(AdaptiveAdjustment adjustment) {
        adjustment.approve();
        return adjustment;
    }

    public AdaptiveAdjustment apply(AdaptiveAdjustment adjustment) {
        adjustment.apply();
        return adjustment;
    }
}
