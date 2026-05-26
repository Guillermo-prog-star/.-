package com.integrityfamily.scanner.service;

import com.integrityfamily.risk.service.RiskAlgoV1Engine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DeterministicExplanationPipeline — IF-DEP")
class DeterministicExplanationPipelineTest {

    private final DeterministicExplanationPipeline pipeline = new DeterministicExplanationPipeline();

    // ── Helper ────────────────────────────────────────────────────────────────

    private RiskAlgoV1Engine.AlgoResult algo(String risk, String critDim, boolean relapse,
                                              boolean simulation, double uncertainty,
                                              List<String> relapseFlags) {
        boolean highUncert = uncertainty > 0.5;
        return new RiskAlgoV1Engine.AlgoResult(
                Map.of("emociones", 3.0, "comunicacion", 3.0, "habitos", 3.0, "tiempos", 3.0),
                60.0, risk, critDim,
                simulation, relapse,
                "COMUNICACION_CONSCIENTE", "Consciente", 2,
                relapseFlags, List.of(),
                new RiskAlgoV1Engine.UncertaintyVector(0.05, 0.05, 0.05,
                        highUncert ? 0.90 : 0.05, 0.05, uncertainty));
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("buildFamiliarNarrative — estructura")
    class NarrativeStructure {
        @Test
        void containsMandatorySections() {
            var a = algo("BAJO", "emociones", false, false, 0.10, List.of());
            String narrative = pipeline.buildFamiliarNarrative(a, "PADRE");

            assertThat(narrative)
                    .contains("[DIAGNÓSTICO CONSCIENTE]")
                    .contains("Observación:")
                    .contains("Recomendación:")
                    .contains("Misión activada:")
                    .contains("Nivel de consciencia:");
        }

        @Test
        void isDeterministicForSameInput() {
            var a = algo("MODERADO", "comunicacion", false, false, 0.15, List.of());
            String first  = pipeline.buildFamiliarNarrative(a, "MADRE");
            String second = pipeline.buildFamiliarNarrative(a, "MADRE");
            assertThat(first).isEqualTo(second);
        }
    }

    @Nested
    @DisplayName("Normalización de rol")
    class RoleNormalization {
        @ParameterizedTest(name = "role={0} → {1}")
        @CsvSource({
                "PADRE,      Se observa estabilidad en el liderazgo",
                "padre,      Se observa estabilidad en el liderazgo",
                "MADRE,      Se observa equilibrio en la carga mental",
                "ADOLESCENTE,Se observa expresión emocional segura",
                "NINO,       Se observan hábitos positivos",
                "NIÑO,       Se observan hábitos positivos",
                "HIJA,       Se observan hábitos positivos",
                "HIJO,       Se observan hábitos positivos"
        })
        void narrativeContainsRoleSpecificObservation(String role, String expectedFragment) {
            var a = algo("BAJO", "emociones", false, false, 0.10, List.of());
            String narrative = pipeline.buildFamiliarNarrative(a, role);
            assertThat(narrative).contains(expectedFragment.trim());
        }

        @Test
        void nullRoleFallsBackToOtro() {
            var a = algo("BAJO", "emociones", false, false, 0.10, List.of());
            String narrative = pipeline.buildFamiliarNarrative(a, null);
            assertThat(narrative).contains("participación familiar activa");
        }

        @Test
        void unknownRoleFallsBackToOtro() {
            var a = algo("BAJO", "emociones", false, false, 0.10, List.of());
            String narrative = pipeline.buildFamiliarNarrative(a, "ABUELO");
            assertThat(narrative).contains("participación familiar activa");
        }
    }

    @Nested
    @DisplayName("Sección de recaída")
    class RelapseSection {
        @Test
        void includesRelapseWarningWhenDetected() {
            var a = algo("ALTO", "emociones", true, false, 0.10, List.of("dim=emociones"));
            String narrative = pipeline.buildFamiliarNarrative(a, "PADRE");
            assertThat(narrative).contains("Alerta de recaída");
        }

        @Test
        void noRelapseWarningWhenNotDetected() {
            var a = algo("MODERADO", "emociones", false, false, 0.10, List.of());
            String narrative = pipeline.buildFamiliarNarrative(a, "PADRE");
            assertThat(narrative).doesNotContain("Alerta de recaída");
        }
    }

    @Nested
    @DisplayName("Sección de incertidumbre")
    class UncertaintySection {
        @Test
        void includesUncertaintyWarningWhenHigh() {
            var a = algo("MODERADO", "emociones", false, false, 0.75, List.of());
            String narrative = pipeline.buildFamiliarNarrative(a, "PADRE");
            assertThat(narrative).contains("Incertidumbre elevada");
        }

        @Test
        void noUncertaintyWarningWhenLow() {
            var a = algo("BAJO", "emociones", false, false, 0.10, List.of());
            String narrative = pipeline.buildFamiliarNarrative(a, "PADRE");
            assertThat(narrative).doesNotContain("Incertidumbre elevada");
        }
    }

    @Nested
    @DisplayName("Recomendación por dimensión")
    class RecommendationByDimension {
        @ParameterizedTest(name = "critDim={0}")
        @CsvSource({
                "emociones,    escucha activa",
                "comunicacion, diálogo sin interrupciones",
                "habitos,      Revisar y simplificar rutinas",
                "tiempos,      tiempo de calidad"
        })
        void containsDimensionSpecificRecommendation(String critDim, String expectedFragment) {
            var a = algo("MODERADO", critDim, false, false, 0.10, List.of());
            String narrative = pipeline.buildFamiliarNarrative(a, "PADRE");
            assertThat(narrative).contains(expectedFragment.trim());
        }

        @Test
        void unknownDimensionUsesDefaultRecommendation() {
            var a = algo("MODERADO", "unknown_dim", false, false, 0.10, List.of());
            String narrative = pipeline.buildFamiliarNarrative(a, "PADRE");
            assertThat(narrative).contains("dimensión más vulnerable");
        }
    }

    @Nested
    @DisplayName("buildTechnicalSummary")
    class TechnicalSummary {
        @Test
        void containsRequiredFields() {
            var a = algo("ALTO", "comunicacion", false, true, 0.20, List.of());
            String summary = pipeline.buildTechnicalSummary(a);

            assertThat(summary)
                    .contains("ICF=")
                    .contains("[ALTO]")
                    .contains("critDim=comunicacion")
                    .contains("sim=true")
                    .contains("relapse=false")
                    .contains("algo=RISK_ALGO_V1");
        }

        @Test
        void isDeterministic() {
            var a = algo("BAJO", "habitos", false, false, 0.10, List.of());
            assertThat(pipeline.buildTechnicalSummary(a))
                    .isEqualTo(pipeline.buildTechnicalSummary(a));
        }
    }
}
