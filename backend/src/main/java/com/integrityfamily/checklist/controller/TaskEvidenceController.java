package com.integrityfamily.checklist.controller;

import com.integrityfamily.domain.TaskEvidence;
import com.integrityfamily.checklist.service.TaskEvidenceService;
import com.integrityfamily.checklist.dto.TaskEvidenceDtos;
import com.integrityfamily.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * SDD SPEC 6.8: Controlador REST para la gestión de evidencias de transformación conductual.
 */
@RestController
@RequestMapping("/api/evidences")
@RequiredArgsConstructor
public class TaskEvidenceController {

    private final TaskEvidenceService taskEvidenceService;

    @GetMapping("/family/{familyId}")
    @Transactional(readOnly = true)
    public ApiResponse<List<TaskEvidenceDtos.TaskEvidenceResponse>> getByFamily(@PathVariable Long familyId) {
        List<TaskEvidenceDtos.TaskEvidenceResponse> responses = taskEvidenceService.getFamilyEvidences(familyId).stream()
                .map(TaskEvidenceDtos.TaskEvidenceResponse::fromEntity)
                .toList();
        return ApiResponse.ok(responses);
    }

    @GetMapping("/task/{taskId}")
    @Transactional(readOnly = true)
    public ApiResponse<List<TaskEvidenceDtos.TaskEvidenceResponse>> getByTask(@PathVariable Long taskId) {
        List<TaskEvidenceDtos.TaskEvidenceResponse> responses = taskEvidenceService.getEvidencesByTask(taskId).stream()
                .map(TaskEvidenceDtos.TaskEvidenceResponse::fromEntity)
                .toList();
        return ApiResponse.ok(responses);
    }

    @PostMapping("/submit")
    public ApiResponse<TaskEvidenceDtos.TaskEvidenceResponse> submit(@Valid @RequestBody TaskEvidenceDtos.SubmitRequest request) {
        TaskEvidence evidence = taskEvidenceService.submitEvidence(
                request.taskId(),
                request.familyId(),
                request.evidenceType(),
                request.title(),
                request.description(),
                request.fileUrl(),
                request.textContent(),
                request.submittedBy()
        );
        return ApiResponse.ok(TaskEvidenceDtos.TaskEvidenceResponse.fromEntity(evidence));
    }

    @PostMapping("/{id}/validate")
    public ApiResponse<TaskEvidenceDtos.TaskEvidenceResponse> validate(@PathVariable Long id, @Valid @RequestBody TaskEvidenceDtos.ValidateRequest request) {
        TaskEvidence validated = taskEvidenceService.validateEvidence(
                id,
                request.score(),
                request.validatorName()
        );
        return ApiResponse.ok(TaskEvidenceDtos.TaskEvidenceResponse.fromEntity(validated));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<TaskEvidenceDtos.TaskEvidenceResponse> reject(@PathVariable Long id, @Valid @RequestBody TaskEvidenceDtos.RejectRequest request) {
        TaskEvidence rejected = taskEvidenceService.rejectEvidence(
                id,
                request.validatorName()
        );
        return ApiResponse.ok(TaskEvidenceDtos.TaskEvidenceResponse.fromEntity(rejected));
    }
}

