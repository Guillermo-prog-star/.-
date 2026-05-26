package com.integrityfamily.dto;

import com.integrityfamily.domain.Evaluation;
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
                        Double healthyIndex,   // antes: globalScore — renombrado para coincidir con el contrato frontend
                        Long riskSnapshotId,
                        String aiReport,
                        Boolean hasCrisis,
                        // Nuevos campos RISK_ALGO_V1 Taxonomía v2
                        Boolean simulationSuspected,
                        Boolean relapseDetected,
                        String suggestedMissionGenerator,
                        String consciousnessLabel,
                        Integer consciousnessLevel,
                        List<String> relapseFlags,
                        List<String> mirrorFlags) {
        }

        public record DimensionScoreDto(
                        String dimension,
                        Double score,
                        Double normalizedScore) {   // antes: percentage — renombrado para coincidir con frontend
        }

        public record EvaluationResponse(
                        Long id,
                        Long familyId,
                        Long memberId,
                        EvaluationStatus status,
                        LocalDateTime startedAt,
                        LocalDateTime finalizedAt,
                        Double icf,
                        String riskLevel,
                        String criticalDimension) {
        }

        /**
         * Resultado completo de finalizar una evaluación:
         * contiene tanto la entidad persistida como el resultado detallado del algoritmo.
         */
        public record FinalizeResult(
                        Evaluation evaluation,
                        EvaluationResultResponse algoResult) {
        }

        public record TimelineEntryDto(
                        Long evaluationId,
                        LocalDateTime finalizedAt,
                        Double healthyIndex,
                        String riskLevel,
                        String criticalDimension,
                        String algorithmVersion,
                        /** IF-TOS: EMERGING|STABLE|ESCALATING|CRITICAL|RECOVERING|RESOLVED */
                        String operationalState,
                        /** IF-SUM: incertidumbre total 0.0–1.0 */
                        Double uncertaintyTotal) {
        }

        // ── Flujo incremental (mobile-first) ──────────────────────────────────

        /**
         * Cuerpo de una respuesta individual enviada durante el cuestionario.
         * Soporta escala 1-5 (value) y preguntas dicotómicas Sí/No (booleanAnswer).
         * Si solo se envía booleanAnswer, el backend lo convierte: true → 5, false → 1.
         */
        public record SaveAnswerRequest(
                        @NotNull(message = "questionId es obligatorio")
                        Long questionId,
                        @Min(value = 1, message = "El valor mínimo es 1")
                        @Max(value = 5, message = "El valor máximo es 5")
                        Integer value,
                        /** true = Sí, false = No. Ignorado si 'value' está presente. */
                        Boolean booleanAnswer) {

                /** Resuelve el valor efectivo en escala 1-5. */
                public int effectiveScore() {
                        if (value != null) return value;
                        if (booleanAnswer != null) return booleanAnswer ? 5 : 1;
                        return 3; // neutral si no se especifica nada
                }
        }

        /**
         * Una respuesta ya persistida, con metadatos para reanudar el cuestionario.
         */
        public record SavedAnswerDto(
                        Long questionId,
                        String questionKey,
                        Integer score,
                        Boolean booleanAnswer,
                        String dimension,
                        String diagnosticDimension,
                        String consciousnessLevel,
                        LocalDateTime answeredAt) {
        }

        /**
         * Estado de progreso del cuestionario.
         * El frontend lo usa para mostrar "X de Y respondidas" y habilitar el botón Finalizar.
         *
         * @param evaluationId   ID de la evaluación activa
         * @param answered       Preguntas respondidas hasta ahora
         * @param totalExpected  Total esperado (normalmente 20)
         * @param canFinalize    true cuando hay suficientes respuestas para calcular el ICF
         * @param answers        Lista de respuestas guardadas (para retomar donde se dejó)
         */
        public record AnswerProgressResponse(
                        Long evaluationId,
                        int answered,
                        int totalExpected,
                        boolean canFinalize,
                        List<SavedAnswerDto> answers) {
        }
}
