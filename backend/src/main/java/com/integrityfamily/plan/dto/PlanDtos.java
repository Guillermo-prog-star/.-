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
            Integer impactoIcf,
            String pillarName,
            String memberType,
            String riskType,
            String missionGenerator
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

    @Builder
    public record AiMissionProposal(
            String dimension,
            String riskLevel,
            String problemDetected,
            String objective,
            String missionType,
            List<String> targetMembers,
            String frequency,
            int estimatedDuration,
            String successMetric,
            String adaptiveReason,
            String title,
            String description
    ) {}

    // --- Contrato IA (Rediseño 6.4) ---
    
    public record IaStep(
            String type,
            String detail
    ) {}

    public record IaTask(
            String title,
            String dimension,
            List<IaStep> steps,
            @com.fasterxml.jackson.annotation.JsonProperty("pillar_name") String pillarName,
            @com.fasterxml.jackson.annotation.JsonProperty("milestone_code") String milestoneCode,
            @com.fasterxml.jackson.annotation.JsonProperty("member_type") String memberType,
            @com.fasterxml.jackson.annotation.JsonProperty("risk_type") String riskType,
            @com.fasterxml.jackson.annotation.JsonProperty("mission_generator") String missionGenerator
    ) {}

    public record IaMilestone(
            String code,
            String objective,
            @com.fasterxml.jackson.annotation.JsonProperty("micro_actions") List<IaTask> tasks
    ) {}

    public record IaPlanResponse(
            String vision_3y,
            List<IaMilestone> milestones
    ) {}

    public record TaskCompleteRequest(Boolean completed) {}

    public record PlanGenerateRequest(Long evaluationId) {}
}
