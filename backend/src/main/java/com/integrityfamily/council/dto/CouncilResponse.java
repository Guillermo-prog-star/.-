package com.integrityfamily.council.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CouncilResponse(
    Long   familyId,
    String familyName,
    String question,
    String topic,
    String councilResponse,   // la respuesta generada
    boolean hasConstitution,  // si la familia tiene constitución definida
    boolean hasDna,           // si la familia tiene ADN sintetizado
    List<String> sourcesUsed, // qué fuentes usó el consejo para responder
    LocalDateTime consultedAt
) {}
