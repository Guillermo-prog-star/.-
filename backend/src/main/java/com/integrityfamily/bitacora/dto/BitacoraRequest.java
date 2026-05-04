package com.integrityfamily.bitacora.dto;

import java.time.LocalDateTime;

/**
 * SDD: Contrato de Entrada para la Bitácora Cognitiva.
 */
public record BitacoraRequest(
    Long familyId,
    String relatedEntity,    // TASK | CRISIS | PLAN | EVALUATION
    Long relatedId,
    String learning,         // Lo que la familia descubrió
    String hypothesis,       // Lo que creen que pasará si cambian algo
    String action,           // El paso concreto a seguir
    String result            // Resultado observado (opcional al inicio)
) {}
