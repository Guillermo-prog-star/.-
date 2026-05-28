package com.integrityfamily.ai.service;

import com.integrityfamily.domain.repository.ImprovementPlanRepository;
import com.integrityfamily.scanner.repository.FamilyAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Fase C: Infiere el objetivo conversacional al inicio de una sesión
 * basándose en el estado actual de la familia (alertas, misiones, contexto).
 *
 * Prioridad: CRISIS_CONTAINMENT → SUPPORT → PLANNING → GENERAL
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ConversationGoalManager {

    private final FamilyAlertRepository alertRepository;
    private final ImprovementPlanRepository planRepository;

    /**
     * Infiere el objetivo óptimo para la sesión conversacional.
     * Falla silenciosamente: devuelve "GENERAL" si hay error.
     *
     * @return GENERAL | SUPPORT | PLANNING | CRISIS_CONTAINMENT
     */
    public String inferGoal(Long familyId, Long memberId) {
        if (familyId == null) return "GENERAL";
        try {
            var alerts = alertRepository.findByFamilyIdAndResolvedFalseOrderByCreatedAtDesc(familyId);
            if (!alerts.isEmpty()) {
                String severity = alerts.get(0).getSeverity();
                if ("CRITICAL".equals(severity)) return "CRISIS_CONTAINMENT";
                if ("HIGH".equals(severity))     return "SUPPORT";
                return "SUPPORT";
            }

            boolean hasPendingMissions = planRepository.findByFamilyId(familyId).stream()
                    .anyMatch(p -> p.getTasks().stream().anyMatch(t -> !t.isCompleted()));
            if (hasPendingMissions) return "PLANNING";

        } catch (Exception e) {
            log.warn("[GOAL_MANAGER] Error infiriendo objetivo para familia {}: {}", familyId, e.getMessage());
        }
        return "GENERAL";
    }
}
