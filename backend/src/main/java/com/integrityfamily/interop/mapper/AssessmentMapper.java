package com.integrityfamily.interop.mapper;

import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.EvaluationDimensionScore;
import com.integrityfamily.interop.canonical.Assessment;
import com.integrityfamily.interop.canonical.Observation;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code Evaluation} (+ sus {@code EvaluationDimensionScore}) → {@link Assessment}.
 * Cada score (ICF, vector neurofenomenológico, dimensiones) se traduce como
 * una {@link Observation} independiente — así es como FHIR representa un
 * QuestionnaireResponse con múltiples mediciones, y evita colapsar todo en
 * un solo valor.
 */
public final class AssessmentMapper {

    private AssessmentMapper() {}

    public static Assessment toCanonical(Evaluation evaluation, List<EvaluationDimensionScore> dimensionScores) {
        List<Observation> observations = new ArrayList<>();
        String subjectId = "family-" + evaluation.getFamily().getId();
        String assessmentId = "evaluation-" + evaluation.getId();

        addObservation(observations, assessmentId, subjectId, "ICF", "Índice de Cohesión Familiar", evaluation.getIcf(), evaluation);
        addObservation(observations, assessmentId, subjectId, "SOMATIC_AWARENESS", "Conciencia somática", evaluation.getSomaticAwareness(), evaluation);
        addObservation(observations, assessmentId, subjectId, "EMOTIONAL_AWARENESS", "Conciencia emocional", evaluation.getEmotionalAwareness(), evaluation);
        addObservation(observations, assessmentId, subjectId, "COGNITIVE_AWARENESS", "Conciencia cognitiva", evaluation.getCognitiveAwareness(), evaluation);
        addObservation(observations, assessmentId, subjectId, "IMPULSIVE_AWARENESS", "Conciencia impulsiva", evaluation.getImpulsiveAwareness(), evaluation);
        addObservation(observations, assessmentId, subjectId, "PAUSE_CAPACITY", "Capacidad de pausa", evaluation.getPauseCapacity(), evaluation);
        addObservation(observations, assessmentId, subjectId, "INTEGRATION_SCORE", "Puntaje de integración", evaluation.getIntegrationScore(), evaluation);

        if (dimensionScores != null) {
            for (EvaluationDimensionScore d : dimensionScores) {
                observations.add(Observation.builder()
                        .canonicalId(assessmentId + "-dim-" + d.getId())
                        .subjectId(subjectId)
                        .code(d.getDimensionName())
                        .display(d.getDimensionName())
                        .valueNumeric(d.getScore())
                        .effectiveAt(evaluation.getFinalizedAt() != null ? evaluation.getFinalizedAt() : evaluation.getStartedAt())
                        .status(observationStatus(evaluation))
                        .build());
            }
        }

        return Assessment.builder()
                .canonicalId(assessmentId)
                .subjectId(subjectId)
                .status(evaluation.getStatus() != null ? evaluation.getStatus().name() : null)
                .startedAt(evaluation.getStartedAt())
                .finalizedAt(evaluation.getFinalizedAt())
                .observations(observations)
                .overallRiskLevel(evaluation.getRiskLevel())
                .criticalDimension(evaluation.getCriticalDimension())
                .build();
    }

    private static void addObservation(List<Observation> observations, String assessmentId, String subjectId,
                                        String code, String display, Double value, Evaluation evaluation) {
        if (value == null) return;
        observations.add(Observation.builder()
                .canonicalId(assessmentId + "-" + code.toLowerCase())
                .subjectId(subjectId)
                .code(code)
                .display(display)
                .valueNumeric(value)
                .effectiveAt(evaluation.getFinalizedAt() != null ? evaluation.getFinalizedAt() : evaluation.getStartedAt())
                .status(observationStatus(evaluation))
                .build());
    }

    private static String observationStatus(Evaluation evaluation) {
        return evaluation.getFinalizedAt() != null ? "FINAL" : "PRELIMINARY";
    }
}
