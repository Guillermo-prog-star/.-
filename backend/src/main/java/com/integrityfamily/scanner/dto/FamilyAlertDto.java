package com.integrityfamily.scanner.dto;

import java.time.Instant;

/** IF-ALT: Proyección de una alerta clínica para el frontend. */
public record FamilyAlertDto(
        Long    id,
        Long    familyId,
        String  alertType,
        String  severity,
        String  title,
        String  detail,
        String  inferenceKey,
        Long    evaluationId,
        boolean resolved,
        Instant resolvedAt,
        Instant createdAt
) {}
