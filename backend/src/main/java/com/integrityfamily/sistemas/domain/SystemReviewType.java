package com.integrityfamily.sistemas.domain;

public enum SystemReviewType {
    DAILY("Revisión diaria"),
    WEEKLY("Revisión semanal"),
    MONTHLY("Revisión mensual"),
    ANNUAL("Revisión anual"),
    CHANGE("Ajuste por cambio");

    private final String label;

    SystemReviewType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static SystemReviewType from(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return SystemReviewType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
