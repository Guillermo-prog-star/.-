package com.integrityfamily.interop.canonical;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Nota de un profesional (guardian/acompañante) sobre la familia. Traduce
 * eventualmente a FHIR ClinicalImpression o Composition según el nivel de
 * estructuración del contenido.
 */
@Builder
public record ProfessionalNote(
        String canonicalId,
        String subjectId,
        String authorName,
        String authorRole,
        String content,
        LocalDateTime recordedAt
) {}
