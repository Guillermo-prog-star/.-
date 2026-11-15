package com.integrityfamily.domain;

/**
 * Paso de la práctica "Antes de concluir, pregunta" (ADR-018), usado como
 * clave de {@code journal_entries.explicit_responses}. Cada paso corresponde
 * a una columna de {@link JournalEntry}.
 */
public enum JournalStep {
    OBSERVATIONS,            // ¿Qué ocurrió de forma observable?
    INTERPRETATION,          // ¿Qué estoy concluyendo?
    BODY_SIGNAL,             // ¿Qué noto en el cuerpo?
    EMOTION,                 // ¿Cómo lo nombro?
    ALTERNATIVE_EXPLANATION, // ¿Qué me falta saber / qué otra explicación considero?
    REFLECTION,              // ¿Qué acción elijo y cómo lo comprobaré?
    LEARNING;                // ¿Qué ocurrió después? ¿Qué mantener, reparar o cambiar?

    /** Solo los pasos de comprobar y revisar admiten "no funcionó". */
    public boolean allowsNotHelpful() {
        return this == REFLECTION || this == LEARNING;
    }
}
