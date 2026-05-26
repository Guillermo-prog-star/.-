package com.integrityfamily.plan.service;

import com.integrityfamily.analytics.dto.ConvivenceAnalyticsDto.OperativeDashboardResponse;
import com.integrityfamily.analytics.service.ConvivenceAnalyticsService;
import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import com.integrityfamily.plan.dto.AdaptivePlanDtos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * SDD Sprint 7: Servicio de Planes Adaptativos IA (AdaptivePlanService).
 * Implementación estricta de las 4 reglas de negocio de ajuste dinámico.
 */
@Slf4j
@Service("legacyAdaptivePlanService")
@RequiredArgsConstructor
public class AdaptivePlanService {

    private final ImprovementPlanRepository planRepository;
    private final PlanAdjustmentRepository planAdjustmentRepository;
    private final PlanTaskRepository planTaskRepository;
    private final TaskEvidenceRepository taskEvidenceRepository;
    private final AiInferenceRepository aiInferenceRepository;
    private final FamilyMetricsSnapshotRepository snapshotRepository;
    private final ConvivenceAnalyticsService analyticsService;

    @Transactional
    public PlanAdjustmentResponse proposeAdjustment(Long familyId, ProposeAdjustmentRequest req) {
        log.info("🤖 [ADAPTIVE ENGINE] Proponiendo ajuste adaptativo para familia ID: {}, origen: {}", familyId, req != null ? req.triggerSource() : "AUTO");

        // 1. Obtener plan activo de la familia
        List<ImprovementPlan> plans = planRepository.findByFamilyId(familyId);
        if (plans.isEmpty()) {
            throw new BusinessException("No se encontró un plan de mejora activo para la familia ID: " + familyId, "PLAN_NOT_FOUND", HttpStatus.NOT_FOUND);
        }
        ImprovementPlan activePlan = plans.get(plans.size() - 1);

        // 2. Obtener métricas operativas del motor analítico
        OperativeDashboardResponse dash = analyticsService.getOperativeDashboard(familyId);
        double adherence = dash.adherenceRate();
        double currentCommScore = dash.communicationScore();

        // 3. Evaluar caída histórica en comunicación (> 15 pts)
        List<FamilyMetricsSnapshot> history = snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(familyId);
        double maxPastCommScore = currentCommScore;
        for (FamilyMetricsSnapshot snap : history) {
            if (snap.getCommunicationScore() != null && snap.getCommunicationScore() > maxPastCommScore) {
                maxPastCommScore = snap.getCommunicationScore();
            }
        }
        double communicationDrop = maxPastCommScore - currentCommScore;

        // 4. Obtener días de inactividad
        List<TaskEvidence> evidences = taskEvidenceRepository.findAll().stream()
                .filter(e -> e.getFamily() != null && e.getFamily().getId().equals(familyId))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();

        int inactiveDays = 15;
        if (!evidences.isEmpty()) {
            inactiveDays = (int) ChronoUnit.DAYS.between(evidences.get(0).getCreatedAt(), LocalDateTime.now());
        }

        // 5. Evaluar tareas vencidas
        List<PlanTask> tasks = activePlan.getTasks() != null ? activePlan.getTasks() : new ArrayList<>();
        long totalTasks = tasks.size();
        long overdueTasks = tasks.stream()
                .filter(t -> !t.isCompleted() && t.getDueDate() != null && t.getDueDate().isBefore(LocalDateTime.now()))
                .count();
        double overduePercentage = totalTasks > 0 ? ((double) overdueTasks / totalTasks) * 100.0 : 0.0;

        // 6. Motor de Reglas Adaptativas (Reglas Exactas de Negocio)
        String adjustmentType = "MAINTAIN";
        String reason = "Métricas estables. Manteniendo progresión actual del plan sin alteraciones.";
        String action = "MAINTAIN";

        // Regla 1: Si adherencia < 40% -> Reducir frecuencia 50%
        if (adherence < 40.0) {
            adjustmentType = "REDUCE_LOAD";
            reason = String.format("Regla 1: Adherencia crítica (%.1f%% < 40%%). Reduciendo frecuencia de tareas al 50%% para mitigar sobrecarga familiar.", adherence);
            action = "REDUCE_FREQUENCY";
        } 
        // Regla 2: Si inactividad >= 14 días -> Activar reinicio suave
        else if (inactiveDays >= 14) {
            adjustmentType = "SOFT_RESET";
            reason = String.format("Regla 2: Inactividad prolongada (>= %d días sin evidencias). Activando protocolo de reinicio suave con objetivos introductorios.", inactiveDays);
            action = "SOFT_RESET";
        } 
        // Regla 3: Si puntaje de comunicación cae > 15 pts -> Escucha breve guiada
        else if (communicationDrop > 15.0) {
            adjustmentType = "GUIDED_LISTENING";
            reason = String.format("Regla 3: Caída severa en comunicación familiar (deterioro de %.1f pts > 15 pts). Activando protocolo inmediato de escucha breve guiada.", communicationDrop);
            action = "GUIDED_LISTENING";
        } 
        // Regla 4: Si tareas vencidas > 50% -> Pausar tareas no críticas
        else if (overduePercentage > 50.0) {
            adjustmentType = "PAUSE_NON_CRITICAL";
            reason = String.format("Regla 4: Saturación operativa (%.1f%% de tareas vencidas > 50%%). Pausando tareas secundarias/no críticas para enfocar la energía familiar.", overduePercentage);
            action = "PAUSE_NON_CRITICAL";
        }

        if (req != null && req.customReason() != null && !req.customReason().isEmpty()) {
            reason = req.customReason();
        }

        // 7. Obtener o crear inferencia de respaldo
        AiInferenceEntity lastInference = aiInferenceRepository.findFirstByFamilyIdOrderByCreatedAtDesc(familyId);

        // 8. Ensamblar entidad PlanAdjustment
        PlanAdjustment adjustment = PlanAdjustment.builder()
                .familyPlan(activePlan)
                .sourceInference(lastInference)
                .adjustmentType(adjustmentType)
                .reason(reason)
                .status(AdjustmentStatus.PROPOSED)
                .createdAt(LocalDateTime.now())
                .missionAdjustments(new ArrayList<>())
                .build();

        // 9. Ensamblar mutaciones por misión (MissionAdjustment)
        for (PlanTask task : tasks) {
            String oldFreq = task.getPeriodicityMonths() > 0 ? task.getPeriodicityMonths() + " meses" : "1 mes";
            String newFreq = oldFreq;
            String oldDiff = "MODERADA";
            String newDiff = "MODERADA";
            String oldDue = task.getDueDate() != null ? task.getDueDate().toString() : LocalDateTime.now().plusDays(7).toString();
            String newDue = oldDue;
            String taskAction = action;

            if ("REDUCE_FREQUENCY".equals(action)) {
                newFreq = (task.getPeriodicityMonths() * 2) + " meses";
                newDiff = "BAJA";
            } else if ("PAUSE_NON_CRITICAL".equals(action)) {
                boolean isCritical = task.getImpactoIcf() != null && task.getImpactoIcf() >= 5;
                if (!isCritical) {
                    newFreq = "PAUSADA";
                }
            } else if ("SOFT_RESET".equals(action)) {
                newDue = LocalDateTime.now().plusDays(14).toString();
                newDiff = "INTRODUCTORIA";
            } else if ("GUIDED_LISTENING".equals(action)) {
                newDiff = "GUIADA_BREVE";
            }

            MissionAdjustment ma = MissionAdjustment.builder()
                    .planAdjustment(adjustment)
                    .task(task)
                    .action(taskAction)
                    .oldFrequency(oldFreq)
                    .newFrequency(newFreq)
                    .oldDifficulty(oldDiff)
                    .newDifficulty(newDiff)
                    .oldDueDate(oldDue)
                    .newDueDate(newDue)
                    .reason(reason)
                    .build();

            adjustment.getMissionAdjustments().add(ma);
        }

        planAdjustmentRepository.save(adjustment);
        log.info("✅ Ajuste adaptativo propuesto exitosamente con ID: {}", adjustment.getId());

        return mapToDto(adjustment);
    }

    @Transactional
    public PlanAdjustmentResponse approveAdjustment(Long adjustmentId, AdjustmentApprovalRequest req) {
        log.info("👍 [ADAPTIVE ENGINE] Aprobando ajuste adaptativo ID: {}", adjustmentId);
        PlanAdjustment adjustment = planAdjustmentRepository.findById(adjustmentId)
                .orElseThrow(() -> new BusinessException("Ajuste adaptativo no encontrado: " + adjustmentId, "ADJUSTMENT_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (adjustment.getStatus() != AdjustmentStatus.PROPOSED) {
            throw new BusinessException("El ajuste ID " + adjustmentId + " no puede aprobarse porque su estado es: " + adjustment.getStatus(), "INVALID_ADJUSTMENT_STATE", HttpStatus.CONFLICT);
        }

        adjustment.setStatus(AdjustmentStatus.APPROVED);
        adjustment.setApprovedAt(LocalDateTime.now());
        adjustment.setApprovedBy(req != null && req.approvedBy() != null ? req.approvedBy() : "Consejo de Familia");

        if (req != null && req.notes() != null && !req.notes().isEmpty()) {
            adjustment.setReason(adjustment.getReason() + " | Notas de aprobación: " + req.notes());
        }

        planAdjustmentRepository.save(adjustment);
        log.info("✅ Ajuste adaptativo ID {} aprobado por {}", adjustmentId, adjustment.getApprovedBy());

        return mapToDto(adjustment);
    }

    @Transactional
    public PlanAdjustmentResponse applyAdjustment(Long adjustmentId) {
        log.info("⚡ [ADAPTIVE ENGINE] Aplicando mutaciones de ajuste adaptativo ID: {}", adjustmentId);
        PlanAdjustment adjustment = planAdjustmentRepository.findById(adjustmentId)
                .orElseThrow(() -> new BusinessException("Ajuste adaptativo no encontrado: " + adjustmentId, "ADJUSTMENT_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (adjustment.getStatus() != AdjustmentStatus.APPROVED && adjustment.getStatus() != AdjustmentStatus.PROPOSED) {
            throw new BusinessException("El ajuste ID " + adjustmentId + " no puede aplicarse desde el estado: " + adjustment.getStatus(), "INVALID_ADJUSTMENT_STATE", HttpStatus.CONFLICT);
        }

        adjustment.setStatus(AdjustmentStatus.APPLIED);

        // Aplicar cambios reales en las tareas del plan
        if (adjustment.getMissionAdjustments() != null) {
            for (MissionAdjustment ma : adjustment.getMissionAdjustments()) {
                PlanTask task = ma.getTask();
                if (task != null) {
                    if ("PAUSE_NON_CRITICAL".equals(ma.getAction())) {
                        boolean isCritical = task.getImpactoIcf() != null && task.getImpactoIcf() >= 5;
                        if (!isCritical) {
                            task.setDescription(task.getDescription() + " [PAUSADA: Tarea no crítica suspendida por exceso de tareas vencidas]");
                        }
                    } else if ("REDUCE_FREQUENCY".equals(ma.getAction())) {
                        task.setPeriodicityMonths(task.getPeriodicityMonths() * 2);
                        task.setDescription(task.getDescription() + " [FRECUENCIA REDUCIDA AL 50% por baja adherencia]");
                    } else if ("SOFT_RESET".equals(ma.getAction())) {
                        task.setDueDate(LocalDateTime.now().plusDays(14));
                        task.setDescription(task.getDescription() + " [REINICIO SUAVE: Fecha extendida por inactividad]");
                    } else if ("GUIDED_LISTENING".equals(ma.getAction())) {
                        task.setDescription(task.getDescription() + " [PROTOCOLO ESCUCHA BREVE: Aplicar pausa reflexiva de 5 minutos antes de responder]");
                    }
                    planTaskRepository.save(task);
                }
            }
        }

        planAdjustmentRepository.save(adjustment);
        log.info("✅ Mutaciones aplicadas exitosamente en el plan familiar.");

        return mapToDto(adjustment);
    }

    @Transactional(readOnly = true)
    public List<PlanAdjustmentResponse> getFamilyAdjustments(Long familyId) {
        log.info("📋 [ADAPTIVE ENGINE] Consultando historial de ajustes para familia ID: {}", familyId);
        return planAdjustmentRepository.findByFamilyPlanFamilyIdOrderByCreatedAtDesc(familyId).stream()
                .map(this::mapToDto)
                .toList();
    }

    private PlanAdjustmentResponse mapToDto(PlanAdjustment entity) {
        List<MissionAdjustmentResponse> missions = entity.getMissionAdjustments() == null ? new ArrayList<>() :
                entity.getMissionAdjustments().stream()
                        .map(ma -> MissionAdjustmentResponse.builder()
                                .id(ma.getId())
                                .taskId(ma.getTask() != null ? ma.getTask().getId() : null)
                                .taskTitle(ma.getTask() != null ? ma.getTask().getTitle() : "Misión General")
                                .action(ma.getAction())
                                .oldFrequency(ma.getOldFrequency())
                                .newFrequency(ma.getNewFrequency())
                                .oldDifficulty(ma.getOldDifficulty())
                                .newDifficulty(ma.getNewDifficulty())
                                .oldDueDate(ma.getOldDueDate())
                                .newDueDate(ma.getNewDueDate())
                                .reason(ma.getReason())
                                .build())
                        .toList();

        return PlanAdjustmentResponse.builder()
                .id(entity.getId())
                .familyPlanId(entity.getFamilyPlan() != null ? entity.getFamilyPlan().getId() : null)
                .sourceInferenceId(entity.getSourceInference() != null ? entity.getSourceInference().getId() : null)
                .adjustmentType(entity.getAdjustmentType())
                .reason(entity.getReason())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .createdAt(entity.getCreatedAt())
                .approvedAt(entity.getApprovedAt())
                .approvedBy(entity.getApprovedBy())
                .missionAdjustments(missions)
                .build();
    }
}
