package com.integrityfamily.domain;

/**
 * Respuesta explícita a un paso del diario (ADR-018, Amendment 3). Es un
 * dato, no una ausencia: "no identifico ninguna señal" no equivale a no
 * haber respondido (que se representa con la ausencia de la clave).
 */
public enum ExplicitResponse {
    NONE_NOTICED, // "No identifico ninguna señal"
    UNKNOWN,      // "No sé"
    DECLINED,     // "Prefiero no compartir" -- nunca se muestra a otros miembros
    NOT_HELPFUL   // "No funcionó" -- solo en REFLECTION y LEARNING
}
