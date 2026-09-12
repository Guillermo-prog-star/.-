package com.integrityfamily.familyhome.port;

public record AssessmentProgressSnapshot(
    int completedQuestions,
    int totalQuestions
) {
}
