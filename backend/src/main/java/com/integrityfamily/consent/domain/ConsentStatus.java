package com.integrityfamily.consent.domain;

/**
 * Mismos cuatro estados que {@code dto.home.ConsentStatus} (usado hoy solo
 * para visibilidad de medios en el Hogar Digital) — se declara localmente en
 * vez de reutilizar esa clase para no acoplar el módulo de consentimiento a
 * un paquete de una feature de UI no relacionada.
 */
public enum ConsentStatus {
    GRANTED,
    REVOKED,
    PENDING,
    NOT_REQUIRED
}
