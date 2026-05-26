package com.integrityfamily.scanner.service;

import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.EvaluationStatus;
import com.integrityfamily.domain.Family;
import com.integrityfamily.risk.service.RiskAlgoV1Engine;
import com.integrityfamily.scanner.domain.FamilyAlert;
import com.integrityfamily.scanner.domain.InferenceRecord;
import com.integrityfamily.scanner.domain.RuleActivation;
import com.integrityfamily.scanner.repository.FamilyAlertRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlertEngine — IF-ALT")
class AlertEngineTest {

    @Mock private FamilyAlertRepository     alertRepo;
    @Mock private InferenceRecordRepository inferenceRepo;

    @InjectMocks
    private AlertEngine alertEngine;

    private static final Long FAMILY_ID = 10L;
    private static final Long EVAL_ID   = 20L;

    private Evaluation evaluation;

    @BeforeEach
    void setUp() {
        Family family = Family.builder().id(FAMILY_ID).currentMilestone("M6").build();
        evaluation = Evaluation.builder()
                .id(EVAL_ID).family(family)
                .status(EvaluationStatus.FINALIZED)
                .icf(30.0).riskLevel("ALTO")
                .finalizedAt(LocalDateTime.now())
                .build();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private RiskAlgoV1Engine.AlgoResult algo(boolean simulation, boolean relapse, String risk) {
        return new RiskAlgoV1Engine.AlgoResult(
                Map.of("emociones", 2.0, "comunicacion", 2.5, "habitos", 3.0, "tiempos", 3.0),
                30.0, risk, "emociones",
                simulation, relapse,
                "ESTABILIZACION_EMOCIONAL", "Reactiva", 4,
                relapse ? List.of("dim=emociones") : List.of(),
                List.of(),
                new RiskAlgoV1Engine.UncertaintyVector(0.05, 0.05, 0.05, 0.05, 0.05, 0.10));
    }

    private InferenceRecord inferenceRecord(String risk, String opState, boolean simulation, int daysAgo) {
        InferenceRecord r = new InferenceRecord();
        r.setFamilyId(FAMILY_ID);
        r.setInferenceKey("ICF_CALC");
        r.setRiskLevel(risk);
        r.setOperationalState(opState);
        r.setSimulationSuspected(simulation);
        r.setCreatedAt(Instant.now().minus(daysAgo, ChronoUnit.DAYS));
        return r;
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RELAPSE_CONFIRMED")
    class RelapseConfirmed {
        @Test
        void createsAlertWhenRelapseDetected() {
            when(alertRepo.existsByFamilyIdAndAlertTypeAndResolvedFalse(FAMILY_ID, "RELAPSE_CONFIRMED"))
                    .thenReturn(false);
            when(inferenceRepo.findByFamilyIdOrderByCreatedAtDesc(FAMILY_ID)).thenReturn(List.of());

            alertEngine.evaluate(evaluation, algo(false, true, "ALTO"), List.of());

            ArgumentCaptor<FamilyAlert> captor = ArgumentCaptor.forClass(FamilyAlert.class);
            verify(alertRepo, atLeastOnce()).save(captor.capture());
            assertThat(captor.getAllValues()).anyMatch(a -> "RELAPSE_CONFIRMED".equals(a.getAlertType()));
        }

        @Test
        void noAlertWhenRelapseNotDetected() {
            when(inferenceRepo.findByFamilyIdOrderByCreatedAtDesc(FAMILY_ID)).thenReturn(List.of());

            alertEngine.evaluate(evaluation, algo(false, false, "BAJO"), List.of());

            verify(alertRepo, never()).save(argThat(a -> "RELAPSE_CONFIRMED".equals(a.getAlertType())));
        }
    }

    @Nested
    @DisplayName("MULTI_RULE_ACTIVATION")
    class MultiRuleActivation {
        @Test
        void createsAlertWhenThreeOrMoreActivations() {
            when(alertRepo.existsByFamilyIdAndAlertTypeAndResolvedFalse(anyLong(), anyString()))
                    .thenReturn(false);
            when(inferenceRepo.findByFamilyIdOrderByCreatedAtDesc(FAMILY_ID)).thenReturn(List.of());

            List<RuleActivation> activations = List.of(
                    new RuleActivation(1L, "rule_a", 1, null, 0.8, null, List.of()),
                    new RuleActivation(2L, "rule_b", 1, null, 0.7, null, List.of()),
                    new RuleActivation(3L, "rule_c", 1, null, 0.6, null, List.of())
            );

            alertEngine.evaluate(evaluation, algo(false, false, "ALTO"), activations);

            ArgumentCaptor<FamilyAlert> captor = ArgumentCaptor.forClass(FamilyAlert.class);
            verify(alertRepo, atLeastOnce()).save(captor.capture());
            assertThat(captor.getAllValues()).anyMatch(a -> "MULTI_RULE_ACTIVATION".equals(a.getAlertType()));
        }

        @Test
        void noAlertWhenFewerThanThreeActivations() {
            when(inferenceRepo.findByFamilyIdOrderByCreatedAtDesc(FAMILY_ID)).thenReturn(List.of());

            List<RuleActivation> activations = List.of(
                    new RuleActivation(1L, "rule_a", 1, null, 0.8, null, List.of()),
                    new RuleActivation(2L, "rule_b", 1, null, 0.7, null, List.of())
            );

            alertEngine.evaluate(evaluation, algo(false, false, "BAJO"), activations);

            verify(alertRepo, never()).save(argThat(a -> "MULTI_RULE_ACTIVATION".equals(a.getAlertType())));
        }
    }

    @Nested
    @DisplayName("SIMULATION_REPEAT")
    class SimulationRepeat {
        @Test
        void createsAlertWhenTwoRecentSimulations() {
            when(alertRepo.existsByFamilyIdAndAlertTypeAndResolvedFalse(anyLong(), anyString()))
                    .thenReturn(false);

            List<InferenceRecord> history = List.of(
                    inferenceRecord("ALTO", "ESCALATING", true, 1),
                    inferenceRecord("MODERADO", "STABLE", true, 5)
            );
            when(inferenceRepo.findByFamilyIdOrderByCreatedAtDesc(FAMILY_ID)).thenReturn(history);

            alertEngine.evaluate(evaluation, algo(true, false, "MODERADO"), List.of());

            ArgumentCaptor<FamilyAlert> captor = ArgumentCaptor.forClass(FamilyAlert.class);
            verify(alertRepo, atLeastOnce()).save(captor.capture());
            assertThat(captor.getAllValues()).anyMatch(a -> "SIMULATION_REPEAT".equals(a.getAlertType()));
        }

        @Test
        void noAlertWhenSimulationNotSuspected() {
            when(inferenceRepo.findByFamilyIdOrderByCreatedAtDesc(FAMILY_ID)).thenReturn(List.of());

            alertEngine.evaluate(evaluation, algo(false, false, "BAJO"), List.of());

            verify(alertRepo, never()).save(argThat(a -> "SIMULATION_REPEAT".equals(a.getAlertType())));
        }
    }

    @Nested
    @DisplayName("CONSECUTIVE_HIGH_RISK")
    class ConsecutiveHighRisk {
        @Test
        void createsAlertWhenTwoRecentHighRiskInferences() {
            when(alertRepo.existsByFamilyIdAndAlertTypeAndResolvedFalse(anyLong(), anyString()))
                    .thenReturn(false);

            List<InferenceRecord> history = List.of(
                    inferenceRecord("ALTO",   "ESCALATING", false, 5),
                    inferenceRecord("CRITICO", "CRITICAL",  false, 10)
            );
            when(inferenceRepo.findByFamilyIdOrderByCreatedAtDesc(FAMILY_ID)).thenReturn(history);

            alertEngine.evaluate(evaluation, algo(false, false, "ALTO"), List.of());

            ArgumentCaptor<FamilyAlert> captor = ArgumentCaptor.forClass(FamilyAlert.class);
            verify(alertRepo, atLeastOnce()).save(captor.capture());
            assertThat(captor.getAllValues()).anyMatch(a -> "CONSECUTIVE_HIGH_RISK".equals(a.getAlertType()));
        }

        @Test
        void noAlertWhenCurrentRiskIsLow() {
            when(inferenceRepo.findByFamilyIdOrderByCreatedAtDesc(FAMILY_ID)).thenReturn(List.of());

            alertEngine.evaluate(evaluation, algo(false, false, "BAJO"), List.of());

            verify(alertRepo, never()).save(argThat(a -> "CONSECUTIVE_HIGH_RISK".equals(a.getAlertType())));
        }
    }

    @Nested
    @DisplayName("CRITICAL_STATE_SUSTAINED")
    class CriticalStateSustained {
        @Test
        void createsAlertWhenThreeConsecutiveCriticalInferences() {
            when(alertRepo.existsByFamilyIdAndAlertTypeAndResolvedFalse(anyLong(), anyString()))
                    .thenReturn(false);

            List<InferenceRecord> history = List.of(
                    inferenceRecord("CRITICO", "CRITICAL", false, 1),
                    inferenceRecord("CRITICO", "CRITICAL", false, 5),
                    inferenceRecord("CRITICO", "CRITICAL", false, 10)
            );
            when(inferenceRepo.findByFamilyIdOrderByCreatedAtDesc(FAMILY_ID)).thenReturn(history);

            alertEngine.evaluate(evaluation, algo(false, false, "BAJO"), List.of());

            ArgumentCaptor<FamilyAlert> captor = ArgumentCaptor.forClass(FamilyAlert.class);
            verify(alertRepo, atLeastOnce()).save(captor.capture());
            assertThat(captor.getAllValues()).anyMatch(a -> "CRITICAL_STATE_SUSTAINED".equals(a.getAlertType()));
        }

        @Test
        void noAlertWhenFewerThanThreeCritical() {
            List<InferenceRecord> history = List.of(
                    inferenceRecord("CRITICO", "CRITICAL", false, 1),
                    inferenceRecord("CRITICO", "CRITICAL", false, 5)
            );
            when(inferenceRepo.findByFamilyIdOrderByCreatedAtDesc(FAMILY_ID)).thenReturn(history);

            alertEngine.evaluate(evaluation, algo(false, false, "BAJO"), List.of());

            verify(alertRepo, never()).save(argThat(a -> "CRITICAL_STATE_SUSTAINED".equals(a.getAlertType())));
        }
    }

    @Nested
    @DisplayName("Idempotencia")
    class Idempotency {
        @Test
        void doesNotDuplicateExistingActiveAlert() {
            when(alertRepo.existsByFamilyIdAndAlertTypeAndResolvedFalse(FAMILY_ID, "RELAPSE_CONFIRMED"))
                    .thenReturn(true);
            when(inferenceRepo.findByFamilyIdOrderByCreatedAtDesc(FAMILY_ID)).thenReturn(List.of());

            alertEngine.evaluate(evaluation, algo(false, true, "ALTO"), List.of());

            verify(alertRepo, never()).save(argThat(a -> "RELAPSE_CONFIRMED".equals(a.getAlertType())));
        }
    }
}
