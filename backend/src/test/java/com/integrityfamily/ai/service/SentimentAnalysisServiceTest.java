package com.integrityfamily.ai.service;

import com.integrityfamily.ai.dto.DimensionCorrelation;
import com.integrityfamily.ai.dto.LogbookCorrelationResult;
import com.integrityfamily.ai.dto.SentimentResult;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.EvaluationDimensionScore;
import com.integrityfamily.domain.EvaluationStatus;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyLogbookEntry;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.repository.FamilyLogbookEntryRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SentimentAnalysisService")
class SentimentAnalysisServiceTest {

    @Mock FamilyRepository             familyRepository;
    @Mock FamilyLogbookEntryRepository logbookRepository;
    @Mock EvaluationRepository         evaluationRepository;
    @Mock AiProvider                   aiProvider;
    @Mock ContextSynthesizer           contextSynthesizer;

    @InjectMocks SentimentAnalysisService service;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Family family() {
        return Family.builder().id(1L).name("Familia Test").currentMilestone("W1").build();
    }

    private FamilyLogbookEntry entry(String situation, String difficulty,
                                     String emotion, String agreement) {
        return FamilyLogbookEntry.builder()
                .situation(situation)
                .difficultyDetected(difficulty)
                .emotionIdentified(emotion)
                .familyAgreement(agreement)
                .build();
    }

    private LogbookCorrelationResult runCorrelation(FamilyLogbookEntry... entries) {
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family()));
        when(logbookRepository.findByFamilyIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(entries));
        when(evaluationRepository.findFirstByFamilyIdAndStatusOrderByFinalizedAtDesc(anyLong(), any()))
                .thenReturn(Optional.empty());
        when(contextSynthesizer.synthesize(any(Family.class), anyString()))
                .thenThrow(new RuntimeException("mock-fallback"));
        return service.correlateFamilySentiment(1L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // analyze — VADER léxico español
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("analyze — VADER léxico")
    class Analyze {

        @Test
        @DisplayName("palabras positivas ('amor paz abrazo') → score>0.4, label POSITIVO")
        void positiveText_positiveLabel() {
            SentimentResult r = service.analyze("amor paz abrazo");

            assertThat(r.getScore()).isGreaterThan(0.4);
            assertThat(r.getLabel()).isEqualTo("POSITIVO");
        }

        @Test
        @DisplayName("palabras negativas intensas ('crisis pelea insulto') → score<-0.4, label CRISIS")
        void negativeText_crisisLabel() {
            SentimentResult r = service.analyze("crisis pelea insulto grosería");

            assertThat(r.getScore()).isLessThan(-0.4);
            assertThat(r.getLabel()).isEqualTo("CRISIS");
        }

        @Test
        @DisplayName("palabras sin léxico → score=0.0, label CONSCIENTE")
        void neutralText_conscienteLabel() {
            SentimentResult r = service.analyze("hola mundo esto es una prueba ordinaria");

            assertThat(r.getScore()).isEqualTo(0.0);
            assertThat(r.getLabel()).isEqualTo("CONSCIENTE");
        }

        @Test
        @DisplayName("texto null → score=0.0, label CONSCIENTE (no NPE)")
        void nullText_noException() {
            SentimentResult r = service.analyze(null);

            assertThat(r.getScore()).isEqualTo(0.0);
            assertThat(r.getLabel()).isEqualTo("CONSCIENTE");
        }

        @Test
        @DisplayName("texto vacío → score=0.0, label CONSCIENTE")
        void emptyText_consciente() {
            SentimentResult r = service.analyze("");

            assertThat(r.getScore()).isEqualTo(0.0);
            assertThat(r.getLabel()).isEqualTo("CONSCIENTE");
        }

        @Test
        @DisplayName("texto mixto ('amor crisis') → score negativo, label NEGATIVO")
        void mixedText_negativeLabel() {
            // amor(+0.8) + crisis(-1.0) = -0.2 → NEGATIVO
            SentimentResult r = service.analyze("amor crisis");

            assertThat(r.getScore()).isBetween(-1.0, 0.0);
            assertThat(r.getLabel()).isEqualTo("NEGATIVO");
        }

        @Test
        @DisplayName("palabra con puntuación ('perdón,') → se limpia y reconoce en léxico")
        void wordWithPunctuation_recognized() {
            SentimentResult r = service.analyze("perdón, gracias familia");

            assertThat(r.getScore()).isGreaterThan(0.0);
        }

        @Test
        @DisplayName("score boundary -0.39 → label NEGATIVO (no CRISIS)")
        void boundaryNegativo_notCrisis() {
            // "triste" = -0.5 entre otros → pero probamos con "rabia" solo (-0.6)
            // Para un score exactamente en NEGATIVO usamos "distancia" (-0.4) solo
            // con 1 palabra → score = -0.4, que es < 0 pero NO < -0.4 → NEGATIVO
            SentimentResult r = service.analyze("distancia");

            assertThat(r.getScore()).isEqualTo(-0.4);
            assertThat(r.getLabel()).isEqualTo("NEGATIVO");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // correlateFamilySentiment — estructura y lógica
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("correlateFamilySentiment — estructura")
    class Correlate {

        @Test
        @DisplayName("familia no encontrada → IllegalArgumentException")
        void familyNotFound_throws() {
            when(familyRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.correlateFamilySentiment(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("sin entradas → totalEntriesAnalyzed=0, label=CONSCIENTE, 4 correlaciones")
        void noEntries_emptyResult() {
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family()));
            when(logbookRepository.findByFamilyIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
            when(evaluationRepository.findFirstByFamilyIdAndStatusOrderByFinalizedAtDesc(anyLong(), any()))
                    .thenReturn(Optional.empty());

            LogbookCorrelationResult r = service.correlateFamilySentiment(1L);

            assertThat(r.getTotalEntriesAnalyzed()).isEqualTo(0);
            assertThat(r.getGeneralLabel()).isEqualTo("CONSCIENTE");
            assertThat(r.getDimensionCorrelations()).hasSize(4);
        }

        @Test
        @DisplayName("sin entradas + evaluación con score → correlación usa score de eval")
        void noEntries_withEval_usesEvalScores() {
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family()));
            when(logbookRepository.findByFamilyIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
            EvaluationDimensionScore ds = EvaluationDimensionScore.builder()
                    .dimensionName("comunicacion").score(80.0).build();
            Evaluation eval = Evaluation.builder().id(1L).dimensionScores(List.of(ds)).build();
            when(evaluationRepository.findFirstByFamilyIdAndStatusOrderByFinalizedAtDesc(anyLong(), any()))
                    .thenReturn(Optional.of(eval));

            LogbookCorrelationResult r = service.correlateFamilySentiment(1L);

            DimensionCorrelation comm = r.getDimensionCorrelations().stream()
                    .filter(d -> "comunicacion".equals(d.getDimensionName()))
                    .findFirst().orElseThrow();
            assertThat(comm.getDiagnosticScore()).isEqualTo(80.0);
        }

        @Test
        @DisplayName("siempre retorna exactamente 4 correlaciones (una por dimensión)")
        void alwaysFourCorrelations_onePerDimension() {
            LogbookCorrelationResult r = runCorrelation(
                    entry("crisis familiar", null, null, null));

            assertThat(r.getDimensionCorrelations()).hasSize(4);
            assertThat(r.getDimensionCorrelations())
                    .extracting(DimensionCorrelation::getDimensionName)
                    .containsExactlyInAnyOrder("comunicacion", "emociones", "habitos", "tiempos");
        }

        @Test
        @DisplayName("entradas positivas → averageEmotionalScore > 0")
        void positiveEntries_positiveAverageScore() {
            LogbookCorrelationResult r = runCorrelation(
                    entry("amor paz juntos", "ninguna", "feliz", "acuerdo"));

            assertThat(r.getAverageEmotionalScore()).isGreaterThan(0.0);
            assertThat(r.getTotalEntriesAnalyzed()).isEqualTo(1);
        }

        @Test
        @DisplayName("familyId y familyName propagados correctamente al resultado")
        void familyMetadata_propagated() {
            // Sin entradas usa buildEmptyCorrelationResult → no llama a contextSynthesizer
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family()));
            when(logbookRepository.findByFamilyIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
            when(evaluationRepository.findFirstByFamilyIdAndStatusOrderByFinalizedAtDesc(anyLong(), any()))
                    .thenReturn(Optional.empty());

            LogbookCorrelationResult r = service.correlateFamilySentiment(1L);

            assertThat(r.getFamilyId()).isEqualTo(1L);
            assertThat(r.getFamilyName()).isEqualTo("Familia Test");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // priorityShift — diagScore>70 + sentiment<-0.25
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("priorityShift: diagScore>70 + logbookSentiment<-0.25 → requiresPriorityShift=true")
    void priorityShift_detected() {
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family()));
        // "crisis pelea" → ninguna keyword de habitos/emociones/tiempos → comunicacion
        // crisis(-1.0) + pelea(-0.8) → clamped -1.0 → muy negativo
        FamilyLogbookEntry e = entry("crisis pelea", null, null, null);
        when(logbookRepository.findByFamilyIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(e));
        EvaluationDimensionScore ds = EvaluationDimensionScore.builder()
                .dimensionName("comunicacion").score(85.0).build();
        Evaluation eval = Evaluation.builder().id(1L).dimensionScores(List.of(ds)).build();
        when(evaluationRepository.findFirstByFamilyIdAndStatusOrderByFinalizedAtDesc(anyLong(), any()))
                .thenReturn(Optional.of(eval));
        when(contextSynthesizer.synthesize(any(Family.class), anyString()))
                .thenThrow(new RuntimeException("mock-fallback"));

        LogbookCorrelationResult r = service.correlateFamilySentiment(1L);

        DimensionCorrelation comm = r.getDimensionCorrelations().stream()
                .filter(d -> "comunicacion".equals(d.getDimensionName())).findFirst().orElseThrow();
        assertThat(comm.getDiagnosticScore()).isEqualTo(85.0);
        assertThat(comm.getLogbookSentimentScore()).isLessThan(-0.25);
        assertThat(comm.isRequiresPriorityShift()).isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // classifyEntryDimension — palabras clave
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("classifyEntryDimension — palabras clave")
    class ClassifyEntryDimension {

        @Test
        @DisplayName("'celular' en situación → entra en categoría habitos")
        void keyword_celular_habitos() {
            LogbookCorrelationResult r = runCorrelation(
                    entry("celular todo el día", null, null, null));

            DimensionCorrelation habitos = r.getDimensionCorrelations().stream()
                    .filter(d -> "habitos".equals(d.getDimensionName())).findFirst().orElseThrow();
            // "celular" → -0.3 en el léxico
            assertThat(habitos.getLogbookSentimentScore()).isLessThan(0.0);
        }

        @Test
        @DisplayName("'rabia' en emoción → entra en categoría emociones")
        void keyword_rabia_emociones() {
            LogbookCorrelationResult r = runCorrelation(
                    entry("conflicto familiar", null, "rabia intensa", null));

            DimensionCorrelation emociones = r.getDimensionCorrelations().stream()
                    .filter(d -> "emociones".equals(d.getDimensionName())).findFirst().orElseThrow();
            // "rabia" = -0.6
            assertThat(emociones.getLogbookSentimentScore()).isLessThan(0.0);
        }

        @Test
        @DisplayName("'ausente' en situación → entra en categoría tiempos")
        void keyword_ausente_tiempos() {
            // "ausente" contiene "ausen" → tiempos
            LogbookCorrelationResult r = runCorrelation(
                    entry("padre ausente del hogar", null, null, null));

            DimensionCorrelation tiempos = r.getDimensionCorrelations().stream()
                    .filter(d -> "tiempos".equals(d.getDimensionName())).findFirst().orElseThrow();
            // "ausente" = -0.5 en el léxico
            assertThat(tiempos.getLogbookSentimentScore()).isLessThan(0.0);
        }

        @Test
        @DisplayName("sin palabras clave → categoría default comunicacion")
        void noKeyword_defaultComunicacion() {
            // Ninguna keyword de habitos/emociones/tiempos → comunicacion
            LogbookCorrelationResult r = runCorrelation(
                    entry("nota ordinaria del domingo", null, null, null));

            // Las otras 3 dims no tienen entradas → logbookSentimentScore = 0.0
            assertThat(r.getDimensionCorrelations().stream()
                    .filter(d -> !"comunicacion".equals(d.getDimensionName()))
                    .allMatch(d -> d.getLogbookSentimentScore() == 0.0)).isTrue();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // generateAdaptationRecommendation — texto de fallback
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generateAdaptationRecommendation — texto de fallback")
    class AdaptationRecommendation {

        @Test
        @DisplayName("sentimiento muy negativo (<-0.4) → recomendación contiene 'SENTINEL'")
        void extremelyNegative_sentinelRecommendation() {
            // crisis(-1) + pelea(-0.8) + grito(-0.7) + insulto(-0.9) = muy negativo
            LogbookCorrelationResult r = runCorrelation(
                    entry("crisis pelea grito insulto grosería", null, null, null));

            assertThat(r.getAdaptationRecommendation()).contains("SENTINEL");
        }

        @Test
        @DisplayName("sentimiento muy positivo (>0.4) → recomendación contiene 'EXCELENCIA'")
        void highPositive_excellenceRecommendation() {
            // amor(0.8) + paz(0.8) + perdón(0.9) + acuerdo(0.6) = 3.1 → clamped 1.0
            LogbookCorrelationResult r = runCorrelation(
                    entry("amor paz perdón acuerdo feliz felicidad", null, null, null));

            assertThat(r.getAdaptationRecommendation()).containsIgnoringCase("EXCELENCIA");
        }

        @Test
        @DisplayName("sentimiento neutro (0.0) → recomendación contiene 'EQUILIBRIO'")
        void neutral_balancedRecommendation() {
            // "nota fin de semana" → 0.0, no priority shifts → EQUILIBRIO
            LogbookCorrelationResult r = runCorrelation(
                    entry("nota del fin de semana", null, null, null));

            assertThat(r.getAdaptationRecommendation()).contains("EQUILIBRIO");
        }
    }
}
