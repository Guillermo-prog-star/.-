package com.integrityfamily.dto.home;

import java.util.List;

/**
 * Payload del comando {@code accept-first-sprint} (Return Stage). Todos los campos son
 * opcionales: el Home aún no recolecta un formulario para esta acción (es un solo botón
 * "Aceptar Primer Sprint"), así que el motor aplica valores por defecto neutrales cuando
 * el cliente no los envía.
 */
public record AcceptFirstSprintRequest(
    String objective,
    String riskDimension,
    Integer durationDays,
    List<String> missions
) {
}
