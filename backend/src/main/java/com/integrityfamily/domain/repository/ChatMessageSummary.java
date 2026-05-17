package com.integrityfamily.domain.repository;

import java.time.LocalDateTime;

public interface ChatMessageSummary {
    Long getId();
    String getContent();
    boolean isAi();
    LocalDateTime getCreatedAt();
}
