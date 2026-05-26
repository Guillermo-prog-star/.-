package com.integrityfamily.scanner.dto;

import java.time.Instant;
import java.util.List;

/**
 * DTO de una regla EEDSL para la API REST.
 * Expone todos los campos editables más metadatos de auditoría.
 */
public record EmotionalRuleDto(
        Long      id,
        String    ruleKey,
        int       version,
        boolean   active,
        String    milestoneScope,
        String    memberRole,
        List<String> requiredSignals,
        int       temporalWindowDays,
        String    projectionLabel,
        double    confidenceBase,
        String    riskOutput,
        String    createdBy,
        Instant   createdAt
) {}
