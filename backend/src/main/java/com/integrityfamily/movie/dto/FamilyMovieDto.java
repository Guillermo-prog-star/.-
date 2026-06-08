package com.integrityfamily.movie.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record FamilyMovieDto(
    Long    id,
    Long    familyId,
    String  familyName,
    String  periodLabel,
    LocalDate periodStart,
    LocalDate periodEnd,
    // Estadísticas
    int     evidencesCount,
    int     gratitudesCount,
    int     missionsCompleted,
    int     crisesCount,
    int     ritualsCompleted,
    int     daysActive,
    int     bestStreak,
    Double  icfStart,
    Double  icfEnd,
    Double  icfDelta,
    // Narrativa
    String  openingLine,
    String  chapter1,
    String  chapter2,
    String  chapter3,
    String  mentorLetter,
    String  highlightQuote,
    LocalDateTime generatedAt
) {}
