package com.integrityfamily.hud.policy;

public class FamilyPresentationPolicy {

    public static String formatICaF(int icafValue) {
        if (icafValue >= 80) {
            return "¡Excelente progreso! Las dinámicas familiares están muy fuertes y estables.";
        } else if (icafValue >= 60) {
            return "Han avanzado respecto al mes pasado. Sigan así.";
        } else {
            return "Esta es una buena oportunidad para enfocarse en fortalecer la comunicación y la unión familiar.";
        }
    }

    public static String formatAdaptiveCapacity(double capacity) {
        if (capacity >= 0.8) {
            return "El ritmo recomendado para esta semana es dinámico y activo.";
        } else if (capacity >= 0.5) {
            return "El ritmo recomendado para esta semana es tranquilo y equilibrado.";
        } else {
            return "El ritmo recomendado para esta semana es de pausa y autocuidado familiar.";
        }
    }

    /**
     * currentRiskLevel se calcula y persiste en español (RiskAlgoV1Engine,
     * RiskService, FamilyCausalEngine: "BAJO"/"MODERADO"/"MEDIO"/"ALTO"/"CRITICO"),
     * nunca en inglés — comparar contra "HIGH"/"MEDIUM" nunca coincidía con datos
     * reales y esta alerta no se disparaba jamás (ver ADR-002, action item 9).
     */
    public static String formatRisk(String riskLevel) {
        if ("CRITICO".equalsIgnoreCase(riskLevel) || "ALTO".equalsIgnoreCase(riskLevel)) {
            return "Es un buen momento para pedir apoyo o programar un espacio de conversación tranquila.";
        } else if ("MEDIO".equalsIgnoreCase(riskLevel) || "MODERADO".equalsIgnoreCase(riskLevel)) {
            return "Esta semana la familia tiene una gran oportunidad para fortalecer la comunicación.";
        } else {
            return "El ambiente familiar se encuentra estable y protector.";
        }
    }
}
