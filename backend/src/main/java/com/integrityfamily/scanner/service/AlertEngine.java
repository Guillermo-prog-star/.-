package com.integrityfamily.scanner.service;

import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.risk.service.RiskAlgoV1Engine;
import com.integrityfamily.scanner.domain.FamilyAlert;
import com.integrityfamily.scanner.domain.InferenceRecord;
import com.integrityfamily.scanner.domain.RuleActivation;
import com.integrityfamily.scanner.repository.FamilyAlertRepository;
import com.integrityfamily.scanner.repository.InferenceRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

/**
 * IF-ALT: Motor de detección de patrones clínicos críticos.
 *
 * Analiza el resultado de una evaluación junto al historial de inferencias
 * y genera alertas cuando detecta patrones que requieren atención clínica.
 *
 * Patrones detectados:
 *   CONSECUTIVE_HIGH_RISK    → 2+ inferencias ICF_CALC con riskLevel ALTO/CRITICO en 30 días
 *   CRITICAL_STATE_SUSTAINED → 3+ inferencias con operationalState = CRITICAL
 *   SIMULATION_REPEAT        → 2+ inferencias con simulationSuspected = true en 14 días
 *   RELAPSE_CONFIRMED        → relapseDetected = true en la evaluación actual
 *   MULTI_RULE_ACTIVATION    → 3+ reglas EEDSL activadas en la misma evaluación
 *
 * Propiedades: no bloqueante, idempotente por tipo (no duplica alertas activas del mismo tipo).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertEngine {

    private static final Set<String> HIGH_RISK_LEVELS = Set.of("ALTO", "CRITICO");

    private final FamilyAlertRepository       alertRepo;
    private final InferenceRecordRepository   inferenceRepo;

    /**
     * Evalúa los patrones de alerta para la evaluación finalizada.
     * Se llama desde EvaluationService.finalize() — no lanza excepciones al caller.
     */
    @Transactional
    public void evaluate(Evaluation evaluation,
                         RiskAlgoV1Engine.AlgoResult algo,
                         List<RuleActivation> activations) {
        Long familyId    = evaluation.getFamily().getId();
        Long evaluationId = evaluation.getId();

        checkRelapseConfirmed(familyId, evaluationId, algo);
        checkMultiRuleActivation(familyId, evaluationId, activations);
        checkSimulationRepeat(familyId, evaluationId, algo);
        checkConsecutiveHighRisk(familyId, evaluationId, algo);
        checkCriticalStateSustained(familyId, evaluationId);
    }

    // ── Detectores ────────────────────────────────────────────────────────────

    private void checkRelapseConfirmed(Long familyId, Long evalId, RiskAlgoV1Engine.AlgoResult algo) {
        if (!algo.relapseDetected()) return;
        createIfAbsent(FamilyAlert.builder()
                .familyId(familyId)
                .evaluationId(evalId)
                .alertType("RELAPSE_CONFIRMED")
                .severity("HIGH")
                .title("Recaída emocional confirmada")
                .detail("El motor detectó respuestas de recaída en esta evaluación. " +
                        "Requiere revisión clínica inmediata y plan de contención.")
                .inferenceKey("ICF_CALC")
                .build());
    }

    private void checkMultiRuleActivation(Long familyId, Long evalId, List<RuleActivation> activations) {
        if (activations.size() < 3) return;
        String rulesSummary = activations.stream()
                .map(RuleActivation::ruleKey)
                .limit(5)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        createIfAbsent(FamilyAlert.builder()
                .familyId(familyId)
                .evaluationId(evalId)
                .alertType("MULTI_RULE_ACTIVATION")
                .severity("HIGH")
                .title("Activación múltiple de reglas emocionales")
                .detail(String.format("%d reglas EEDSL activadas simultáneamente: %s. " +
                        "Patrón de convergencia crítica.", activations.size(), rulesSummary))
                .inferenceKey("EEDSL")
                .build());
    }

    private void checkSimulationRepeat(Long familyId, Long evalId, RiskAlgoV1Engine.AlgoResult algo) {
        if (!algo.simulationSuspected()) return;
        Instant window = Instant.now().minus(14, ChronoUnit.DAYS);
        List<InferenceRecord> recent = inferenceRepo.findByFamilyIdOrderByCreatedAtDesc(familyId);
        long simulationCount = recent.stream()
                .filter(r -> r.getCreatedAt().isAfter(window))
                .filter(r -> Boolean.TRUE.equals(r.getSimulationSuspected()))
                .count();
        if (simulationCount < 2) return;
        createIfAbsent(FamilyAlert.builder()
                .familyId(familyId)
                .evaluationId(evalId)
                .alertType("SIMULATION_REPEAT")
                .severity("MEDIUM")
                .title("Simulación detectada reiteradamente")
                .detail(String.format("Simulación sospechada en %d evaluaciones en los últimos 14 días. " +
                        "Posible fachada de bienestar. Requiere evaluación cualitativa.", simulationCount))
                .inferenceKey("ICF_CALC")
                .build());
    }

    private void checkConsecutiveHighRisk(Long familyId, Long evalId, RiskAlgoV1Engine.AlgoResult algo) {
        if (!HIGH_RISK_LEVELS.contains(algo.riskLevel())) return;
        Instant window = Instant.now().minus(30, ChronoUnit.DAYS);
        List<InferenceRecord> recent = inferenceRepo.findByFamilyIdOrderByCreatedAtDesc(familyId);
        long highRiskCount = recent.stream()
                .filter(r -> r.getCreatedAt().isAfter(window))
                .filter(r -> "ICF_CALC".equals(r.getInferenceKey()))
                .filter(r -> HIGH_RISK_LEVELS.contains(r.getRiskLevel()))
                .count();
        if (highRiskCount < 2) return;
        createIfAbsent(FamilyAlert.builder()
                .familyId(familyId)
                .evaluationId(evalId)
                .alertType("CONSECUTIVE_HIGH_RISK")
                .severity("CRITICAL")
                .title("Riesgo alto sostenido en el tiempo")
                .detail(String.format("%d evaluaciones con riesgo ALTO o CRÍTICO en los últimos 30 días. " +
                        "El sistema familiar no está respondiendo a intervenciones previas.", highRiskCount))
                .inferenceKey("ICF_CALC")
                .build());
    }

    private void checkCriticalStateSustained(Long familyId, Long evalId) {
        List<InferenceRecord> recent = inferenceRepo.findByFamilyIdOrderByCreatedAtDesc(familyId);
        long criticalCount = recent.stream()
                .limit(5)
                .filter(r -> "CRITICAL".equals(r.getOperationalState()))
                .count();
        if (criticalCount < 3) return;
        createIfAbsent(FamilyAlert.builder()
                .familyId(familyId)
                .evaluationId(evalId)
                .alertType("CRITICAL_STATE_SUSTAINED")
                .severity("CRITICAL")
                .title("Estado crítico sostenido (IF-TOS)")
                .detail(String.format("La familia lleva %d inferencias consecutivas en estado CRITICAL. " +
                        "Requiere intervención clínica urgente.", criticalCount))
                .inferenceKey("IF-TOS")
                .build());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void createIfAbsent(FamilyAlert alert) {
        if (alertRepo.existsByFamilyIdAndAlertTypeAndResolvedFalse(alert.getFamilyId(), alert.getAlertType())) {
            log.debug("[IF-ALT] Alerta {} ya activa para familia {} — no duplica.",
                    alert.getAlertType(), alert.getFamilyId());
            return;
        }
        alertRepo.save(alert);
        log.warn("[IF-ALT] Alerta generada: tipo={} | severidad={} | familia={}",
                alert.getAlertType(), alert.getSeverity(), alert.getFamilyId());
    }
}
