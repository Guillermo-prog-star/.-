package com.integrityfamily.assessment.service;

import com.integrityfamily.domain.RiskLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias para {@link EvaluationScoringService}.
 *
 * Documenta los umbrales de riesgo (Lógica de rangos de William):
 *   ≥ 70  → LOW
 *   40-69 → MEDIUM
 *    < 40 → HIGH
 *
 * No levanta contexto Spring — instancia directa.
 */
@DisplayName("EvaluationScoringService — Unit Tests")
class EvaluationScoringServiceTest {

    private final EvaluationScoringService service = new EvaluationScoringService();

    // ═══════════════════════════════════════════════════════════════════════
    //  calculateRisk — Nivel de riesgo por puntuación global
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("calculateRisk() — niveles de riesgo")
    class CalculateRisk {

        @ParameterizedTest(name = "score={0} → {1}")
        @CsvSource({
            "100, LOW",
            " 70, LOW",     // frontera inferior LOW
            " 69, MEDIUM",  // frontera superior MEDIUM
            " 55, MEDIUM",
            " 40, MEDIUM",  // frontera inferior MEDIUM
            " 39, HIGH",    // frontera superior HIGH
            " 20, HIGH",
            "  0, HIGH"
        })
        @DisplayName("clasificación correcta en todos los rangos y fronteras")
        void riskLevelMatchesThresholds(String score, RiskLevel expected) {
            var result = service.calculateRisk(new BigDecimal(score));
            assertThat(result.riskLevel()).isEqualTo(expected);
        }

        @Test
        @DisplayName("ScoringResult propaga la misma puntuación a todas las dimensiones")
        void globalScoreIsReplicatedToAllDimensions() {
            var score  = new BigDecimal("65");
            var result = service.calculateRisk(score);

            assertThat(result.emotions()).isEqualByComparingTo(score);
            assertThat(result.communication()).isEqualByComparingTo(score);
            assertThat(result.habits()).isEqualByComparingTo(score);
            assertThat(result.times()).isEqualByComparingTo(score);
            assertThat(result.global()).isEqualByComparingTo(score);
        }

        @Test
        @DisplayName("score exactamente en 70 → LOW (no MEDIUM)")
        void boundary70IsLow() {
            assertThat(service.calculateRisk(new BigDecimal("70")).riskLevel())
                .isEqualTo(RiskLevel.LOW);
        }

        @Test
        @DisplayName("score exactamente en 40 → MEDIUM (no HIGH)")
        void boundary40IsMedium() {
            assertThat(service.calculateRisk(new BigDecimal("40")).riskLevel())
                .isEqualTo(RiskLevel.MEDIUM);
        }
    }
}
