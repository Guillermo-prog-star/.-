package com.integrityfamily.scanner.service;

import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.EvaluationStatus;
import com.integrityfamily.domain.Family;
import com.integrityfamily.risk.service.RiskAlgoV1Engine;
import com.integrityfamily.scanner.domain.EmotionalRule;
import com.integrityfamily.scanner.domain.RuleActivation;
import com.integrityfamily.scanner.repository.EmotionalRuleRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RuleExecutionEngine — IF-REE")
class RuleExecutionEngineTest {

    @Mock private EmotionalRuleRepository ruleRepo;
    @Mock private SignalResolver          signalResolver;

    @InjectMocks
    private RuleExecutionEngine engine;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private RiskAlgoV1Engine.AlgoResult baseAlgo() {
        return new RiskAlgoV1Engine.AlgoResult(
                Map.of("emociones", 2.0, "comunicacion", 3.0, "habitos", 3.0, "tiempos", 3.0),
                35.0, "ALTO", "emociones",
                false, false,
                "ESTABILIZACION_EMOCIONAL", "Reactiva", 4,
                List.of(), List.of(),
                new RiskAlgoV1Engine.UncertaintyVector(0.05, 0.05, 0.05, 0.05, 0.05, 0.10));
    }

    private Evaluation evalWith(String milestone, String memberRole) {
        Family family = Family.builder().id(1L).currentMilestone(milestone).build();
        return Evaluation.builder()
                .id(100L).family(family)
                .status(EvaluationStatus.FINALIZED)
                .icf(35.0).riskLevel("ALTO")
                .finalizedAt(LocalDateTime.now())
                .build();
    }

    private EmotionalRule rule(String ruleKey, String milestoneScope, String memberRole,
                               List<String> signals) {
        return EmotionalRule.builder()
                .id(1L)
                .ruleKey(ruleKey)
                .version(1)
                .active(true)
                .milestoneScope(milestoneScope)
                .memberRole(memberRole)
                .requiredSignals(signals)
                .confidenceBase(0.80)
                .projectionLabel("TEST_LABEL")
                .build();
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Activación de reglas")
    class RuleActivationTests {
        @Test
        void activatesRuleWhenAllSignalsPresent() {
            EmotionalRule r = rule("stress_test", "*", "*", List.of("high_risk", "low_icf"));
            when(ruleRepo.findByActiveTrue()).thenReturn(List.of(r));
            var algo = baseAlgo();
            when(signalResolver.isPresent("high_risk", algo)).thenReturn(true);
            when(signalResolver.isPresent("low_icf",   algo)).thenReturn(true);

            List<RuleActivation> result = engine.evaluateRules(algo, evalWith("M6", null), null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).ruleKey()).isEqualTo("stress_test");
        }

        @Test
        void doesNotActivateWhenSignalPartiallyMissing() {
            EmotionalRule r = rule("stress_test", "*", "*", List.of("high_risk", "low_icf"));
            when(ruleRepo.findByActiveTrue()).thenReturn(List.of(r));
            var algo = baseAlgo();
            when(signalResolver.isPresent("high_risk", algo)).thenReturn(true);
            when(signalResolver.isPresent("low_icf",   algo)).thenReturn(false);

            List<RuleActivation> result = engine.evaluateRules(algo, evalWith("M6", null), null);

            assertThat(result).isEmpty();
        }

        @Test
        void returnsEmptyWhenNoActiveRules() {
            when(ruleRepo.findByActiveTrue()).thenReturn(List.of());

            List<RuleActivation> result = engine.evaluateRules(baseAlgo(), evalWith("M6", null), null);

            assertThat(result).isEmpty();
        }

        @Test
        void skipsRuleWithNoRequiredSignals() {
            EmotionalRule r = rule("empty_signals", "*", "*", List.of());
            when(ruleRepo.findByActiveTrue()).thenReturn(List.of(r));

            List<RuleActivation> result = engine.evaluateRules(baseAlgo(), evalWith("M6", null), null);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Filtro por rol (memberRole)")
    class RoleScope {
        @Test
        void activatesWhenRoleMatches() {
            EmotionalRule r = rule("padre_rule", "*", "PADRE", List.of("high_risk"));
            when(ruleRepo.findByActiveTrue()).thenReturn(List.of(r));
            var algo = baseAlgo();
            when(signalResolver.isPresent("high_risk", algo)).thenReturn(true);

            List<RuleActivation> result = engine.evaluateRules(algo, evalWith("M6", "PADRE"), "PADRE");

            assertThat(result).hasSize(1);
        }

        @Test
        void doesNotActivateWhenRoleDiffers() {
            EmotionalRule r = rule("padre_rule", "*", "PADRE", List.of("high_risk"));
            when(ruleRepo.findByActiveTrue()).thenReturn(List.of(r));

            List<RuleActivation> result = engine.evaluateRules(baseAlgo(), evalWith("M6", "MADRE"), "MADRE");

            assertThat(result).isEmpty();
        }

        @Test
        void wildcardRoleMatchesAnyRole() {
            EmotionalRule r = rule("universal_rule", "*", "*", List.of("high_risk"));
            when(ruleRepo.findByActiveTrue()).thenReturn(List.of(r));
            var algo = baseAlgo();
            when(signalResolver.isPresent("high_risk", algo)).thenReturn(true);

            List<RuleActivation> result = engine.evaluateRules(algo, evalWith("M6", "ADOLESCENTE"), "ADOLESCENTE");

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Filtro por hito (milestoneScope)")
    class MilestoneScope {
        @Test
        void wildcardScopeMatchesAnyMilestone() {
            EmotionalRule r = rule("any_milestone", "*", "*", List.of("high_risk"));
            when(ruleRepo.findByActiveTrue()).thenReturn(List.of(r));
            var algo = baseAlgo();
            when(signalResolver.isPresent("high_risk", algo)).thenReturn(true);

            List<RuleActivation> result = engine.evaluateRules(algo, evalWith("M99", null), null);

            assertThat(result).hasSize(1);
        }

        @Test
        void exactScopeMatchesCurrentMilestone() {
            EmotionalRule r = rule("m6_rule", "M6", "*", List.of("high_risk"));
            when(ruleRepo.findByActiveTrue()).thenReturn(List.of(r));
            var algo = baseAlgo();
            when(signalResolver.isPresent("high_risk", algo)).thenReturn(true);

            List<RuleActivation> result = engine.evaluateRules(algo, evalWith("M6", null), null);

            assertThat(result).hasSize(1);
        }

        @Test
        void exactScopeDoesNotMatchDifferentMilestone() {
            EmotionalRule r = rule("m6_rule", "M6", "*", List.of("high_risk"));
            when(ruleRepo.findByActiveTrue()).thenReturn(List.of(r));

            List<RuleActivation> result = engine.evaluateRules(baseAlgo(), evalWith("M12", null), null);

            assertThat(result).isEmpty();
        }
    }

    @Test
    @DisplayName("Error en una regla no bloquea las demás")
    void exceptionInOneRuleDoesNotBlockOthers() {
        EmotionalRule bad  = rule("bad_rule",  "*", "*", List.of("broken_signal"));
        EmotionalRule good = rule("good_rule", "*", "*", List.of("high_risk"));
        when(ruleRepo.findByActiveTrue()).thenReturn(List.of(bad, good));
        var algo = baseAlgo();
        when(signalResolver.isPresent("broken_signal", algo)).thenThrow(new RuntimeException("signal error"));
        when(signalResolver.isPresent("high_risk", algo)).thenReturn(true);

        List<RuleActivation> result = engine.evaluateRules(algo, evalWith("M6", null), null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).ruleKey()).isEqualTo("good_rule");
    }
}
