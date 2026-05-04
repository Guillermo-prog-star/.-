package com.integrityfamily.family.dto;

import com.integrityfamily.domain.LogbookStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class FamilyLogbookDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateLogbookEntryRequest {
        private String title;
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CloseLogbookEntryRequest {
        private String evidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogbookEntryResponse {
        private Long id;
        private Long familyId;
        private String title;
        private String description;
        private LogbookStatus status;
        private String evidence;
        private LocalDateTime createdAt;
        private LocalDateTime resolvedAt;
        private String createdByName;
    }
}
