package com.integrityfamily.plan.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

/**
 * SDD Sprint 7: Data Transfer Objects para el Bounded Context de Planes Adaptativos IA.
 * Garantiza el aislamiento de la capa de persistencia y la inmutabilidad de los contratos REST.
 */
public class AdaptivePlanDtos {

    @Builder
    public record MissionAdjustmentResponse(
            Long id,
            Long taskId,
            String taskTitle,
            String action,
            String oldFrequency,
            String newFrequency,
            String oldDifficulty,
            String newDifficulty,
            String oldDueDate,
            String newDueDate,
            String reason
    ) {}

    @Builder
    public record PlanAdjustmentResponse(
            Long id,
            Long familyPlanId,
            Long sourceInferenceId,
            String adjustmentType,
            String reason,
            String status,
            LocalDateTime createdAt,
            LocalDateTime approvedAt,
            String approvedBy,
            List<MissionAdjustmentResponse> missionAdjustments
    ) {}

    public record ProposeAdjustmentRequest(
            String triggerSource,
            String customReason
    ) {}

    public record AdjustmentApprovalRequest(
            String approvedBy,
            String notes
    ) {}
}
