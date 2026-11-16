package com.integrityfamily.hud.policy;

public class ProfessionalPresentationPolicy {

    public static String formatICaF(int icafValue, String trend) {
        return String.format("ICaF: %d (Tendencia: %s)", icafValue, trend != null ? trend : "ESTABLE");
    }

    public static String formatAdaptiveCapacity(double capacity) {
        return String.format("Capacidad Adaptativa: %.2f", capacity);
    }

    public static String formatRisk(String riskLevel, int alertsCount) {
        return String.format("Riesgo Clínico: %s (%d alertas activas)", riskLevel != null ? riskLevel : "INDETERMINADO", alertsCount);
    }
}
