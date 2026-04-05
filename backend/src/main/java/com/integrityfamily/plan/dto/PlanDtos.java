package com.integrityfamily.plan.dto;
import java.time.LocalDateTime; import java.util.List;
public class PlanDtos {
    public record PlanTaskResponse(Long id, String title, String description,
            Long assignedMemberId, String assignedMemberName, Boolean completed) {}
    public record PlanResponse(Long id, Long familyId, Long evaluationId, String title,
            String description, String aiReport, LocalDateTime aiGeneratedAt,
            String status, List<PlanTaskResponse> tasks) {}
    public record TaskCompleteRequest(Boolean completed) {}
}
