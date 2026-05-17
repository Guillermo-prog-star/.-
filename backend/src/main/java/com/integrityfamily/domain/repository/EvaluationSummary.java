package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.EvaluationStatus;
import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDateTime;

public interface EvaluationSummary {
    Long getId();
    
    @Value("#{target.family.id}")
    Long getFamilyId();
    
    @Value("#{target.member != null ? target.member.id : null}")
    Long getMemberId();
    
    EvaluationStatus getStatus();
    LocalDateTime getStartedAt();
    LocalDateTime getFinalizedAt();
    Double getIcf();
    String getRiskLevel();
    String getCriticalDimension();
}
