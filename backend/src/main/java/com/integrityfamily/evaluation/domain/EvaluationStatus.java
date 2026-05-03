package com.integrityfamily.evaluation.domain;

/**
 * SDD-EVAL-01: Lifecycle states for Family Evaluations.
 * Unificado para resolver discrepancias entre AiService, AdminAnalytics y
 * EvaluationService.
 */
public enum EvaluationStatus {
    STARTED,
    IN_PROGRESS,
    COMPLETED,
    FINISHED, // Requerido por AiService.java:[106]
    FINALIZED, // Requerido por AdminAnalyticsService.java:[57] y EvaluationService.java:[78]
    CANCELLED
}


