package com.integrityfamily.domain;

import lombok.Getter;

@Getter
public enum RiskLevel {
    LOW("Bajo", "El núcleo familiar muestra coherencia y estabilidad.", 80, 100),
    MEDIUM("Medio", "Se detectan desajustes en la comunicación o hábitos.", 50, 79),
    HIGH("Alto", "Riesgo crítico detectado. Requiere intervención inmediata.", 0, 49),
    CRISIS("Crisis", "Protocolo Sentinel activado. Ruptura de simetría detectada.", 0, 0);

    private final String label;
    private final String description;
    private final int min;
    private final int max;

    RiskLevel(String label, String description, int min, int max) {
        this.label = label;
        this.description = description;
        this.min = min;
        this.max = max;
    }

    public static RiskLevel fromScore(double scorePercentage) {
        if (scorePercentage < 50) return HIGH;
        if (scorePercentage < 80) return MEDIUM;
        return LOW;
    }

    public static RiskLevel safeValueOf(String value) {
        try {
            return RiskLevel.valueOf(value);
        } catch (Exception e) {
            return LOW;
        }
    }
}
