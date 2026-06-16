package com.integrityfamily.documentary.dto;

import com.integrityfamily.domain.FamilyDocumentary;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FamilyDocumentaryDTO {
    private Long id;
    private Long familyId;
    private String title;
    private String content;
    private String sourceType;
    private String taskTitle;
    private String sprintMissionTitle;
    private LocalDateTime createdAt;

    public static FamilyDocumentaryDTO fromEntity(FamilyDocumentary doc) {
        FamilyDocumentaryDTO dto = new FamilyDocumentaryDTO();
        dto.setId(doc.getId());
        dto.setFamilyId(doc.getFamily().getId());
        dto.setTitle(doc.getTitle());
        dto.setContent(doc.getContent());
        dto.setSourceType(doc.getSourceType() != null ? doc.getSourceType().name() : null);
        if (doc.getTask() != null) {
            dto.setTaskTitle(doc.getTask().getTitle());
        }
        if (doc.getSprint() != null) {
            dto.setSprintMissionTitle(doc.getSprint().getObjective());
        }
        dto.setCreatedAt(doc.getCreatedAt());
        return dto;
    }
}
