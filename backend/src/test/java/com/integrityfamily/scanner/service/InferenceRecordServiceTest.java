package com.integrityfamily.scanner.service;

import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.EvaluationStatus;
import com.integrityfamily.domain.Family;
import com.integrityfamily.risk.service.RiskAlgoV1Engine;
import com.integrityfamily.scanner.domain.EmotionalOperationalState;
import com.integrityfamily.scanner.domain.InferenceRecord;
import com.integrityfamily.scanner.domain.RuleActivation;
import com.integrityfamily.scanner.repository.InferenceRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InferenceRecordService — IF-CIS")
class InferenceRecordServiceTest {

    @Mock private InferenceRecordRepository inferenceRecordRepository;
    @Mock private EmotionalStateClassifier  stateClassifier;

    @InjectMocks
    private InferenceRecordService service;

    private Evaluation evaluation;
    private RiskAlgoV1Engine.AlgoResult algo;

    @BeforeEach
    void setUp() {
        Family family = Family.builder().id(5L).currentMilestone("M6").build();
        evaluation = Evaluation.builder()
                .id(42L).family(family)
                .status(EvaluationStatus.FINALIZED)
                .icf(55.0).riskLevel("MODERADO")
                .algorithmVersion("RISK_ALGO_V1")
                .finalizedAt(LocalDateTime.now())
                .build();

        algo = new RiskAlgoV1Engine.AlgoResult(
                Map.of("emociones", 3.0, "comunicacion", 3.0, "habitos", 3.0, "tiempos", 3.0),
                55.0, "MODERADO", "emociones",
                false, false,
                "COMUNICACION_CONSCIENTE", "Consciente", 2,
                List.of(), List.of(),
                new RiskAlgoV1Engine.UncertaintyVector(0.05, 0.05, 0.05, 0.05, 0.05, 0.10));
    }

    // ── createFromEvaluation ──────────────────────────────────────────────────

    @Nested
    @DisplayName("createFromEvaluation")
    class CreateFromEvaluation {

        @Test
        void persistsRecordWhenHashIsNew() {
            when(inferenceRecordRepository.existsByEvidenceHash(anyString())).thenReturn(false);
            when(stateClassifier.classify(5L)).thenReturn(EmotionalOperationalState.STABLE);
            when(inferenceRecordRepository.save(any(InferenceRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            InferenceRecord result = service.createFromEvaluation(evaluation, algo);

            assertThat(result).isNotNull();
            assertThat(result.getInferenceKey()).isEqualTo("ICF_CALC");
            assertThat(result.getEpistemicState()).isEqualTo("INFERRED");
            assertThat(result.getFamilyId()).isEqualTo(5L);
            assertThat(result.getEvaluationId()).isEqualTo(42L);
            assertThat(result.getOperationalState()).isEqualTo("STABLE");
        }

        @Test
        void returnsNullWhenHashAlreadyExists() {
            when(inferenceRecordRepository.existsByEvidenceHash(anyString())).thenReturn(true);

            InferenceRecord result = service.createFromEvaluation(evaluation, algo);

            assertThat(result).isNull();
            verify(inferenceRecordRepository, never()).save(any());
        }

        @Test
        void hashIsDeterministicForSameInput() {
            when(inferenceRecordRepository.existsByEvidenceHash(anyString())).thenReturn(false);
            when(stateClassifier.classify(5L)).thenReturn(EmotionalOperationalState.STABLE);
            when(inferenceRecordRepository.save(any(InferenceRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.createFromEvaluation(evaluation, algo);
            service.createFromEvaluation(evaluation, algo);

            ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
            verify(inferenceRecordRepository, times(2)).existsByEvidenceHash(hashCaptor.capture());
            assertThat(hashCaptor.getAllValues().get(0))
                    .isEqualTo(hashCaptor.getAllValues().get(1));
        }

        @Test
        void setsUncertaintyFromAlgoWhenPresent() {
            when(inferenceRecordRepository.existsByEvidenceHash(anyString())).thenReturn(false);
            when(stateClassifier.classify(5L)).thenReturn(EmotionalOperationalState.STABLE);
            when(inferenceRecordRepository.save(any(InferenceRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            InferenceRecord result = service.createFromEvaluation(evaluation, algo);

            assertThat(result.getUncertaintyTotal()).isEqualTo(0.10);
        }
    }

    // ── createFromRule ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createFromRule")
    class CreateFromRule {

        private RuleActivation activation;

        @BeforeEach
        void setUpActivation() {
            activation = new RuleActivation(7L, "relational_stress", 1,
                    "ESTRES_RELACIONAL", 0.80, "ALTO", List.of("high_risk", "avoidance"));
        }

        @Test
        void persistsRecordWithRuleKeyAsInferenceKey() {
            when(inferenceRecordRepository.existsByEvidenceHash(anyString())).thenReturn(false);
            when(stateClassifier.classify(5L)).thenReturn(EmotionalOperationalState.ESCALATING);
            when(inferenceRecordRepository.save(any(InferenceRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            InferenceRecord result = service.createFromRule(evaluation, algo, activation);

            assertThat(result).isNotNull();
            assertThat(result.getInferenceKey()).isEqualTo("relational_stress");
            assertThat(result.getRiskLevel()).isEqualTo("ALTO");
        }

        @Test
        void usesAlgoRiskWhenRuleRiskOutputIsNull() {
            RuleActivation noRiskOutput = new RuleActivation(
                    7L, "relational_stress", 1, null, 0.80, null, List.of("high_risk"));
            when(inferenceRecordRepository.existsByEvidenceHash(anyString())).thenReturn(false);
            when(stateClassifier.classify(5L)).thenReturn(EmotionalOperationalState.STABLE);
            when(inferenceRecordRepository.save(any(InferenceRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            InferenceRecord result = service.createFromRule(evaluation, algo, noRiskOutput);

            assertThat(result.getRiskLevel()).isEqualTo("MODERADO");
        }

        @Test
        void modulatesUncertaintyByConfidence() {
            when(inferenceRecordRepository.existsByEvidenceHash(anyString())).thenReturn(false);
            when(stateClassifier.classify(5L)).thenReturn(EmotionalOperationalState.STABLE);
            when(inferenceRecordRepository.save(any(InferenceRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            InferenceRecord result = service.createFromRule(evaluation, algo, activation);

            // uncertainty = 0.10 * (1 - 0.80) = 0.02
            assertThat(result.getUncertaintyTotal()).isEqualTo(0.10 * (1.0 - 0.80), within(0.001));
        }

        @Test
        void returnsNullOnDuplicateHash() {
            when(inferenceRecordRepository.existsByEvidenceHash(anyString())).thenReturn(true);

            InferenceRecord result = service.createFromRule(evaluation, algo, activation);

            assertThat(result).isNull();
            verify(inferenceRecordRepository, never()).save(any());
        }
    }

    // ── stabilize ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("stabilize")
    class Stabilize {
        @Test
        void changesEpistemicStateToStabilized() {
            InferenceRecord record = new InferenceRecord();
            record.setId(99L);
            record.setEpistemicState("INFERRED");
            when(inferenceRecordRepository.findById(99L)).thenReturn(Optional.of(record));
            when(inferenceRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.stabilize(99L);

            assertThat(record.getEpistemicState()).isEqualTo("STABILIZED");
            assertThat(record.getStabilizedAt()).isNotNull();
        }

        @Test
        void doesNotChangeAlreadyStabilizedRecord() {
            InferenceRecord record = new InferenceRecord();
            record.setId(99L);
            record.setEpistemicState("STABILIZED");
            when(inferenceRecordRepository.findById(99L)).thenReturn(Optional.of(record));

            service.stabilize(99L);

            verify(inferenceRecordRepository, never()).save(any());
        }

        @Test
        void doesNothingWhenRecordNotFound() {
            when(inferenceRecordRepository.findById(999L)).thenReturn(Optional.empty());

            service.stabilize(999L);

            verify(inferenceRecordRepository, never()).save(any());
        }
    }

    // ── within helper ─────────────────────────────────────────────────────────
    private static org.assertj.core.data.Offset<Double> within(double delta) {
        return org.assertj.core.data.Offset.offset(delta);
    }
}
