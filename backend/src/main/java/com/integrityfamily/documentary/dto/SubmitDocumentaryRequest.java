package com.integrityfamily.documentary.dto;

import com.integrityfamily.domain.DocumentarySourceType;
import lombok.Data;
import java.util.List;

@Data
public class SubmitDocumentaryRequest {
    private Long familyId;
    private Long taskId;
    private Long sprintId;
    private Long pillarId;
    private String title;
    private String content;
    private DocumentarySourceType sourceType;
    private List<Long> evidenceIds; // Para vincular las fotos y notas ya subidas
}
