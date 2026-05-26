package com.integrityfamily.scanner.dto;

import java.util.List;

/**
 * Payload para crear o actualizar una regla EEDSL.
 */
public record EmotionalRuleRequest(
        String       ruleKey,
        String       milestoneScope,
        String       memberRole,
        List<String> requiredSignals,
        Integer      temporalWindowDays,
        String       projectionLabel,
        Double       confidenceBase,
        String       riskOutput
) {}
