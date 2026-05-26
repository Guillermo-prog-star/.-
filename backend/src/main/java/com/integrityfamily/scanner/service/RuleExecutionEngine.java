package com.integrityfamily.scanner.service;

import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.risk.service.RiskAlgoV1Engine;
import com.integrityfamily.scanner.domain.EmotionalRule;
import com.integrityfamily.scanner.domain.RuleActivation;
import com.integrityfamily.scanner.repository.EmotionalRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * IF-REE: Motor de Ejecución de Reglas EEDSL.
 *
 * Evalúa las reglas emocionales activas contra el AlgoResult de una evaluación
 * y devuelve la lista de activaciones (reglas que se cumplen en su totalidad).
 *
 * Propiedades:
 *   - Determinístico: misma entrada → mismo resultado
 *   - No bloqueante: errores en una regla no afectan las demás
 *   - Trazable: cada activación incluye las señales que la dispararon
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleExecutionEngine {

    private final EmotionalRuleRepository ruleRepo;
    private final SignalResolver           signalResolver;

    /**
     * Evalúa todas las reglas activas contra el resultado del algoritmo.
     *
     * @param algo       resultado completo de RISK_ALGO_V1
     * @param evaluation evaluación finalizada (para filtrar por hito)
     * @param memberRole rol del miembro evaluado ("PADRE", "MADRE", etc.) o null si es familiar
     * @return lista de reglas activadas (puede ser vacía, nunca null)
     */
    public List<RuleActivation> evaluateRules(RiskAlgoV1Engine.AlgoResult algo,
                                              Evaluation evaluation,
                                              String memberRole) {
        List<EmotionalRule> activeRules = ruleRepo.findByActiveTrue();
        String currentMilestone = evaluation.getFamily().getCurrentMilestone();

        List<RuleActivation> activations = new ArrayList<>();

        for (EmotionalRule rule : activeRules) {
            try {
                if (!matchesScope(rule, currentMilestone, memberRole)) continue;
                if (rule.getRequiredSignals() == null || rule.getRequiredSignals().isEmpty()) continue;

                List<String> triggered = rule.getRequiredSignals().stream()
                        .filter(signal -> signalResolver.isPresent(signal, algo))
                        .toList();

                if (triggered.size() == rule.getRequiredSignals().size()) {
                    activations.add(new RuleActivation(
                            rule.getId(),
                            rule.getRuleKey(),
                            rule.getVersion(),
                            rule.getProjectionLabel(),
                            rule.getConfidenceBase(),
                            rule.getRiskOutput(),
                            triggered
                    ));
                    log.info("[IF-REE] Regla activada: {} v{} | señales={} | familia={}",
                            rule.getRuleKey(), rule.getVersion(), triggered,
                            evaluation.getFamily().getId());
                }
            } catch (Exception ex) {
                log.warn("[IF-REE] Error evaluando regla {} (no bloqueante): {}",
                        rule.getRuleKey(), ex.getMessage());
            }
        }

        log.info("[IF-REE] Evaluación familia={} — {} reglas activas, {} activadas.",
                evaluation.getFamily().getId(), activeRules.size(), activations.size());

        return activations;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Determina si una regla aplica al hito actual y al rol del miembro.
     *
     * Lógica de scope:
     *   - "*" → aplica siempre
     *   - match exacto del hito actual → aplica
     *   - rango "X-Y" → aplica si el hito actual está entre X e Y (orden lexicográfico del milestone_key)
     */
    private boolean matchesScope(EmotionalRule rule, String currentMilestone, String memberRole) {
        if (!matchesRoleScope(rule.getMemberRole(), memberRole)) return false;
        if (!matchesMilestoneScope(rule.getMilestoneScope(), currentMilestone)) return false;
        return true;
    }

    private boolean matchesRoleScope(String ruleRole, String evaluationRole) {
        if (ruleRole == null || "*".equals(ruleRole)) return true;
        if (evaluationRole == null) return false;
        return ruleRole.equalsIgnoreCase(evaluationRole);
    }

    private boolean matchesMilestoneScope(String scope, String current) {
        if (scope == null || "*".equals(scope)) return true;
        if (current == null) return false;

        // Rango "X-Y": aplica si current está entre X e Y lexicográficamente
        if (scope.contains("-")) {
            String[] parts = scope.split("-", 2);
            String from = parts[0].trim();
            String to   = parts[1].trim();
            return current.compareToIgnoreCase(from) >= 0
                    && current.compareToIgnoreCase(to) <= 0;
        }

        // Match exacto
        return scope.equalsIgnoreCase(current);
    }
}
