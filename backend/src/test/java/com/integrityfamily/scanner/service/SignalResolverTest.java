package com.integrityfamily.scanner.service;

import com.integrityfamily.risk.service.RiskAlgoV1Engine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SignalResolver — IF-REE")
class SignalResolverTest {

    private final SignalResolver resolver = new SignalResolver();

    // ── Helper ────────────────────────────────────────────────────────────────

    private RiskAlgoV1Engine.AlgoResult algo(
            double icf, String risk, String critDim,
            boolean simulation, boolean relapse,
            Map<String, Double> dims,
            List<String> relapseFlags, List<String> mirrorFlags) {
        return new RiskAlgoV1Engine.AlgoResult(
                dims, icf, risk, critDim,
                simulation, relapse,
                "ESTABILIZACION_EMOCIONAL", "Consciente", 2,
                relapseFlags, mirrorFlags,
                new RiskAlgoV1Engine.UncertaintyVector(0.05, 0.05, 0.05, 0.05, 0.05, 0.10));
    }

    private Map<String, Double> dims(double emociones, double comunicacion,
                                     double habitos, double tiempos) {
        return Map.of("emociones", emociones, "comunicacion", comunicacion,
                      "habitos", habitos, "tiempos", tiempos);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("simulation_suspected")
    class SimulationSuspected {
        @Test
        void trueWhenFlagSet() {
            var a = algo(80, "BAJO", "emociones", true, false, dims(3,3,3,3), List.of(), List.of());
            assertThat(resolver.isPresent("simulation_suspected", a)).isTrue();
        }

        @Test
        void falseWhenFlagNotSet() {
            var a = algo(80, "BAJO", "emociones", false, false, dims(3,3,3,3), List.of(), List.of());
            assertThat(resolver.isPresent("simulation_suspected", a)).isFalse();
        }
    }

    @Nested
    @DisplayName("relapse_detected")
    class RelapseDetected {
        @Test
        void trueWhenFlagSet() {
            var a = algo(30, "ALTO", "emociones", false, true, dims(3,3,3,3), List.of("dim=emociones"), List.of());
            assertThat(resolver.isPresent("relapse_detected", a)).isTrue();
        }

        @Test
        void falseWhenFlagNotSet() {
            var a = algo(80, "BAJO", "emociones", false, false, dims(3,3,3,3), List.of(), List.of());
            assertThat(resolver.isPresent("relapse_detected", a)).isFalse();
        }
    }

    @Nested
    @DisplayName("dimension_collapse")
    class DimensionCollapse {
        @Test
        void trueWhenAnyDimensionBelow2() {
            var a = algo(25, "ALTO", "emociones", false, false, dims(1.5, 3, 3, 3), List.of(), List.of());
            assertThat(resolver.isPresent("dimension_collapse", a)).isTrue();
        }

        @Test
        void falseWhenAllDimensionsAbove2() {
            var a = algo(70, "BAJO", "emociones", false, false, dims(2.1, 3, 3, 3), List.of(), List.of());
            assertThat(resolver.isPresent("dimension_collapse", a)).isFalse();
        }

        @Test
        void falseWhenDimsMapEmpty() {
            var a = algo(70, "BAJO", "emociones", false, false, Map.of(), List.of(), List.of());
            assertThat(resolver.isPresent("dimension_collapse", a)).isFalse();
        }
    }

    @Nested
    @DisplayName("voice_tension")
    class VoiceTension {
        @Test
        void trueWhenRelapseFlagContainsEmociones() {
            var a = algo(50, "MODERADO", "emociones", false, false,
                    dims(3,3,3,3), List.of("dim=emociones"), List.of());
            assertThat(resolver.isPresent("voice_tension", a)).isTrue();
        }

        @Test
        void trueWhenMirrorFlagsNotEmpty() {
            var a = algo(50, "MODERADO", "emociones", false, false,
                    dims(3,3,3,3), List.of(), List.of("q42"));
            assertThat(resolver.isPresent("voice_tension", a)).isTrue();
        }

        @Test
        void falseWhenNeitherConditionMet() {
            var a = algo(80, "BAJO", "emociones", false, false, dims(3,3,3,3), List.of(), List.of());
            assertThat(resolver.isPresent("voice_tension", a)).isFalse();
        }
    }

    @Nested
    @DisplayName("interruptions")
    class Interruptions {
        @Test
        void trueWhenTwoOrMoreMirrorFlags() {
            var a = algo(50, "MODERADO", "emociones", false, false,
                    dims(3,3,3,3), List.of(), List.of("q1", "q2"));
            assertThat(resolver.isPresent("interruptions", a)).isTrue();
        }

        @Test
        void falseWhenOneMirrorFlag() {
            var a = algo(50, "MODERADO", "emociones", false, false,
                    dims(3,3,3,3), List.of(), List.of("q1"));
            assertThat(resolver.isPresent("interruptions", a)).isFalse();
        }
    }

    @Nested
    @DisplayName("avoidance")
    class Avoidance {
        @Test
        void trueWhenRelapseFlagContainsComunicacion() {
            var a = algo(50, "MODERADO", "comunicacion", false, false,
                    dims(3,3,3,3), List.of("dim=comunicacion"), List.of());
            assertThat(resolver.isPresent("avoidance", a)).isTrue();
        }

        @Test
        void falseWhenNoRelationDimensionFlagged() {
            var a = algo(80, "BAJO", "emociones", false, false,
                    dims(3,3,3,3), List.of("dim=habitos"), List.of());
            assertThat(resolver.isPresent("avoidance", a)).isFalse();
        }
    }

    @Nested
    @DisplayName("reduced_participation")
    class ReducedParticipation {
        @Test
        void trueWhenComunicacionBelow2_5() {
            var a = algo(40, "ALTO", "comunicacion", false, false, dims(3, 2.0, 3, 3), List.of(), List.of());
            assertThat(resolver.isPresent("reduced_participation", a)).isTrue();
        }

        @Test
        void falseWhenComunicacionAbove2_5() {
            var a = algo(70, "BAJO", "comunicacion", false, false, dims(3, 3.0, 3, 3), List.of(), List.of());
            assertThat(resolver.isPresent("reduced_participation", a)).isFalse();
        }
    }

    @Nested
    @DisplayName("chronic_fatigue_signals")
    class ChronicFatigueSignals {
        @Test
        void trueWhenBothConditionsMet() {
            var a = algo(30, "ALTO", "habitos", false, false, dims(2.4, 3, 1.8, 3), List.of(), List.of());
            assertThat(resolver.isPresent("chronic_fatigue_signals", a)).isTrue();
        }

        @Test
        void falseWhenOnlyHabitosLow() {
            var a = algo(50, "MODERADO", "habitos", false, false, dims(3.0, 3, 1.8, 3), List.of(), List.of());
            assertThat(resolver.isPresent("chronic_fatigue_signals", a)).isFalse();
        }

        @Test
        void falseWhenOnlyEmocionesLow() {
            var a = algo(50, "MODERADO", "emociones", false, false, dims(2.4, 3, 2.5, 3), List.of(), List.of());
            assertThat(resolver.isPresent("chronic_fatigue_signals", a)).isFalse();
        }
    }

    @Nested
    @DisplayName("high_risk")
    class HighRisk {
        @Test
        void trueForAlto() {
            var a = algo(35, "ALTO", "emociones", false, false, dims(3,3,3,3), List.of(), List.of());
            assertThat(resolver.isPresent("high_risk", a)).isTrue();
        }

        @Test
        void trueForCritico() {
            var a = algo(10, "CRITICO", "emociones", false, false, dims(3,3,3,3), List.of(), List.of());
            assertThat(resolver.isPresent("high_risk", a)).isTrue();
        }

        @Test
        void falseForBajo() {
            var a = algo(80, "BAJO", "emociones", false, false, dims(3,3,3,3), List.of(), List.of());
            assertThat(resolver.isPresent("high_risk", a)).isFalse();
        }
    }

    @Nested
    @DisplayName("low_icf")
    class LowIcf {
        @Test
        void trueWhenIcfBelow40() {
            var a = algo(35, "ALTO", "emociones", false, false, dims(3,3,3,3), List.of(), List.of());
            assertThat(resolver.isPresent("low_icf", a)).isTrue();
        }

        @Test
        void falseWhenIcfAbove40() {
            var a = algo(55, "MODERADO", "emociones", false, false, dims(3,3,3,3), List.of(), List.of());
            assertThat(resolver.isPresent("low_icf", a)).isFalse();
        }
    }

    @Nested
    @DisplayName("edge cases")
    class EdgeCases {
        @Test
        void nullSignalReturnsFalse() {
            var a = algo(80, "BAJO", "emociones", false, false, dims(3,3,3,3), List.of(), List.of());
            assertThat(resolver.isPresent(null, a)).isFalse();
        }

        @Test
        void unknownSignalReturnsFalse() {
            var a = algo(80, "BAJO", "emociones", false, false, dims(3,3,3,3), List.of(), List.of());
            assertThat(resolver.isPresent("nonexistent_signal_xyz", a)).isFalse();
        }

        @Test
        void signalNameIsCaseInsensitive() {
            var a = algo(80, "BAJO", "emociones", true, false, dims(3,3,3,3), List.of(), List.of());
            assertThat(resolver.isPresent("SIMULATION_SUSPECTED", a)).isTrue();
        }
    }
}
