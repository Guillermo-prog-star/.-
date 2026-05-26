package com.integrityfamily.scanner.domain;

/**
 * IF-TOS: Estados operacionales del scanner emocional familiar.
 *
 * Representan la trayectoria dinámica de la familia, no solo el estado puntual.
 * Cada transición entre estados es un evento formal en el timeline.
 *
 * Orden natural de ciclo: EMERGING → STABLE ↔ ESCALATING → CRITICAL → RECOVERING → RESOLVED
 */
public enum EmotionalOperationalState {

    /** Primera evaluación: línea de base establecida, sin historial comparativo. */
    EMERGING,

    /** Sin cambio significativo (Δ ICF < ±5 en la última evaluación). */
    STABLE,

    /** Deterioro progresivo: ≥2 evaluaciones consecutivas empeorando. */
    ESCALATING,

    /** riskLevel=CRITICO o cualquier dimensión < 25: requiere intervención inmediata. */
    CRITICAL,

    /** Mejora desde estado CRITICAL o ALTO: recuperación en curso. */
    RECOVERING,

    /** ≥2 evaluaciones consecutivas en BAJO con ICF ≥ 70: estabilización confirmada. */
    RESOLVED
}
