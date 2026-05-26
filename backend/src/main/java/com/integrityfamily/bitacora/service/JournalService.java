package com.integrityfamily.bitacora.service;

import com.integrityfamily.bitacora.dto.JournalDtos.*;
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
import java.util.*;

/**
 * SDD Sprint 4: Servicio de Aprendizaje Familiar Longitudinal (Evidencias y Bitácora).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JournalService {

    private final PlanTaskRepository planTaskRepository;
    private final FamilyRepository familyRepository;
    private final TaskEvidenceRepository taskEvidenceRepository;
    private final ReflectionRepository reflectionRepository;
    private final LearningEntryRepository learningEntryRepository;
    private final JournalEntryRepository journalEntryRepository;

    @Transactional
    public TaskEvidence uploadEvidence(Long taskId, EvidenceUploadRequest req) {
        log.info("📸 [EVIDENCE] Subiendo evidencia para Tarea ID: {}", taskId);
        PlanTask task = planTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("Tarea no encontrada: " + taskId, "TASK_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (task.getPlan() == null || task.getPlan().getFamily() == null) {
            throw new BusinessException("La tarea no tiene un plan o familia asociada.", "TASK_NO_FAMILY", HttpStatus.BAD_REQUEST);
        }
        Family family = task.getPlan().getFamily();

        EvidenceType type;
        try {
            type = EvidenceType.valueOf(req.evidenceType().toUpperCase());
        } catch (Exception e) {
            type = EvidenceType.PHOTO;
        }

        TaskEvidence evidence = TaskEvidence.builder()
                .task(task)
                .family(family)
                .evidenceType(type)
                .status(EvidenceStatus.SUBMITTED)
                .title(req.title() != null ? req.title() : "Evidencia de " + task.getTitle())
                .description(req.description())
                .fileUrl(req.fileUrl())
                .textContent(req.textContent())
                .submittedBy(req.submittedBy())
                .validated(false)
                .createdAt(LocalDateTime.now())
                .build();

        return taskEvidenceRepository.save(evidence);
    }

    @Transactional
    public Reflection createReflection(ReflectionCreateRequest req) {
        log.info("🧠 [REFLECTION] Creando reflexión guiada para Tarea ID: {}", req.taskId());
        PlanTask task = planTaskRepository.findById(req.taskId())
                .orElseThrow(() -> new BusinessException("Tarea no encontrada: " + req.taskId(), "TASK_NOT_FOUND", HttpStatus.NOT_FOUND));

        Family family = familyRepository.findById(req.familyId())
                .orElseThrow(() -> new BusinessException("Familia no encontrada: " + req.familyId(), "FAMILY_NOT_FOUND", HttpStatus.NOT_FOUND));

        Reflection reflection = Reflection.builder()
                .task(task)
                .family(family)
                .emotionalImpact(req.emotionalImpact())
                .communicationImproved(req.communicationImproved())
                .difficulty(req.difficulty())
                .learning(req.learning())
                .repeatIntent(req.repeatIntent())
                .status(ReflectionStatus.COMPLETED)
                .submittedBy(req.submittedBy())
                .createdAt(LocalDateTime.now())
                .build();
        reflection = reflectionRepository.save(reflection);

        // SDD SPEC: Generar aprendizaje estructurado automáticamente
        String behavioralChange = String.format("La familia completó la misión '%s' reportando un impacto emocional de %d/5. ¿Mejoró comunicación?: %s. Dificultad reportada: %s.",
                task.getTitle(), req.emotionalImpact(), Boolean.TRUE.equals(req.communicationImproved()) ? "Sí" : "No", req.difficulty());
        String awarenessShift = String.format("Aprendizaje expresado: '%s'. Intención de repetición: %s.",
                req.learning(), Boolean.TRUE.equals(req.repeatIntent()) ? "Alta" : "Moderada");

        LearningEntry learning = LearningEntry.builder()
                .family(family)
                .task(task)
                .behavioralChange(behavioralChange)
                .awarenessShift(awarenessShift)
                .createdAt(LocalDateTime.now())
                .build();
        learningEntryRepository.save(learning);

        // SDD SPEC: Generar entrada en la bitácora híbrida
        JournalEntry journal = JournalEntry.builder()
                .family(family)
                .origin(JournalOrigin.TASK)
                .riskDimension(task.getDimension() != null ? task.getDimension() : "COMUNICACION")
                .emotion("Transformación Consciente")
                .relatedTask(task)
                .moodAfter(req.emotionalImpact())
                .complianceStatus("COMPLETED")
                .title("Reflexión y Aprendizaje: " + task.getTitle())
                .reflection(req.difficulty())
                .learning(req.learning())
                .observations("Generado automáticamente tras completar reflexión guiada.")
                .status(JournalStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
        journalEntryRepository.save(journal);

        log.info("✅ Reflexión, aprendizaje y bitácora registrados con éxito.");
        return reflection;
    }

    @Transactional
    public JournalEntry createJournal(JournalCreateRequest req) {
        log.info("📖 [JOURNAL] Registrando entrada de bitácora para familia ID: {}", req.familyId());
        Family family = familyRepository.findById(req.familyId())
                .orElseThrow(() -> new BusinessException("Familia no encontrada: " + req.familyId(), "FAMILY_NOT_FOUND", HttpStatus.NOT_FOUND));

        PlanTask task = null;
        if (req.relatedTaskId() != null) {
            task = planTaskRepository.findById(req.relatedTaskId()).orElse(null);
        }

        JournalOrigin origin;
        try {
            origin = JournalOrigin.valueOf(req.origin().toUpperCase());
        } catch (Exception e) {
            origin = JournalOrigin.TASK;
        }

        JournalEntry journal = JournalEntry.builder()
                .family(family)
                .origin(origin)
                .riskDimension(req.riskDimension())
                .emotion(req.emotion())
                .relatedTask(task)
                .moodAfter(req.moodAfter())
                .complianceStatus(req.complianceStatus())
                .title(req.title())
                .reflection(req.reflection())
                .learning(req.learning())
                .observations(req.observations())
                .status(JournalStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        return journalEntryRepository.save(journal);
    }

    @Transactional(readOnly = true)
    public List<TimelineEntryDto> getTimeline(Long familyId) {
        List<TimelineEntryDto> timeline = new ArrayList<>();

        // Evidencias
        List<TaskEvidence> evidences = taskEvidenceRepository.findAll().stream()
                .filter(e -> e.getFamily() != null && e.getFamily().getId().equals(familyId))
                .toList();
        for (TaskEvidence e : evidences) {
            timeline.add(TimelineEntryDto.builder()
                    .entryType("EVIDENCE")
                    .id(e.getId())
                    .title(e.getTitle())
                    .description(e.getDescription() != null ? e.getDescription() : "Evidencia subida (" + e.getEvidenceType() + ")")
                    .timestamp(e.getCreatedAt())
                    .metadata(Map.of("status", e.getStatus().name(), "type", e.getEvidenceType().name()))
                    .build());
        }

        // Reflexiones
        List<Reflection> reflections = reflectionRepository.findByFamilyId(familyId);
        for (Reflection r : reflections) {
            timeline.add(TimelineEntryDto.builder()
                    .entryType("REFLECTION")
                    .id(r.getId())
                    .title("Reflexión: " + (r.getTask() != null ? r.getTask().getTitle() : "General"))
                    .description(r.getLearning())
                    .timestamp(r.getCreatedAt())
                    .metadata(Map.of("emotionalImpact", r.getEmotionalImpact() != null ? r.getEmotionalImpact() : 3, "communicationImproved", Boolean.TRUE.equals(r.getCommunicationImproved())))
                    .build());
        }

        // Aprendizajes
        List<LearningEntry> learnings = learningEntryRepository.findByFamilyId(familyId);
        for (LearningEntry l : learnings) {
            timeline.add(TimelineEntryDto.builder()
                    .entryType("LEARNING")
                    .id(l.getId())
                    .title("Aprendizaje Consolidado")
                    .description(l.getAwarenessShift())
                    .timestamp(l.getCreatedAt())
                    .metadata(Map.of("behavioralChange", l.getBehavioralChange()))
                    .build());
        }

        // Bitácoras
        List<JournalEntry> journals = journalEntryRepository.findByFamilyIdOrderByCreatedAtDesc(familyId);
        for (JournalEntry j : journals) {
            timeline.add(TimelineEntryDto.builder()
                    .entryType("JOURNAL")
                    .id(j.getId())
                    .title(j.getTitle())
                    .description(j.getReflection() != null ? j.getReflection() : j.getObservations())
                    .timestamp(j.getCreatedAt())
                    .metadata(Map.of("origin", j.getOrigin().name(), "moodAfter", j.getMoodAfter() != null ? j.getMoodAfter() : 3))
                    .build());
        }

        // Ordenar descendente
        timeline.sort((a, b) -> b.timestamp().compareTo(a.timestamp()));
        return timeline;
    }

    @Transactional(readOnly = true)
    public LongitudinalMetricsDto getMetrics(Long familyId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new BusinessException("Familia no encontrada: " + familyId, "FAMILY_NOT_FOUND", HttpStatus.NOT_FOUND));

        // Calcular adherencia
        List<PlanTask> familyTasks = planTaskRepository.findAll().stream()
                .filter(t -> t.getPlan() != null && t.getPlan().getFamily() != null && t.getPlan().getFamily().getId().equals(familyId))
                .toList();

        long totalTasks = familyTasks.size();
        long completedTasks = familyTasks.stream().filter(PlanTask::isCompleted).count();
        double adherence = totalTasks > 0 ? (double) completedTasks / totalTasks : 1.0;

        // Miembros activos
        int activeMembers = family.getMembers() != null ? family.getMembers().size() : 2;

        // Reflexiones activas
        int reflectionsCount = reflectionRepository.findByFamilyId(familyId).size();

        // Evolución emocional (score actual - previo)
        List<JournalEntry> journals = journalEntryRepository.findByFamilyIdOrderByCreatedAtDesc(familyId);
        double emotionalEvolution = 0.0;
        if (journals.size() >= 2) {
            Integer latestMood = journals.get(0).getMoodAfter();
            Integer oldestMood = journals.get(journals.size() - 1).getMoodAfter();
            if (latestMood != null && oldestMood != null) {
                emotionalEvolution = (double) (latestMood - oldestMood);
            }
        }

        // Semanas consecutivas de persistencia
        int persistenceWeeks = 1;
        if (!journals.isEmpty()) {
            LocalDateTime oldest = journals.get(journals.size() - 1).getCreatedAt();
            long weeks = ChronoUnit.WEEKS.between(oldest, LocalDateTime.now());
            persistenceWeeks = (int) Math.max(1, weeks + 1);
        }

        return LongitudinalMetricsDto.builder()
                .adherenceRate(adherence)
                .activeMembersCount(activeMembers)
                .completedReflectionsCount(reflectionsCount)
                .emotionalEvolutionScore(emotionalEvolution)
                .persistenceWeeks(persistenceWeeks)
                .build();
    }
}
