package com.integrityfamily.dto.home;

import java.time.Instant;
import java.util.UUID;

/** Respuesta de un comando ejecutado por el Family Action Engine. */
public record FamilyActionResult(
    String action,
    String status,
    UUID sprintId,
    Instant executedAt,
    boolean replayed
) {
}
