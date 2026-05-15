package com.integrityfamily.checklist.dto;

import com.integrityfamily.domain.EvidenceType;
import com.integrityfamily.domain.EvidenceStatus;
import com.integrityfamily.domain.TaskEvidence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * SDD SPEC 6.7: DTOs para el flujo de evidencias conductuales usando Java Records.
 */
public class TaskEvidenceDtos {

    public record SubmitRequest(
        @NotNull Long taskId,
        @NotNull Long familyId,
        @NotNull EvidenceType evidenceType,
        @NotBlank String title,
        String description,
        String fileUrl,
        String textContent,
        @NotBlank String submittedBy
    ) {}

    public record ValidateRequest(
        @NotNull Double score,
        @NotBlank String validatorName
    ) {}

    public record RejectRequest(
        @NotBlank String validatorName
    ) {}

    public record TaskRef(
        Long id
    ) {}

    public record TaskEvidenceResponse(
        Long id,
        TaskRef task,
        Long familyId,
        EvidenceType evidenceType,
        EvidenceStatus status,
        String title,
        String description,
        String fileUrl,
        String textContent,
        String submittedBy,
        Double aiScore,
        Double humanScore,
        boolean validated,
        LocalDateTime createdAt,
        LocalDateTime validatedAt
    ) {
        public static TaskEvidenceResponse fromEntity(TaskEvidence entity) {
            if (entity == null) return null;
            return new TaskEvidenceResponse(
                entity.getId(),
                entity.getTask() != null ? new TaskRef(entity.getTask().getId()) : null,
                entity.getFamily() != null ? entity.getFamily().getId() : null,
                entity.getEvidenceType(),
                entity.getStatus(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getFileUrl(),
                entity.getTextContent(),
                entity.getSubmittedBy(),
                entity.getAiScore(),
                entity.getHumanScore(),
                entity.isValidated(),
                entity.getCreatedAt(),
                entity.getValidatedAt()
            );
        }
    }
}

