package com.integrityfamily.tree.dto;

import java.util.List;

/**
 * Herencia que una familia recibe de sus ancestros.
 * Todo lo que las generaciones anteriores dejaron.
 */
public record HeritageDto(
    Long familyId,
    String familyName,
    List<AncestorHeritage> ancestors
) {
    public record AncestorHeritage(
        Long familyId,
        String familyName,
        String familyCode,
        int generation,
        // Del legado
        String foundingPrinciple,
        String familyMission,
        String familyVision,
        String historyLessons,
        String historyRecognition,
        // Del ADN
        String dnaValues,
        String dnaNarrativeIa,
        // Cartas legibles
        List<MessageSummary> readableMessages,
        // Contadores de memoria
        long evidenceCount,
        long gratitudeCount
    ) {}

    public record MessageSummary(
        Long id,
        String subject,
        String content,
        String authorName,
        String messageType,
        int fromYear
    ) {}
}
