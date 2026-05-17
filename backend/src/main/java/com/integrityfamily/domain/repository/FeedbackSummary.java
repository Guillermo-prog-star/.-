package com.integrityfamily.domain.repository;

import java.time.LocalDateTime;

public interface FeedbackSummary {
    Long getId();
    int getScore();
    String getComment();
    String getType();
    String getMilestoneAtMoment();
    LocalDateTime getCreatedAt();
    
    ReporterSummary getReporter();
    
    interface ReporterSummary {
        String getFullName();
    }
}
