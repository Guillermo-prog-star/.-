package com.integrityfamily.documentation.dto;

import com.integrityfamily.documentation.domain.DocumentCategory;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public class DocumentationDtos {

    @Data
    public static class DocumentSummaryResponse {
        private Long id;
        private String code;
        private String title;
        private DocumentCategory category;
        private String summary;
        private String version;
        private String tags;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class DocumentDetailResponse {
        private Long id;
        private String code;
        private String title;
        private DocumentCategory category;
        private String content;
        private String summary;
        private String version;
        private String status;
        private String tags;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class DocumentListResponse {
        private List<DocumentSummaryResponse> documents;
        private int total;
    }

    @Data
    public static class QueryRequest {
        private String question;
    }

    @Data
    public static class QueryResponse {
        private String answer;
        private List<DocumentSummaryResponse> sources;
    }
}
