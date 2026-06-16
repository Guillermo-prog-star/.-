package com.integrityfamily.risk.service;

import com.integrityfamily.domain.Question;
import com.integrityfamily.domain.repository.QuestionRepository;
import com.integrityfamily.dto.EvaluationDtos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para RiskAlgoV1Engine.
 *
 * Estrategia: mockeamos QuestionRepository.findAllById() para controlar exactamente
 * las propiedades de cada Question (dimensión, tipo, peso, reverseQuestion, etc.).
 * Todos los cálculos son deterministas — los resultados se pueden verificar con aritmética.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RiskAlgoV1Engine (RISK_ALGO_V1)")
class RiskAlgoV1EngineTest {

    @Mock QuestionRepository questionRepository;

    @InjectMocks RiskAlgoV1Engine engine;

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Respuesta mínima: questionId + value en escala 1-5 */
    private static EvaluationDtos.AnswerDto ans(long qId, int value) {
        return new EvaluationDtos.AnswerDto(qId, value, null);
    }

    /** Question builder factory */
    private static Question q(long id, String dim, String type, boolean reverse,
                               double severity, String milestoneCode, boolean detectsRelapse) {
        return Question.builder()
                .id(id)
                .questionKey("Q-TEST-" + id)
                .dimension(dim)
                .type(type)          // "CORE", "ADAPTIVE", "MIRROR", etc.
                .reverseQuestion(reverse)
                .direction(reverse ? "NEGATIVE" : "POSITIVE")
                .severityWeight(severity)
                .milestoneCode(milestoneCode)
                .detectsRelapse(detectsRelapse)
                .active(true)
                .weight(1)
                .build();
    }

    /** Question CORE normal (más común) */
    private static Question coreQ(long id, String dim) {
        return q(id, dim, "CORE", false, 1.0, "W1", false);
    }

    /** Question CORE con milestoneCode que NO coincide con el hito activo */
    private static Question coreQOtherMilestone(long id, String dim) {
        return q(id, dim, "CORE", false, 1.0, "M6", false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sin respuestas
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("sin respuestas")
    class NoAnswers {

        @Test
        @DisplayName("answers null → ICF neutro 50, MODERADO, todas las dims en 50")
        void nullAnswers_returnsNeutralResult() {
            RiskAlgoV1Engine.AlgoResult r = engine.compute(null, "W1");

            assertThat(r.healthyIndex()).isEqualTo(50.0);
            assertThat(r.riskLevel()).isEqualTo("MODERADO");
            assertThat(r.consciousnessLabel()).isEqualTo("Reactiva");
            assertThat(r.dimensionScores()).containsEntry("emociones", 50.0)
                                          .containsEntry("comunicacion", 50.0)
                                          .containsEntry("habitos", 50.0)
                                          .containsEntry("tiempos", 50.0);
        }

        @Test
        @DisplayName("answers lista vacía → mismo resultado neutro que null")
        void emptyAnswers_returnsNeutralResult() {
            // compute() hace early-return cuando answers.isEmpty() → findAllById nunca se llama
            RiskAlgoV1Engine.AlgoResult r = engine.compute(List.of(), "W1");

            assertThat(r.healthyIndex()).isEqualTo(50.0);
            assertThat(r.riskLevel()).isEqualTo("MODERADO");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Normalización de score
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("normalización de score 1-5 → 0-100")
    class ScoreNormalization {

        @Test
        @DisplayName("question normal, value=5 → normScore=100.0")
        void normalQuestion_value5_normScore100() {
            Question q = coreQ(1L, "emociones");
            when(questionRepository.findAllById(List.of(1L))).thenReturn(List.of(q));

            RiskAlgoV1Engine.AlgoResult r = engine.compute(List.of(ans(1L, 5)), "M6");

            // emociones=100; otras dims sin datos = 100 (sin riesgo declarado)
            assertThat(r.dimensionScores().get("emociones")).isEqualTo(100.0);
        }

        @Test
        @DisplayName("question normal, value=1 → normScore=0.0")
        void normalQuestion_value1_normScore0() {
            Question q = coreQ(1L, "emociones");
            when(questionRepository.findAllById(List.of(1L))).thenReturn(List.of(q));

            RiskAlgoV1Engine.AlgoResult r = engine.compute(List.of(ans(1L, 1)), "M6");

            assertThat(r.dimensionScores().get("emociones")).isEqualTo(0.0);
        }

        @Test
        @DisplayName("question normal, value=3 → normScore=50.0  (escala lineal (3-1)/4*100)")
        void normalQuestion_value3_normScore50() {
            Question q = coreQ(1L, "comunicacion");
            when(questionRepository.findAllById(List.of(1L))).thenReturn(List.of(q));

            RiskAlgoV1Engine.AlgoResult r = engine.compute(List.of(ans(1L, 3)), "M6");

            assertThat(r.dimensionScores().get("comunicacion")).isEqualTo(50.0);
        }

        @Test
        @DisplayName("reverseQuestion=true, value=5 → normScore=0.0 (escala invertida)")
        void reverseQuestion_value5_normScore0() {
            Question q = q(2L, "emociones", "CORE", true, 1.0, "M3", false);
            when(questionRepository.findAllById(List.of(2L))).thenReturn(List.of(q));

            RiskAlgoV1Engine.AlgoResult r = engine.compute(List.of(ans(2L, 5)), "M6");

            assertThat(r.dimensionScores().get("emociones")).isEqualTo(0.0);
        }

        @Test
        @DisplayName("reverseQuestion=true, value=1 → normScore=100.0")
        void reverseQuestion_value1_normScore100() {
            Question q = q(3L, "habitos", "CORE", true, 1.0, "M3", false);
            when(questionRepository.findAllById(List.of(3L))).thenReturn(List.of(q));

            RiskAlgoV1Engine.AlgoResult r = engine.compute(List.of(ans(3L, 1)), "M6");

            assertThat(r.dimensionScores().get("habitos")).isEqualTo(100.0);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ICF ponderado
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("fórmula ICF: emo×0.30 + com×0.30 + hab×0.20 + tie×0.20")
    class IcfFormula {

        @Test
        @DisplayName("scores: emo=100, com=50, hab=0, tie=50 → ICF=55.0")
        void icf_weightedAverage_correctFormula() {
            // emo=100 (value=5), com=50 (value=3), hab=0 (value=1), tie=50 (value=3)
            Question qEmo = coreQOtherMilestone(1L, "emociones");
            Question qCom = coreQOtherMilestone(2L, "comunicacion");
            Question qHab = coreQOtherMilestone(3L, "habitos");
            Question qTie = coreQOtherMilestone(4L, "tiempos");
            when(questionRepository.findAllById(anyList())).thenReturn(List.of(qEmo, qCom, qHab, qTie));

            List<EvaluationDtos.AnswerDto> answers = List.of(
                    ans(1L, 5), // emo: (5-1)/4*100 = 100
                    ans(2L, 3), // com: (3-1)/4*100 = 50
                    ans(3L, 1), // hab: (1-1)/4*100 = 0
                    ans(4L, 3)  // tie: (3-1)/4*100 = 50
            );

            RiskAlgoV1Engine.AlgoResult r = engine.compute(answers, "M6");

            // ICF = 100*0.30 + 50*0.30 + 0*0.20 + 50*0.20 = 30 + 15 + 0 + 10 = 55.0
            assertThat(r.healthyIndex()).isEqualTo(55.0);
        }

        @Test
        @DisplayName("pregunta de dimensión desconocida → se mapea a 'emociones'")
        void unknownDimension_mapsToEmociones() {
            Question q = q(1L, "desconocida", "CORE", false, 1.0, "M3", false);
            when(questionRepository.findAllById(anyList())).thenReturn(List.of(q));

            RiskAlgoV1Engine.AlgoResult r = engine.compute(List.of(ans(1L, 5)), "M6");

            // "desconocida" → "emociones" → score 100
            assertThat(r.dimensionScores().get("emociones")).isEqualTo(100.0);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Clasificación de riesgo por fase
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("clasificación de riesgo adaptativa por fase")
    class RiskClassification {

        @Test
        @DisplayName("fase inconsciente (W1): ICF=65 → BAJO (umbral ≥65)")
        void w1Phase_icf65_isBajo() {
            // emo/com value=4 → 75; hab/tie value=3 → 50
            // ICF = 75*0.30 + 75*0.30 + 50*0.20 + 50*0.20 = 65.0 >= 65 → BAJO en W1
            Question qEmo = q(1L, "emociones",    "CORE", false, 1.0, "M6", false);
            Question qCom = q(2L, "comunicacion", "CORE", false, 1.0, "M6", false);
            Question qHab = q(3L, "habitos",      "CORE", false, 1.0, "M6", false);
            Question qTie = q(4L, "tiempos",      "CORE", false, 1.0, "M6", false);
            when(questionRepository.findAllById(anyList())).thenReturn(List.of(qEmo, qCom, qHab, qTie));

            RiskAlgoV1Engine.AlgoResult r = engine.compute(
                    List.of(ans(1L, 4), ans(2L, 4), ans(3L, 3), ans(4L, 3)), "W1");

            assertThat(r.healthyIndex()).isEqualTo(65.0);
            assertThat(r.riskLevel()).isEqualTo("BAJO");
        }

        @Test
        @DisplayName("fase inconsciente (W1): all dims=100 → ICF=100 → BAJO")
        void w1Phase_allPerfect_isBajo() {
            Question qEmo = coreQOtherMilestone(1L, "emociones");
            Question qCom = coreQOtherMilestone(2L, "comunicacion");
            Question qHab = coreQOtherMilestone(3L, "habitos");
            Question qTie = coreQOtherMilestone(4L, "tiempos");
            when(questionRepository.findAllById(anyList())).thenReturn(List.of(qEmo, qCom, qHab, qTie));

            RiskAlgoV1Engine.AlgoResult r = engine.compute(
                    List.of(ans(1L, 5), ans(2L, 5), ans(3L, 5), ans(4L, 5)), "W1");

            assertThat(r.riskLevel()).isEqualTo("BAJO");
        }

        @Test
        @DisplayName("fase plena (M36): ICF=64 (all dims=64) → MODERADO (umbral ≥65)")
        void m36Phase_icf64_isModerado() {
            // value=3.56 → nearest integer value
            // (3-1)/4*100 = 50 → MODERADO en fase inconsciente pero no en plena
            // Para ICF~64: usar preguntas con valor 4 → normScore=(4-1)/4*100=75.0
            // ICF=75 ≥ 65 pleno umbral BAJO=85 → 75 < 85 → 75 ≥ 65 MODERADO
            Question qEmo = coreQOtherMilestone(1L, "emociones");
            Question qCom = coreQOtherMilestone(2L, "comunicacion");
            Question qHab = coreQOtherMilestone(3L, "habitos");
            Question qTie = coreQOtherMilestone(4L, "tiempos");
            when(questionRepository.findAllById(anyList())).thenReturn(List.of(qEmo, qCom, qHab, qTie));

            // value=4 → normScore=75: ICF=75; fase M36 (pleno): BAJO≥85, MODERADO≥65 → 75≥65 → MODERADO
            RiskAlgoV1Engine.AlgoResult r = engine.compute(
                    List.of(ans(1L, 4), ans(2L, 4), ans(3L, 4), ans(4L, 4)), "M36");

            assertThat(r.healthyIndex()).isEqualTo(75.0);
            assertThat(r.riskLevel()).isEqualTo("MODERADO");
        }

        // ── Reglas de seguridad crítica ────────────────────────────────────

        @Test
        @DisplayName("cualquier dimensión < 25 → CRITICO (sin importar ICF global)")
        void criticalSafetyRule_anyDimBelow25_returnsCritico() {
            // emociones=0 (value=1), otras=100 → ICF ponderado = 0*0.30 + 100*0.70 = 70 (BAJO en W1)
            // Pero emociones=0 < 25 → CRITICO por regla de seguridad
            Question qEmo = coreQOtherMilestone(1L, "emociones");
            Question qCom = coreQOtherMilestone(2L, "comunicacion");
            Question qHab = coreQOtherMilestone(3L, "habitos");
            Question qTie = coreQOtherMilestone(4L, "tiempos");
            when(questionRepository.findAllById(anyList())).thenReturn(List.of(qEmo, qCom, qHab, qTie));

            RiskAlgoV1Engine.AlgoResult r = engine.compute(
                    List.of(ans(1L, 1), ans(2L, 5), ans(3L, 5), ans(4L, 5)), "W1");

            assertThat(r.dimensionScores().get("emociones")).isEqualTo(0.0);
            assertThat(r.riskLevel()).isEqualTo("CRITICO");
            assertThat(r.hasCrisis()).isTrue();
        }

        @Test
        @DisplayName("dimensión entre 25 y 40 + base BAJO → sube a ALTO")
        void criticalSafetyRule_anyDimUnder40_escalatesFromBajoToAlto() {
            // emociones=25 (< 40 pero ≥ 25), otras=100 → ICF = 25*0.30 + 100*0.70 = 77.5
            // W1: 77.5 ≥ 65 → base BAJO; pero emociones=25 < 40 → ALTO
            // Para emociones=25: normScore=25 → value que da 25: (v-1)/4*100=25 → v=2
            Question qEmo = coreQOtherMilestone(1L, "emociones");
            Question qCom = coreQOtherMilestone(2L, "comunicacion");
            Question qHab = coreQOtherMilestone(3L, "habitos");
            Question qTie = coreQOtherMilestone(4L, "tiempos");
            when(questionRepository.findAllById(anyList())).thenReturn(List.of(qEmo, qCom, qHab, qTie));

            RiskAlgoV1Engine.AlgoResult r = engine.compute(
                    List.of(ans(1L, 2), ans(2L, 5), ans(3L, 5), ans(4L, 5)), "W1");

            assertThat(r.dimensionScores().get("emociones")).isEqualTo(25.0);
            assertThat(r.riskLevel()).isEqualTo("ALTO");
            assertThat(r.hasCrisis()).isTrue();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MIRROR — detección de simulación
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("preguntas MIRROR y detección de simulación")
    class MirrorDetection {

        @Test
        @DisplayName("pregunta MIRROR no contribuye al ICF (dimensión queda en 100 sin datos)")
        void mirrorQuestion_notCountedInIcf() {
            Question qMirror = q(1L, "emociones", "MIRROR", false, 1.0, "W1", false);
            when(questionRepository.findAllById(anyList())).thenReturn(List.of(qMirror));

            RiskAlgoV1Engine.AlgoResult r = engine.compute(List.of(ans(1L, 3)), "M6");

            // emociones no tiene datos reales → cae a 100 (sin riesgo declarado)
            assertThat(r.dimensionScores().get("emociones")).isEqualTo(100.0);
        }

        @Test
        @DisplayName("3 MIRROR, 2 con valor 5 (67%) → simulationSuspected=true")
        void mirrorSimulation_above60Percent_suspected() {
            Question qA = q(1L, "emociones", "MIRROR", false, 1.0, "W1", false);
            Question qB = q(2L, "emociones", "MIRROR", false, 1.0, "W1", false);
            Question qC = q(3L, "emociones", "MIRROR", false, 1.0, "W1", false);
            when(questionRepository.findAllById(anyList())).thenReturn(List.of(qA, qB, qC));

            // 2/3 = 66.7% > 60% → simulationSuspected
            RiskAlgoV1Engine.AlgoResult r = engine.compute(
                    List.of(ans(1L, 5), ans(2L, 5), ans(3L, 3)), "W1");

            assertThat(r.simulationSuspected()).isTrue();
            assertThat(r.mirrorFlags()).hasSize(2);
        }

        @Test
        @DisplayName("1 MIRROR con valor 5 de 2 (50%) → simulationSuspected=false (no supera 60%)")
        void mirrorSimulation_below60Percent_notSuspected() {
            Question qA = q(1L, "emociones", "MIRROR", false, 1.0, "W1", false);
            Question qB = q(2L, "emociones", "MIRROR", false, 1.0, "W1", false);
            when(questionRepository.findAllById(anyList())).thenReturn(List.of(qA, qB));

            // 1/2 = 50% ≤ 60% → no sospecha
            RiskAlgoV1Engine.AlgoResult r = engine.compute(
                    List.of(ans(1L, 5), ans(2L, 3)), "W1");

            assertThat(r.simulationSuspected()).isFalse();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Detección de recaída
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("detección de recaída (detectsRelapse)")
    class RelapseDetection {

        @Test
        @DisplayName("detectsRelapse=true y value=2 → relapseDetected=true")
        void relapseSentinel_value2_detected() {
            Question q = q(1L, "emociones", "CORE", false, 1.0, "W1", true);
            when(questionRepository.findAllById(anyList())).thenReturn(List.of(q));

            RiskAlgoV1Engine.AlgoResult r = engine.compute(List.of(ans(1L, 2)), "W1");

            assertThat(r.relapseDetected()).isTrue();
            assertThat(r.relapseFlags()).hasSize(1);
            assertThat(r.relapseFlags().get(0)).contains("Q-TEST-1");
        }

        @Test
        @DisplayName("detectsRelapse=true y value=3 → relapseDetected=false (umbral es ≤ 2)")
        void relapseSentinel_value3_notDetected() {
            Question q = q(1L, "emociones", "CORE", false, 1.0, "W1", true);
            when(questionRepository.findAllById(anyList())).thenReturn(List.of(q));

            RiskAlgoV1Engine.AlgoResult r = engine.compute(List.of(ans(1L, 3)), "W1");

            assertThat(r.relapseDetected()).isFalse();
        }

        @Test
        @DisplayName("detectsRelapse=false y value=1 → relapseDetected=false (campo no activo)")
        void noRelapseSentinel_lowValue_notDetected() {
            Question q = q(1L, "emociones", "CORE", false, 1.0, "W1", false);
            when(questionRepository.findAllById(anyList())).thenReturn(List.of(q));

            RiskAlgoV1Engine.AlgoResult r = engine.compute(List.of(ans(1L, 1)), "W1");

            assertThat(r.relapseDetected()).isFalse();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Boost de hito
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("milestone boost 1.5×")
    class MilestoneBoost {

        @Test
        @DisplayName("pregunta del hito actual recibe 1.5× de peso — desplaza promedio hacia ella")
        void milestoneBoost_shiftsWeightedAverageTowardBoostedQuestion() {
            // Q1: milestoneCode W1 (coincide con hito activo W1) → boost 1.5×, answer=5 → normScore=100
            Question qBoosted   = q(1L, "emociones", "CORE", false, 1.0, "W1", false);
            // Q2: milestoneCode M6 (no coincide) → boost 1.0×, answer=1 → normScore=0
            Question qUnboosted = q(2L, "emociones", "CORE", false, 1.0, "M6", false);
            when(questionRepository.findAllById(anyList())).thenReturn(List.of(qBoosted, qUnboosted));

            RiskAlgoV1Engine.AlgoResult r = engine.compute(
                    List.of(ans(1L, 5), ans(2L, 1)), "W1");

            // weightedSum = 100*1.5 + 0*1.0 = 150; weightTotal = 1.5 + 1.0 = 2.5
            // score = 150/2.5 = 60.0
            assertThat(r.dimensionScores().get("emociones")).isEqualTo(60.0);
        }

        @Test
        @DisplayName("sin boost (diferente hito): promedio simple 50-50")
        void noMilestoneBoost_equalWeights() {
            Question qA = q(1L, "emociones", "CORE", false, 1.0, "M3", false);
            Question qB = q(2L, "emociones", "CORE", false, 1.0, "M6", false);
            when(questionRepository.findAllById(anyList())).thenReturn(List.of(qA, qB));

            // Ambas con hito diferente a "W1" → sin boost; normScores: 100 y 0 → avg=50
            RiskAlgoV1Engine.AlgoResult r = engine.compute(
                    List.of(ans(1L, 5), ans(2L, 1)), "W1");

            assertThat(r.dimensionScores().get("emociones")).isEqualTo(50.0);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Nivel de consciencia
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("nivel de consciencia")
    class ConsciousnessLevel {

        @Test
        @DisplayName("ICF=100 → nivel 1 'Plena'")
        void icf100_isPlena() {
            Question q = coreQOtherMilestone(1L, "emociones");
            when(questionRepository.findAllById(anyList())).thenReturn(List.of(q));

            RiskAlgoV1Engine.AlgoResult r = engine.compute(List.of(ans(1L, 5)), "M6");

            assertThat(r.consciousnessLevel()).isEqualTo(1);
            assertThat(r.consciousnessLabel()).isEqualTo("Plena");
        }

        @Test
        @DisplayName("ICF=65 → nivel 3 'Consciente'  (rango: 55 ≤ ICF < 70)")
        void icf65_isConsciente() {
            // emo/com value=4 → 75; hab/tie value=3 → 50
            // ICF = 75*0.30 + 75*0.30 + 50*0.20 + 50*0.20 = 65.0  → nivel 3
            Question qEmo = coreQOtherMilestone(1L, "emociones");
            Question qCom = coreQOtherMilestone(2L, "comunicacion");
            Question qHab = coreQOtherMilestone(3L, "habitos");
            Question qTie = coreQOtherMilestone(4L, "tiempos");
            when(questionRepository.findAllById(anyList())).thenReturn(List.of(qEmo, qCom, qHab, qTie));

            RiskAlgoV1Engine.AlgoResult r = engine.compute(
                    List.of(ans(1L, 4), ans(2L, 4), ans(3L, 3), ans(4L, 3)), "W1");

            assertThat(r.healthyIndex()).isEqualTo(65.0);
            assertThat(r.consciousnessLevel()).isEqualTo(3);
            assertThat(r.consciousnessLabel()).isEqualTo("Consciente");
        }

        @Test
        @DisplayName("ICF=0 (todas dims=0) → nivel 5 'Inconsciente'")
        void icf0_isInconsciente() {
            Question qEmo = coreQOtherMilestone(1L, "emociones");
            Question qCom = coreQOtherMilestone(2L, "comunicacion");
            Question qHab = coreQOtherMilestone(3L, "habitos");
            Question qTie = coreQOtherMilestone(4L, "tiempos");
            when(questionRepository.findAllById(anyList())).thenReturn(List.of(qEmo, qCom, qHab, qTie));

            RiskAlgoV1Engine.AlgoResult r = engine.compute(
                    List.of(ans(1L, 1), ans(2L, 1), ans(3L, 1), ans(4L, 1)), "M6");

            assertThat(r.healthyIndex()).isEqualTo(0.0);
            assertThat(r.consciousnessLevel()).isEqualTo(5);
            assertThat(r.consciousnessLabel()).isEqualTo("Inconsciente");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Misión sugerida
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generador de misión")
    class MissionGenerator {

        @Test
        @DisplayName("dimensión crítica 'emociones' → misión ESTABILIZACION_EMOCIONAL")
        void criticalDimEmociones_missionEstabilizacion() {
            // emociones=0 (valor=1), otras=100 → critica=emociones
            Question qEmo = coreQOtherMilestone(1L, "emociones");
            Question qCom = coreQOtherMilestone(2L, "comunicacion");
            when(questionRepository.findAllById(anyList())).thenReturn(List.of(qEmo, qCom));

            RiskAlgoV1Engine.AlgoResult r = engine.compute(
                    List.of(ans(1L, 1), ans(2L, 5)), "M6");

            assertThat(r.criticalDimension()).isEqualTo("emociones");
            assertThat(r.suggestedMissionGenerator()).isEqualTo("ESTABILIZACION_EMOCIONAL");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AlgoResult helpers
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("AlgoResult.hasCrisis() y summary()")
    class AlgoResultHelpers {

        @Test
        @DisplayName("hasCrisis() true cuando riskLevel es CRITICO o ALTO")
        void hasCrisis_trueForCriticoAndAlto() {
            Question qEmo = coreQOtherMilestone(1L, "emociones");
            when(questionRepository.findAllById(anyList())).thenReturn(List.of(qEmo));

            // emociones=0 → CRITICO por regla de seguridad
            RiskAlgoV1Engine.AlgoResult r = engine.compute(List.of(ans(1L, 1)), "W1");

            // emociones=0 < 25 → CRITICO → hasCrisis=true
            assertThat(r.hasCrisis()).isTrue();
        }

        @Test
        @DisplayName("summary() incluye ICF, riskLevel y criticalDimension")
        void summary_includesKeyFields() {
            // early-return → findAllById no se llama, no se necesita stub
            RiskAlgoV1Engine.AlgoResult r = engine.compute(List.of(), "W1");

            String s = r.summary();
            assertThat(s).contains("ICF=").contains("MODERADO").contains("emociones");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UncertaintyVector
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("UncertaintyVector")
    class UncertaintyVectorTests {

        @Test
        @DisplayName("sin respuestas → U_o=1.0, level()='HIGH', total=0.40 no supera umbral isHigh (>0.40)")
        void emptyAnswers_observationalUncertainty() {
            // compute(List.of()) hace early-return → findAllById nunca se llama
            RiskAlgoV1Engine.AlgoResult r = engine.compute(List.of(), "W1");

            // buildEmptyResult: UncertaintyVector(0.05, 0.25, 0.10, 1.00, 0.05, 0.40)
            assertThat(r.uncertainty().observational()).isEqualTo(1.0);
            // total=0.40 → level usa < 0.35 → else → "HIGH"
            assertThat(r.uncertainty().level()).isEqualTo("HIGH");
            // total=0.40 es == 0.40, no > 0.40 → isHigh() es false (umbral estricto)
            assertThat(r.uncertainty().isHigh()).isFalse();
            // total=0.40 no > 0.50 → reducesRisk() es false
            assertThat(r.uncertainty().reducesRisk()).isFalse();
        }

        @Test
        @DisplayName("UncertaintyVector.level() 'LOW' cuando total < 0.15")
        void lowLevel_whenTotalBelow15() {
            RiskAlgoV1Engine.UncertaintyVector u =
                    new RiskAlgoV1Engine.UncertaintyVector(0.05, 0.05, 0.05, 0.05, 0.05, 0.10);

            assertThat(u.level()).isEqualTo("LOW");
            assertThat(u.isHigh()).isFalse();
            assertThat(u.reducesRisk()).isFalse();
        }

        @Test
        @DisplayName("UncertaintyVector.reducesRisk() true cuando total > 0.50")
        void reducesRisk_whenTotalAbove50() {
            RiskAlgoV1Engine.UncertaintyVector u =
                    new RiskAlgoV1Engine.UncertaintyVector(0.80, 0.30, 0.10, 1.00, 0.05, 0.60);

            assertThat(u.reducesRisk()).isTrue();
            assertThat(u.isHigh()).isTrue();
            assertThat(u.level()).isEqualTo("HIGH");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers internos
    // ─────────────────────────────────────────────────────────────────────────

    /** Helpers para tests que configuran las 4 dims al mismo valor */
    private void setAllDimsTo(double targetScore, String milestone) {
        // no-op; los tests individuales configuran sus propios stubs
    }

    private List<EvaluationDtos.AnswerDto> answersAllDimsAt(int value) {
        return List.of(ans(1L, value), ans(2L, value), ans(3L, value), ans(4L, value));
    }
}
