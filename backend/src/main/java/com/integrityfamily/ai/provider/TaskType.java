package com.integrityfamily.ai.provider;

/**
 * Classifies AI tasks by computational cost and quality requirements.
 * Used by AiProviderSelector to route requests to the optimal provider.
 *
 * LIGHTWEIGHT  → structured JSON output, classification, tagging, short summaries.
 *               Always routed to cheapest available provider, regardless of Sentinel mode.
 * STANDARD     → conversational analysis, clinical recommendations, adaptive plans.
 *               Routed by context (Sentinel → high-capacity, normal → cost-optimized).
 * HIGH_CAPACITY → complex narratives, family movie, 36-month plans, executive reports.
 *               Always routed to most capable available provider.
 */
public enum TaskType {
    LIGHTWEIGHT,
    STANDARD,
    HIGH_CAPACITY
}
