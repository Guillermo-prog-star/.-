package com.integrityfamily.domain;

/**
 * SDD SPEC 6.3: Estados de evolución de evidencias conductuales.
 */
public enum EvidenceStatus {
    PENDING,          // Esperando envío de evidencia
    SUBMITTED,        // Enviada por la familia, lista para evaluar
    UNDER_REVIEW,     // Analizándose por Sentinel AI o Terapeuta
    VALIDATED,        // Aprobada, suma puntaje e ICF
    REJECTED,         // Rechazada, requiere reenvío
    EXPIRED           // Fecha límite superada sin entrega
}
