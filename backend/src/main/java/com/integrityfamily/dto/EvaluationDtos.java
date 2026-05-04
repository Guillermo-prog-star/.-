package com.integrityfamily.dto;

import com.integrityfamily.domain.EvaluationStatus;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class EvaluationDtos {

        public record EvaluationStartRequest(
                        @NotNull Long familyId,
                        Long memberId) {
        }

        public record AnswerDto(
                        @NotNull Long questionId,
                        @NotNull @Min(1) @Max(5) Integer value,
                        // Soporte para legado del frontend
                        Integer answerValue) {
                public Integer getEffectiveValue() {
                        return value != null ? value : answerValue;
                }
        }

        public record EvaluationFinalizeRequest(
                        @NotEmpty List<AnswerDto> answers,
                        Double icf,
                        Boolean hasCrisis,
                        Map<String, Double> dimensionScores) {
        }

        public record EvaluationResultResponse(
                        Long evaluationId,
                        Long familyId,
                        String riskLevel,
                        List<DimensionScoreDto> dimensionScores,
                        Double globalScore,
                        Long riskSnapshotId,
                        String aiReport,
                        Boolean hasCrisis) {
        }

        public record DimensionScoreDto(
                        String dimension,
                        Double score,
                        Double percentage) {
        }

        public record EvaluationResponse(
                        Long id,
                        Long familyId,
                        Long memberId,
                        EvaluationStatus status,
                        LocalDateTime startedAt,
                        LocalDateTime finalizedAt) {
        }
}
