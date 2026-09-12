package com.integrityfamily.interop.canonical;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Equivalente canónico de una evidencia de tarea/misión (mapea desde
 * {@code checklist.TaskEvidence}). Traduce eventualmente a FHIR
 * DocumentReference (o Binary para el archivo crudo).
 *
 * @param mediaType IMAGE, VIDEO, DOCUMENT, TEXT
 */
@Builder
public record Evidence(
        String canonicalId,
        String subjectId,
        String description,
        String mediaType,
        String uri,
        LocalDateTime capturedAt
) {}
