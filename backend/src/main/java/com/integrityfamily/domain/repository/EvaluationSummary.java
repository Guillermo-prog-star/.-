package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.EvaluationStatus;
import java.time.LocalDateTime;

/**
 * Closed projection para GET /assessments/family/{id}/history.
 *
 * No usa @Value(SpEL) para evitar open-projection: con SpEL Hibernate instancia
 * el proxy de Family y Member por cada row (N+1). Al usar aliases JPQL
 * el driver devuelve el FK directamente sin lazy-load extra.
 */
public interface EvaluationSummary {
    Long getId();
    Long getFamilyId();   // alias JPQL: "familyId"
    Long getMemberId();   // alias JPQL: "memberId"
    EvaluationStatus getStatus();
    LocalDateTime getStartedAt();
    LocalDateTime getFinalizedAt();
    Double getIcf();
    String getRiskLevel();
    String getCriticalDimension();
}
