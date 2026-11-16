package com.integrityfamily.sistemas.domain;

public enum ProjectDecision {
    CONTINUAR("Continuar"),
    CORREGIR("Corregir"),
    PAUSAR("Pausar"),
    CERRAR("Cerrar");

    private final String label;

    ProjectDecision(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static ProjectDecision from(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return ProjectDecision.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
