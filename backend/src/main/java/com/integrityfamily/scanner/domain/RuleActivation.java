package com.integrityfamily.scanner.domain;

import java.util.List;

/**
 * IF-REE: Resultado de la evaluación de una regla EEDSL.
 *
 * Representa una regla que se activó: todas sus señales estaban presentes
 * en el AlgoResult de la evaluación. No es una entidad JPA — es un
 * value object que fluye desde RuleExecutionEngine hacia InferenceRecordService.
 */
public record RuleActivation(
        Long         ruleId,
        String       ruleKey,
        int          version,
        String       projectionLabel,
        double       confidenceBase,
        String       riskOutput,
        List<String> activatedSignals  // señales que se cumplieron
) {}
