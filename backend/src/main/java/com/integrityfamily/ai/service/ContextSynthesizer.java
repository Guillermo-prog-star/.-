package com.integrityfamily.ai.service;

import com.integrityfamily.ai.dto.AiContext;
import com.integrityfamily.domain.repository.ChatMessageRepository;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.RiskSnapshot;
import com.integrityfamily.domain.repository.RiskSnapshotRepository;
import com.integrityfamily.domain.repository.PlanRepository;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.EvaluationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SDD-AI-03.2: Enhanced Context Synthesizer.
 * Injects temporal trends, nodal composition, and conversational memory.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ContextSynthesizer {

    private final RiskSnapshotRepository riskRepo;
    private final PlanRepository planRepo;
    private final EvaluationRepository evalRepo;
    private final ChatMessageRepository chatRepo;

    private static final int MISSION_LIMIT = 5;
    private static final int HISTORY_LIMIT = 10;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Transactional(readOnly = true)
    public AiContext synthesize(Family family, String sentiment) {
        log.debug("Synthesizing memory-enhanced context for family: {}", family.getId());

        List<RiskSnapshot> riskHistory = riskRepo.findByFamilyIdOrderByCreatedAtDesc(family.getId());
        Optional<Evaluation> latestEval = evalRepo.findTopByFamilyIdAndStatusOrderByFinalizedAtDesc(
                family.getId(), EvaluationStatus.FINALIZED);

        RiskSnapshot currentRisk = riskHistory.isEmpty() ? null : riskHistory.get(0);

        return new AiContext(
            buildMetadata(family, currentRisk),
            buildMemberNodes(family),
            buildMetrics(currentRisk),
            buildTrends(currentRisk, riskHistory),
            extractDimensionScores(latestEval),
            fetchTopMissions(family.getId()),
            fetchMessageHistory(family.getId()),
            Boolean.TRUE.equals(family.getSentinelActive()),
            sentiment
        );
    }

    private AiContext.FamilyMetadata buildMetadata(Family family, RiskSnapshot risk) {
        String lastUpdate = (risk != null) ? risk.getCreatedAt().format(DATE_FORMAT) : "N/A";
        return new AiContext.FamilyMetadata(family.getName(), family.getCurrentMilestone(), lastUpdate);
    }

    private List<AiContext.MemberNode> buildMemberNodes(Family family) {
        return family.getMembers().stream()
                .filter(m -> m.isActive())
                .map(m -> new AiContext.MemberNode(m.getFirstName(), m.getRole()))
                .toList();
    }

    private AiContext.IntegrityMetrics buildMetrics(RiskSnapshot risk) {
        if (risk == null) return new AiContext.IntegrityMetrics(0.0, "DESCONOCIDO", "INICIAL");
        return new AiContext.IntegrityMetrics(risk.getIcf(), risk.getRiskLevel(), risk.getConsciousnessLabel());
    }

    private AiContext.TrendAnalysis buildTrends(RiskSnapshot current, List<RiskSnapshot> history) {
        if (current == null || history.size() < 2) {
            return new AiContext.TrendAnalysis(0.0, 0.0, 0.0);
        }

        // SDD-TREND-01: BÃƒÂºsqueda del punto de comparaciÃƒÂ³n significativo (Lookback Window)
        // Evitamos comparar con evaluaciones de hace menos de 24h para reducir el ruido.
        RiskSnapshot significantPrevious = history.stream()
                .skip(1) // Saltar el actual
                .filter(rs -> java.time.Duration.between(rs.getCreatedAt(), current.getCreatedAt()).toHours() >= 24)
                .findFirst()
                .orElse(history.get(1)); // Fallback al inmediatamente anterior si no hay mÃƒÂ¡s

        double delta = current.getIcf() - significantPrevious.getIcf();
        
        // CÃƒÂ¡lculo de Velocidad (Puntos por dÃƒÂ­a)
        long daysBetween = java.time.Duration.between(significantPrevious.getCreatedAt(), current.getCreatedAt()).toDays();
        double velocity = (daysBetween > 0) ? delta / daysBetween : delta;

        log.debug("Trend Analysis: Delta={}, Velocity={} pts/day (Comparison period: {} days)", 
                delta, velocity, daysBetween);

        return new AiContext.TrendAnalysis(significantPrevious.getIcf(), delta, velocity);
    }

    private Map<String, Double> extractDimensionScores(Optional<Evaluation> eval) {
        return eval.map(e -> e.getDimensionScores().stream()
                .collect(Collectors.toMap(
                    ds -> ds.getDimensionName(),
                    ds -> ds.getScore(),
                    (v1, v2) -> v1
                )))
                .orElse(Collections.emptyMap());
    }

    private List<AiContext.ActiveMission> fetchTopMissions(Long familyId) {
        return planRepo.findByFamilyIdOrderByCreatedAtDesc(familyId).stream()
                .flatMap(p -> p.getTasks().stream())
                .filter(t -> !Boolean.TRUE.equals(t.getCompleted()))
                .limit(MISSION_LIMIT)
                .map(t -> new AiContext.ActiveMission(t.getTitle(), t.getDescription()))
                .toList();
    }

    private List<AiContext.MessageHistory> fetchMessageHistory(Long familyId) {
        var messages = chatRepo.findByFamilyIdOrderByCreatedAtDesc(familyId, PageRequest.of(0, HISTORY_LIMIT));
        Collections.reverse(messages); // Orden cronolÃƒÂ³gico para el prompt
        return messages.stream()
                .map(m -> new AiContext.MessageHistory(m.isAi() ? "ASSISTANT" : "USER", m.getContent()))
                .toList();
    }
}



