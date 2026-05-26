package com.integrityfamily.scanner.service;

import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.EvaluationStatus;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.scanner.domain.EmotionalOperationalState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * IF-TOS: Clasificador de estado operacional emocional familiar.
 *
 * Determina en qué punto del ciclo se encuentra la familia comparando
 * la evaluación actual con el historial longitudinal. No evalúa personas:
 * evalúa trayectorias familiares en el tiempo.
 *
 * Modelo temporal:
 *   P(E_t+1 | E_t, ICF, phase, criticalDim)
 *
 * La clasificación nunca implica causalidad absoluta ni diagnóstico clínico.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmotionalStateClassifier {

    private final EvaluationRepository evaluationRepository;

    /** Umbral de cambio significativo en ICF para distinguir stable vs. escalating/recovering. */
    private static final double ICF_CHANGE_THRESHOLD = 5.0;

    /** ICF mínimo para considerar RESOLVED. */
    private static final double ICF_RESOLVED_FLOOR = 70.0;

    /**
     * Clasifica el estado operacional emocional actual de la familia
     * a partir de su historial de evaluaciones finalizadas.
     *
     * @param familyId ID de la familia
     * @return estado operacional actual según el historial
     */
    @Transactional(readOnly = true)
    public EmotionalOperationalState classify(Long familyId) {
        List<Evaluation> history = evaluationRepository.findByFamilyId(familyId).stream()
                .filter(e -> e.getStatus() == EvaluationStatus.FINALIZED && e.getIcf() != null)
                .sorted(Comparator.comparing(Evaluation::getFinalizedAt))
                .toList();

        EmotionalOperationalState state = classifyFromHistory(history);
        log.debug("[IF-TOS] Familia {} → estado operacional: {}", familyId, state);
        return state;
    }

    private EmotionalOperationalState classifyFromHistory(List<Evaluation> evals) {
        if (evals.isEmpty()) {
            return EmotionalOperationalState.EMERGING;
        }

        Evaluation latest = evals.get(evals.size() - 1);

        // CRITICAL: riesgo crítico puntual — tiene prioridad sobre cualquier tendencia
        if ("CRITICO".equals(latest.getRiskLevel())) {
            return EmotionalOperationalState.CRITICAL;
        }

        // EMERGING: primera evaluación, sin historial comparativo
        if (evals.size() == 1) {
            return EmotionalOperationalState.EMERGING;
        }

        int n = evals.size();
        double latestIcf = latest.getIcf();
        Evaluation previous = evals.get(n - 2);
        double prevIcf = previous.getIcf();
        String prevRisk = previous.getRiskLevel();

        // RESOLVED: ≥2 evaluaciones consecutivas en BAJO con ICF ≥ 70
        boolean allBajoRecent = evals.subList(n - 2, n).stream()
                .allMatch(e -> "BAJO".equals(e.getRiskLevel()));
        if (allBajoRecent && latestIcf >= ICF_RESOLVED_FLOOR) {
            return EmotionalOperationalState.RESOLVED;
        }

        // RECOVERING: mejora medible desde un estado deteriorado
        boolean cameFromHighRisk = "ALTO".equals(prevRisk) || "CRITICO".equals(prevRisk);
        if (cameFromHighRisk && latestIcf > prevIcf + ICF_CHANGE_THRESHOLD) {
            return EmotionalOperationalState.RECOVERING;
        }

        // ESCALATING: deterioro progresivo confirmado en ≥2 evaluaciones
        if (n >= 3) {
            double prev2Icf = evals.get(n - 3).getIcf();
            boolean twoStepDecline = prevIcf < prev2Icf - 3.0 && latestIcf < prevIcf - 3.0;
            if (twoStepDecline) {
                return EmotionalOperationalState.ESCALATING;
            }
        }

        // ESCALATING: un paso significativo hacia abajo en riesgo no trivial
        boolean significantDrop = latestIcf < prevIcf - ICF_CHANGE_THRESHOLD;
        boolean nonTrivialRisk = "ALTO".equals(latest.getRiskLevel())
                || "MODERADO".equals(latest.getRiskLevel());
        if (significantDrop && nonTrivialRisk) {
            return EmotionalOperationalState.ESCALATING;
        }

        return EmotionalOperationalState.STABLE;
    }
}
