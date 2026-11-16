package com.integrityfamily.evaluation.service.post;

import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.risk.service.RiskAlgoV1Engine;

/**
 * Datos compartidos entre los pasos posteriores a la finalización (ADR-020).
 *
 * <p>{@code riskLevel} es el único dato que un paso produce para los siguientes:
 * arranca con el riesgo de la evaluación (o "MODERADO") y el paso de riesgo lo
 * reemplaza por el del snapshot, igual que la variable local que existía en
 * {@code EvaluationService.processPostFinalization}.
 */
public final class PostFinalizationContext {

    private final Evaluation evaluation;
    private final RiskAlgoV1Engine.AlgoResult algo;   // null en simulación
    private String riskLevel;

    public PostFinalizationContext(Evaluation evaluation, RiskAlgoV1Engine.AlgoResult algo) {
        this.evaluation = evaluation;
        this.algo = algo;
        this.riskLevel = evaluation.getRiskLevel() != null ? evaluation.getRiskLevel() : "MODERADO";
    }

    public Evaluation evaluation() { return evaluation; }

    /** Resultado completo del algoritmo; {@code null} cuando viene de una simulación. */
    public RiskAlgoV1Engine.AlgoResult algo() { return algo; }

    public String riskLevel() { return riskLevel; }

    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
}
