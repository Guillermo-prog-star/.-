package com.integrityfamily.plan.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

/**
 * SDD: Data Transfer Objects for Improvement Plans.
 * Ensures isolation from JPA lazy-loading mechanisms.
 */
public class PlanDtos {

    @Builder
    public record PlanTaskStepResponse(
            Long id,
            String type,
            String detail,
            boolean completed
    ) {}

    @Builder
    public record PlanTaskResponse(
            Long id,
            String title,
            String description,
            String dimension,
            LocalDateTime dueDate,
            int periodicityMonths,
            Long milestoneId,
            String milestoneCode,
            Long assignedMemberId,
            String assignedMemberName,
            boolean completed,
            List<PlanTaskStepResponse> steps,
            String fase,
            String riesgoAsociado,
            String objetivo,
            String accionConcreta,
            String indicadorCumplimiento,
            String evidenciaRequerida,
            Integer impactoIcf
    ) {}

    @Builder
    public record PlanResponse(
            Long id,
            Long familyId,
            Long evaluationId,
            String title,
            String description,
            String vision3y,
            String aiReport,
            LocalDateTime aiGeneratedAt,
            List<PlanTaskResponse> tasks
    ) {}

    public record TaskCompleteRequest(Boolean completed) {}
}
