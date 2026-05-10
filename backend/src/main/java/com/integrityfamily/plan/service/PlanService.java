package com.integrityfamily.plan.service;

import com.integrityfamily.domain.ImprovementPlan;
import com.integrityfamily.domain.PlanTask;
import com.integrityfamily.domain.PlanTaskStep;
import com.integrityfamily.domain.FamilyLogbookEntry;
import com.integrityfamily.domain.LogbookStatus;
import com.integrityfamily.plan.dto.PlanDtos.*;
import com.integrityfamily.domain.repository.ImprovementPlanRepository;
import com.integrityfamily.domain.repository.PlanTaskRepository;
import com.integrityfamily.domain.repository.FamilyLogbookEntryRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SDD: Servicio de Planificación Harmonizado.
 * Gestiona el ciclo de vida de los planes de transformación familiar.
 * Integra un motor de Auto-Evidencias [Sentinel Auto-Evidence Engine].
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlanService {

    private final ImprovementPlanRepository planRepository;
    private final PlanTaskRepository planTaskRepository;
    private final FamilyLogbookEntryRepository logbookEntryRepository;

    @Transactional(readOnly = true)
    public List<PlanResponse> findAllPlans() {
        return planRepository.findAll().stream().map(this::toPlanResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> findByFamilyId(Long familyId) {
        return planRepository.findByFamilyId(familyId).stream().map(this::toPlanResponse).toList();
    }

    @Transactional(readOnly = true)
    public PlanResponse findPlanById(Long id) {
        ImprovementPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan no encontrado: " + id));
        return toPlanResponse(plan);
    }

    @Transactional
    public PlanResponse createPlan(ImprovementPlan plan) {
        return toPlanResponse(planRepository.save(plan));
    }

    @Transactional
    public PlanResponse updatePlan(Long id, ImprovementPlan request) {
        ImprovementPlan existing = planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan no encontrado: " + id));
        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        return toPlanResponse(planRepository.save(existing));
    }

    @Transactional
    public void deletePlan(Long id) {
        planRepository.deleteById(id);
    }

    // --- Tareas ---

    @Transactional(readOnly = true)
    public List<PlanTaskResponse> findAllTasks() {
        return planTaskRepository.findAll().stream().map(this::toTaskResponse).toList();
    }

    @Transactional(readOnly = true)
    public PlanTaskResponse findTaskById(Long id) {
        PlanTask task = planTaskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada: " + id));
        return toTaskResponse(task);
    }

    @Transactional
    public PlanTaskResponse createTask(PlanTask task) {
        return toTaskResponse(planTaskRepository.save(task));
    }

    @Transactional
    public PlanTaskResponse updateTask(Long id, PlanTask request) {
        PlanTask existing = planTaskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada: " + id));
        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        existing.setCompleted(request.isCompleted());
        return toTaskResponse(planTaskRepository.save(existing));
    }

    @Transactional
    public PlanTaskResponse completeTask(Long id, boolean completed) {
        PlanTask task = planTaskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada: " + id));
        task.setCompleted(completed);
        
        PlanTask savedTask = planTaskRepository.save(task);

        // [SDD SPEC: Sentinel Auto-Evidence Engine]
        // Si la tarea se marca como completada, generamos automáticamente su evidencia en la bitácora familiar (logbook)
        if (completed && task.getPlan() != null && task.getPlan().getFamily() != null) {
            try {
                com.integrityfamily.domain.Family family = task.getPlan().getFamily();
                
                String situation = "Ejecución exitosa de la Misión Clínica: " + task.getTitle();
                String difficultyDetected = task.getRiesgoAsociado() != null && !task.getRiesgoAsociado().isBlank() ? 
                        task.getRiesgoAsociado() : "Prevención de riesgos en la dimensión " + (task.getDimension() != null ? task.getDimension() : "INTEGRIDAD");
                String emotionIdentified = task.getDimension() != null && !task.getDimension().isBlank() ? 
                        task.getDimension() : "EMOCIONES";
                String understanding = task.getObjetivo() != null && !task.getObjetivo().isBlank() ? 
                        task.getObjetivo() : "La familia comprende la importancia de consolidar este hábito de cambio.";
                String correctionAction = task.getAccionConcreta() != null && !task.getAccionConcreta().isBlank() ? 
                        task.getAccionConcreta() : "Realización de las actividades pautadas en el plan.";
                String familyAgreement = task.getIndicadorCumplimiento() != null && !task.getIndicadorCumplimiento().isBlank() ? 
                        task.getIndicadorCumplimiento() : "Sostener el hábito en las prácticas cotidianas.";
                
                String evidenceDesc = String.format(
                        "[EVIDENCIA AUTOMÁTICA SENTINEL v4.5]: Microacción completada con éxito. Requisito de evidencia cumplido: '%s'. Impacto de +%d puntos sumados al ICF.",
                        task.getEvidenciaRequerida() != null && !task.getEvidenciaRequerida().isBlank() ? task.getEvidenciaRequerida() : "Registro clínico de plan de acción",
                        task.getImpactoIcf() != null ? task.getImpactoIcf() : 10
                );

                FamilyLogbookEntry evidenceEntry = FamilyLogbookEntry.builder()
                        .family(family)
                        .situation(situation)
                        .difficultyDetected(difficultyDetected)
                        .emotionIdentified(emotionIdentified)
                        .understanding(understanding)
                        .correctionAction(correctionAction)
                        .familyAgreement(familyAgreement)
                        .progressEvidence(evidenceDesc)
                        .status(LogbookStatus.RESOLVED)
                        .createdBy("SENTINEL_AUTO_AI")
                        .resolvedBy("SENTINEL_AUTO_AI")
                        .createdAt(LocalDateTime.now())
                        .resolvedAt(LocalDateTime.now())
                        .build();

                logbookEntryRepository.save(evidenceEntry);
                log.info("🎯 [AUTO-EVIDENCE] Entrada de bitácora registrada automáticamente para la tarea ID: {}", id);
            } catch (Exception e) {
                log.error("❌ Error en generación automática de evidencia: {}", e.getMessage());
            }
        }

        return toTaskResponse(savedTask);
    }

    @Transactional
    public void deleteTask(Long id) {
        planTaskRepository.deleteById(id);
    }

    // --- Mappers SDD ---

    private PlanResponse toPlanResponse(ImprovementPlan plan) {
        if (plan == null) return null;
        
        List<PlanTaskResponse> tasks = plan.getTasks() == null ? List.of() :
                plan.getTasks().stream().map(this::toTaskResponse).toList();

        return PlanResponse.builder()
                .id(plan.getId())
                .familyId(plan.getFamily() != null ? plan.getFamily().getId() : null)
                .evaluationId(plan.getEvaluation() != null ? plan.getEvaluation().getId() : null)
                .title(plan.getTitle())
                .description(plan.getDescription())
                .vision3y(plan.getVision3y())
                .aiReport(plan.getAiReport())
                .aiGeneratedAt(plan.getAiGeneratedAt())
                .tasks(tasks)
                .build();
    }

    private PlanTaskResponse toTaskResponse(PlanTask task) {
        if (task == null) return null;

        List<PlanTaskStepResponse> steps = task.getSteps() == null ? List.of() :
                task.getSteps().stream().map(this::toStepResponse).toList();

        return PlanTaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .dimension(task.getDimension())
                .dueDate(task.getDueDate())
                .periodicityMonths(task.getPeriodicityMonths())
                .milestoneId(task.getMilestone() != null ? task.getMilestone().getId() : null)
                .milestoneCode(task.getMilestone() != null ? task.getMilestone().getCode() : null)
                .assignedMemberId(task.getResponsible() != null ? task.getResponsible().getId() : null)
                .assignedMemberName(task.getResponsible() != null ? task.getResponsible().getFullName() : null)
                .completed(task.isCompleted())
                .steps(steps)
                .fase(task.getFase())
                .riesgoAsociado(task.getRiesgoAsociado())
                .objetivo(task.getObjetivo())
                .accionConcreta(task.getAccionConcreta())
                .indicadorCumplimiento(task.getIndicadorCumplimiento())
                .evidenciaRequerida(task.getEvidenciaRequerida())
                .impactoIcf(task.getImpactoIcf())
                .build();
    }

    private PlanTaskStepResponse toStepResponse(PlanTaskStep step) {
        if (step == null) return null;
        return PlanTaskStepResponse.builder()
                .id(step.getId())
                .type(step.getType() != null ? step.getType().name() : null)
                .detail(step.getDetail())
                .completed(step.isCompleted())
                .build();
    }
}
