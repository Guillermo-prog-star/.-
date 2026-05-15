package com.integrityfamily.checklist.service;

import com.integrityfamily.domain.TaskEvidence;
import com.integrityfamily.domain.EvidenceType;
import com.integrityfamily.domain.EvidenceStatus;
import com.integrityfamily.domain.PlanTask;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.TaskEvidenceRepository;
import com.integrityfamily.domain.repository.PlanTaskRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.integrityfamily.common.config.RabbitConfig;
import com.integrityfamily.common.event.SystemEvent;
import java.time.LocalDateTime;
import java.util.List;

/**
 * SDD SPEC 6.6: Servicio de Negocio para el Ciclo de Vida de las Evidencias Conductuales.
 */
@Service
@RequiredArgsConstructor
public class TaskEvidenceService {

    private static final Logger log = LoggerFactory.getLogger(TaskEvidenceService.class);

    private final TaskEvidenceRepository taskEvidenceRepository;
    private final PlanTaskRepository planTaskRepository;
    private final FamilyRepository familyRepository;
    private final RabbitTemplate rabbitTemplate;

    public List<TaskEvidence> getFamilyEvidences(Long familyId) {
        return taskEvidenceRepository.findByFamilyId(familyId);
    }

    public List<TaskEvidence> getEvidencesByTask(Long taskId) {
        return taskEvidenceRepository.findByTaskId(taskId);
    }

    @Transactional
    public TaskEvidence submitEvidence(Long taskId, Long familyId, EvidenceType type, String title, 
                                        String description, String fileUrl, String textContent, String submittedBy) {
        
        PlanTask task = planTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Tarea de Plan no encontrada con ID: " + taskId));
        
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new IllegalArgumentException("Familia no encontrada con ID: " + familyId));

        TaskEvidence evidence = TaskEvidence.builder()
                .task(task)
                .family(family)
                .evidenceType(type)
                .status(EvidenceStatus.SUBMITTED)
                .title(title)
                .description(description)
                .fileUrl(fileUrl)
                .textContent(textContent)
                .submittedBy(submittedBy)
                .createdAt(LocalDateTime.now())
                .validated(false)
                .build();

        // Actualizar el estado de la tarea en concordancia si corresponde
        // NOTA: Se marca completada al validar la evidencia
        log.info("📥 [EVIDENCE] Nueva evidencia multimodal '{}' subida por '{}' para la tarea ID '{}'", 
                title, submittedBy, taskId);

        TaskEvidence saved = taskEvidenceRepository.save(evidence);

        // Publicar evento en RabbitMQ para disparar el procesamiento asíncrono cognitivo con Claude
        try {
            SystemEvent event = SystemEvent.of("evidence.submitted", familyId, saved.getId(), submittedBy);
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, "evidence.submitted", event);
            log.info("📢 [EVIDENCE-EVENT] Evento 'evidence.submitted' publicado para Evidencia ID '{}'", saved.getId());
        } catch (Exception e) {
            log.error("⚠️ [EVIDENCE-EVENT] Error al publicar evento de evidencia para ID {}: {}", saved.getId(), e.getMessage());
        }

        return saved;
    }

    @Transactional
    public TaskEvidence validateEvidence(Long evidenceId, Double score, String validatorName) {
        TaskEvidence evidence = taskEvidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new IllegalArgumentException("Evidencia no encontrada con ID: " + evidenceId));

        evidence.validate(score, validatorName);
        TaskEvidence saved = taskEvidenceRepository.save(evidence);

        // Al validar la evidencia, marcamos la tarea asociada como completada
        PlanTask task = evidence.getTask();
        task.setCompleted(true);
        planTaskRepository.save(task);

        log.info("🛡️ [VALIDATION] Evidencia ID '{}' aprobada con score '{}' por '{}'. Tarea ID '{}' completada.", 
                evidenceId, score, validatorName, task.getId());

        return saved;
    }

    @Transactional
    public TaskEvidence rejectEvidence(Long evidenceId, String validatorName) {
        TaskEvidence evidence = taskEvidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new IllegalArgumentException("Evidencia no encontrada con ID: " + evidenceId));

        evidence.setStatus(EvidenceStatus.REJECTED);
        evidence.setValidated(false);
        evidence.setValidatedAt(LocalDateTime.now());
        
        // La tarea vuelve a estar incompleta si se rechaza su única evidencia
        PlanTask task = evidence.getTask();
        task.setCompleted(false);
        planTaskRepository.save(task);

        log.warn("❌ [REJECTION] Evidencia ID '{}' rechazada por '{}'. Tarea ID '{}' marcada como incompleta.", 
                evidenceId, validatorName, task.getId());

        return taskEvidenceRepository.save(evidence);
    }
}
