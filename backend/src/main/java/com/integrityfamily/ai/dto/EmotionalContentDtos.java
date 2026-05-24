package com.integrityfamily.ai.dto;

import lombok.Builder;

public class EmotionalContentDtos {

    public record ReflectionRequest(
        Long familyId,
        Long memberId,
        String reflection,
        Integer emotionalScore
    ) {}

    @Builder
    public record EmotionalInferenceDto(
        Integer empathy,          // 1 to 5
        Integer avoidance,        // 1 to 5
        Integer disconnection,    // 1 to 5
        Integer activePresence,   // 1 to 5
        Integer reactivity,       // 1 to 5
        String feedback,          // Párrafo de acompañamiento reflexivo
        String recommendedAction  // Micro-misión sugerida para sintonizar el hogar
    ) {}

    @Builder
    public record FamilyEmotionalStats(
        Double ioc,                        // Índice de Observación Consciente (0.0 a 100.0)
        Integer totalReflections,          // Total de reflexiones guardadas en el hogar
        Double averageEmpathy,             // Promedio de empatía
        Double averagePresence,            // Promedio de presencia
        Double averageReactivity           // Promedio de reactividad
    ) {}
}
