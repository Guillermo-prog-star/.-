package com.integrityfamily.documentary.dto;

import com.integrityfamily.checklist.dto.TaskEvidenceDtos;
import com.integrityfamily.documentary.domain.DocumentaryProduction;
import com.integrityfamily.documentary.domain.DocumentaryScope;
import com.integrityfamily.documentary.domain.ProductionStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class DocumentaryProductionDTO {
    private Long id;
    private Long familyId;
    private String title;
    private DocumentaryScope scope;
    private Long referenceId;
    private ProductionStatus status;
    private List<TaskEvidenceDtos.TaskEvidenceResponse> curatedEvidences;
    private String scriptData;
    private String exportUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DocumentaryProductionDTO fromEntity(DocumentaryProduction entity) {
        if (entity == null) return null;
        DocumentaryProductionDTO dto = new DocumentaryProductionDTO();
        dto.setId(entity.getId());
        dto.setFamilyId(entity.getFamily() != null ? entity.getFamily().getId() : null);
        dto.setTitle(entity.getTitle());
        dto.setScope(entity.getScope());
        dto.setReferenceId(entity.getReferenceId());
        dto.setStatus(entity.getStatus());
        dto.setScriptData(entity.getScriptData());
        dto.setExportUrl(entity.getExportUrl());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getCuratedEvidences() != null) {
            dto.setCuratedEvidences(entity.getCuratedEvidences().stream()
                    .map(TaskEvidenceDtos.TaskEvidenceResponse::fromEntity)
                    .collect(Collectors.toList()));
        }

        return dto;
    }
}
