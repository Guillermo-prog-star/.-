package com.integrityfamily.council.dto;

import jakarta.validation.constraints.NotBlank;

public record CouncilRequest(
    @NotBlank String question,  // la pregunta concreta de la familia
    String topic,               // DECISION | CRISIS | CONFLICTO | REFLEXION | LEGADO
    String context              // contexto adicional opcional
) {}
