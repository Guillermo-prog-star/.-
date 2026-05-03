package com.integrityfamily.ai.dto;

import java.util.List;
import java.util.Map;

/**
 * SDD-AI-03.3: High-Fidelity Context Record with Transformation Velocity.
 * Includes Velocity to measure the rate of change in family integrity.
 */
public record AiContext(
    FamilyMetadata family,
    List<MemberNode> members,
    IntegrityMetrics metrics,
    TrendAnalysis trends,
    Map<String, Double> dimensionScores,
    List<ActiveMission> missions,
    List<MessageHistory> history,
    boolean sentinelActive,
    String currentSentiment // SDD: Added for dynamic tone adjustment
) {
    public record FamilyMetadata(String name, String milestone, String lastUpdate) {}
    public record MemberNode(String firstName, String role) {}
    public record IntegrityMetrics(Double icf, String riskLevel, String consciousnessLabel) {}
    public record TrendAnalysis(Double previousIcf, Double delta, Double velocity) {}
    public record ActiveMission(String title, String description) {}
    public record MessageHistory(String role, String content) {}
}
