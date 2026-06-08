package com.integrityfamily.tree.dto;

import jakarta.validation.constraints.NotBlank;

public record MessageRequest(
    @NotBlank String authorName,
    String subject,
    @NotBlank String content,
    String messageType,   // LETTER | WISDOM | WARNING | BLESSING
    Long toFamilyId,      // null = para todas las generaciones
    Integer openInYear    // null = abierto inmediatamente
) {}
