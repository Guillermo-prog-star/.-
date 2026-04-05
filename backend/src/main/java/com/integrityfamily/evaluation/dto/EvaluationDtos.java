package com.integrityfamily.evaluation.dto;
import com.integrityfamily.evaluation.domain.EvaluationStatus;
import com.integrityfamily.risk.domain.RiskLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal; import java.time.LocalDateTime; import java.util.List;

public class EvaluationDtos {
    public record EvaluationStartRequest(@NotNull Long familyId, Long memberId) {}
    public record AnswerItem(@NotNull Long questionId, @NotNull @Min(1) @Max(5) Integer answerValue) {}
    public record EvaluationFinalizeRequest(@NotEmpty List<@Valid AnswerItem> answers) {}
    public record EvaluationResponse(Long id, Long familyId, Long memberId, EvaluationStatus status, LocalDateTime startedAt, LocalDateTime finalizedAt) {}
    public record QuestionResponse(Long id, String questionText, String dimension, String bloque) {}
    public record EvaluationResultResponse(
            Long evaluationId, Long familyId, RiskLevel riskLevel,
            BigDecimal scoreEmotions, BigDecimal scoreCommunication,
            BigDecimal scoreHabits, BigDecimal scoreTimes,
            BigDecimal globalScore, Long riskSnapshotId, String aiReport) {}
    public record EvaluationHistoryResponse(
            Long id, Long familyId, Long memberId, String memberName,
            EvaluationStatus status, LocalDateTime startedAt, LocalDateTime finalizedAt) {}
}
