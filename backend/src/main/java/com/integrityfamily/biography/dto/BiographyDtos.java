package com.integrityfamily.biography.dto;

import com.integrityfamily.domain.EntryVisibility;
import com.integrityfamily.domain.EpistemicSource;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.List;

public class BiographyDtos {

    private BiographyDtos() {}

    public record QuestionResponse(
            Long id,
            String questionKey,
            String promptText,
            String deepenText,
            Integer sortOrder
    ) {}

    public record ChapterResponse(
            Integer chapterNumber,
            String chapterTitle,
            List<QuestionResponse> questions
    ) {}

    public record AnswerRequest(
            @NotBlank(message = "La respuesta no puede estar vacía") String answerText,
            EpistemicSource epistemicSource,
            String occurredPeriod,
            String meaningThen,
            String currentMeaning,
            String relatedSourceNote,
            String perceivedEventKey,
            @Min(0) @Max(100) Integer confidenceLevel,
            EntryVisibility visibility,
            Boolean completed,
            String correctionNote
    ) {}

    public record AnswerResponse(
            Long questionId,
            String questionKey,
            String answerText,
            EpistemicSource epistemicSource,
            String occurredPeriod,
            String meaningThen,
            String currentMeaning,
            String relatedSourceNote,
            String perceivedEventKey,
            Integer confidenceLevel,
            EntryVisibility visibility,
            String correctionNote,
            LocalDateTime completedAt,
            LocalDateTime updatedAt
    ) {}
}
