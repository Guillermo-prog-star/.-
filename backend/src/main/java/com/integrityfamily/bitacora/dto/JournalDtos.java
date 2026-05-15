package com.integrityfamily.bitacora.dto;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.Builder;

public class JournalDtos {

    public record EvidenceUploadRequest(
        String evidenceType, // PHOTO, TEXT, AUDIO, etc.
        String title,
        String description,
        String fileUrl,
        String textContent,
        String submittedBy
    ) {}

    public record ReflectionCreateRequest(
        Long taskId,
        Long familyId,
        Integer emotionalImpact,
        Boolean communicationImproved,
        String difficulty,
        String learning,
        Boolean repeatIntent,
        String submittedBy
    ) {}

    public record JournalCreateRequest(
        Long familyId,
        String origin, // RISK, PLAN, TASK, CRISIS
        String riskDimension,
        String emotion,
        Long relatedTaskId,
        Integer moodAfter,
        String complianceStatus,
        String title,
        String reflection,
        String learning,
        String observations
    ) {}

    @Builder
    public record TimelineEntryDto(
        String entryType, // EVIDENCE, REFLECTION, LEARNING, JOURNAL
        Long id,
        String title,
        String description,
        LocalDateTime timestamp,
        Map<String, Object> metadata
    ) {}

    @Builder
    public record LongitudinalMetricsDto(
        Double adherenceRate, // tareas completadas / asignadas (ej. 0.85)
        Integer activeMembersCount,
        Integer completedReflectionsCount,
        Double emotionalEvolutionScore, // score actual - previo
        Integer persistenceWeeks
    ) {}
}
