package com.integrityfamily.ritual.domain;

public enum RitualStatus {
    PENDING,    // Detectado, no mostrado aún
    ACTIVE,     // La familia lo está viviendo
    COMPLETED,  // Completado
    DISMISSED   // La familia lo omitió
}
