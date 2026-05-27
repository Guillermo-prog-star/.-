package com.integrityfamily.analytics.service;

import com.integrityfamily.ai.service.AiService;
import com.integrityfamily.domain.Feedback;
import com.integrityfamily.domain.repository.FeedbackRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SentimentAnalyticsService")
class SentimentAnalyticsServiceTest {

    @Mock FeedbackRepository feedbackRepository;
    @Mock AiService          aiService;

    @InjectMocks SentimentAnalyticsService service;

    // ── Early-return sin feedback ─────────────────────────────────────────────

    @Test
    @DisplayName("retorna reporte con 0 comentarios y sin llamada a IA cuando no hay feedback")
    void analyzeGlobalFeedback_noFeedback_earlyReturn_noAiCall() {
        when(feedbackRepository.findAll()).thenReturn(List.of());

        SentimentAnalyticsService.SentimentReport report = service.analyzeGlobalFeedback();

        assertThat(report.getTotalComments()).isEqualTo(0);
        assertThat(report.getAiExecutiveSummary()).isNotBlank();
        verify(aiService, never()).processAnalyticInference(any(), any());
    }

    // ── calculateDistribution ─────────────────────────────────────────────────

    @Test
    @DisplayName("calcula distribucion de sentimientos correctamente (2 pos, 1 neu, 2 neg de 5)")
    void analyzeGlobalFeedback_withFeedback_calculatesDistribution() {
        Feedback f1 = fb(5);  // positivo (>=4)
        Feedback f2 = fb(4);  // positivo
        Feedback f3 = fb(3);  // neutro
        Feedback f4 = fb(2);  // negativo (<=2)
        Feedback f5 = fb(1);  // negativo
        when(feedbackRepository.findAll()).thenReturn(List.of(f1, f2, f3, f4, f5));

        // Respuesta IA mínima con bloque JSON parcial para extractDimensions
        String aiResponse = "Análisis ejecutivo. {\"scores\":{\"Comunicación\":7.0}}";
        when(aiService.processAnalyticInference(any(), isNull())).thenReturn(aiResponse);

        SentimentAnalyticsService.SentimentReport report = service.analyzeGlobalFeedback();

        assertThat(report.getTotalComments()).isEqualTo(5);
        assertThat(report.getSentimentDistribution()).isNotNull();
        // 2/5 * 100 = 40.0 positivo
        assertThat(report.getSentimentDistribution().get("Positivo")).isEqualTo(40.0);
        // 1/5 * 100 = 20.0 neutro
        assertThat(report.getSentimentDistribution().get("Neutro")).isEqualTo(20.0);
        // 2/5 * 100 = 40.0 negativo
        assertThat(report.getSentimentDistribution().get("Negativo")).isEqualTo(40.0);
    }

    @Test
    @DisplayName("todos los feedbacks positivos — 100% positivo, 0% neutro y negativo")
    void analyzeGlobalFeedback_allPositive_100PercentPositive() {
        when(feedbackRepository.findAll()).thenReturn(List.of(fb(5), fb(4), fb(5)));
        String aiResponse = "Excelente. {\"scores\":{\"Comunicación\":9.0}}";
        when(aiService.processAnalyticInference(any(), isNull())).thenReturn(aiResponse);

        SentimentAnalyticsService.SentimentReport report = service.analyzeGlobalFeedback();

        assertThat(report.getSentimentDistribution().get("Positivo")).isEqualTo(100.0);
        assertThat(report.getSentimentDistribution().get("Neutro")).isEqualTo(0.0);
        assertThat(report.getSentimentDistribution().get("Negativo")).isEqualTo(0.0);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Feedback fb(int score) {
        return Feedback.builder()
                .score(score)
                .comment("comentario score " + score)
                .type("GENERAL")
                .build();
    }
}
