package com.integrityfamily.domain;

/**
 * Gobierna quien puede ver una fila individual de JournalEntry/CriticalDay
 * (ADR-012, H2: perspectivas multiples sobre un mismo evento).
 *
 * Nunca automatico: solo el propio autor puede pasar una fila de PRIVATE a
 * SHARED_WITH_FAMILY. No confundir con el modulo `consent` (V104), que
 * gobierna compartir con un tercero externo a la familia -- esto gobierna
 * visibilidad entre miembros de la misma familia.
 */
public enum EntryVisibility {
    PRIVATE,
    SHARED_WITH_FAMILY
}
