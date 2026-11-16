package com.integrityfamily.sistemas.domain;

public enum AdjustmentKind {
    DEJAR("Dejar"),
    REDUCIR("Reducir"),
    POSPONER("Posponer");

    private final String label;

    AdjustmentKind(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static AdjustmentKind from(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return AdjustmentKind.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
