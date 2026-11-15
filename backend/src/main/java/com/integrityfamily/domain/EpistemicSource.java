package com.integrityfamily.domain;

/**
 * Cómo un miembro sabe lo que está narrando en una {@code BiographyAnswer}
 * (módulo {@code biography}, diario guiado biográfico): distingue el
 * recuerdo propio del relato ajeno o de una fuente documental. Etiqueta
 * opcional sobre la respuesta completa, no un desglose forzado por frase.
 */
public enum EpistemicSource {
    OBSERVED,
    TOLD_BY_OTHER,
    DOCUMENT,
    UNCERTAIN
}
