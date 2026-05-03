package com.integrityfamily.checklist.dto;
import jakarta.validation.constraints.*;
public class ChecklistDtos {
    public record ChecklistItemResponse(Long id, Long familyId, Long planId, Long planTaskId, String title, Boolean completed) {}
    public record CreateRequest(@NotNull Long familyId, @NotBlank @Size(max=150) String title) {}
    public record FromPlanRequest(@NotNull Long planId) {}
    public record CompleteRequest(Boolean completed) {}
}


