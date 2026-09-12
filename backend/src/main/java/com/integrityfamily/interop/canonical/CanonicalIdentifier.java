package com.integrityfamily.interop.canonical;

import lombok.Builder;

/**
 * Identificador con espacio de nombres explícito, equivalente conceptual a
 * FHIR Identifier — pero sin depender de FHIR. Ancla la identidad de una
 * {@link Person} a un documento del mundo real (CC, TI, RC, CE, PA, NUIP...).
 *
 * @param system tipo/emisor del documento (ej. "CC", "TI", "RC", "CE", "PA")
 * @param value   número del documento
 */
@Builder
public record CanonicalIdentifier(String system, String value) {}
