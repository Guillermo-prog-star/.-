package com.integrityfamily.analytics.service;

import com.integrityfamily.ai.service.AiService;
import com.integrityfamily.domain.Feedback;
import com.integrityfamily.domain.repository.FeedbackRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SentimentAnalyticsService {

    private final FeedbackRepository feedbackRepository;
    private final AiService aiService;

    @Data
    @Builder
    public static class SentimentReport {
        private int totalComments;
        private Map<String, Double> sentimentDistribution;
        private Map<String, Double> dimensionScores;
        private String aiExecutiveSummary;
    }

    public SentimentReport analyzeGlobalFeedback() {
        log.info("🧠 [SENTIMENT-ANALYSIS] Iniciando procesamiento de feedback fase Proyectiva...");

        List<Feedback> allFeedback = feedbackRepository.findAll();
        if (allFeedback.isEmpty()) {
            return SentimentReport.builder()
                    .totalComments(0)
                    .aiExecutiveSummary("Aún no hay feedback suficiente para el análisis.")
                    .build();
        }

        String feedbackBlob = allFeedback.stream()
                .map(f -> String.format("[%s] Score %d: %s",
                        f.getType() != null ? f.getType() : "GENERIC",
                        f.getScore(),
                        f.getComment()))
                .collect(Collectors.joining("\n---\n"));

        // SDD: Prompt Engineering para la Ruta B (IA-Driven) con JSON Enforcement
        String prompt = String.format(
                """
                <identity>
                ACTÚA COMO AUDITOR DE INTEGRIDAD Y ESTRATEGA PEDAGÓGICO.
                </identity>
                <data_input>
                %s
                </data_input>
                <task_instruction>
                1. Genera un reporte crítico de análisis de tensión.
                2. Evalúa de 1.0 a 10.0 la salud de: Comunicación, Afecto, Regulación y Estabilidad.
                </task_instruction>
                <output_constraints>
                - Formato Markdown.
                - AL FINAL, incluye este bloque JSON:
                {
                  "scores": {
                    "Comunicación": 0.0, "Afecto": 0.0, "Regulación": 0.0, "Estabilidad": 0.0
                  }
                }
                </output_constraints>
                """,
                feedbackBlob);

        // 3. Invocación Sincronizada (Usando el nombre de método sanado para evitar ambigüedad)
        String fullAiResponse = aiService.processAnalyticInference(prompt, null);

        // 4. Extracción de Datos
        String cleanReport = fullAiResponse.split("\\{")[0].trim();
        Map<String, Double> dimensions = extractDimensions(fullAiResponse);

        return SentimentReport.builder()
                .totalComments(allFeedback.size())
                .sentimentDistribution(calculateDistribution(allFeedback))
                .dimensionScores(dimensions)
                .aiExecutiveSummary(cleanReport)
                .build();
    }

    private Map<String, Double> extractDimensions(String response) {
        Map<String, Double> scores = new HashMap<>();
        String[] keys = { "Comunicación", "Afecto", "Regulación", "Estabilidad" };
        try {
            for (String key : keys) {
                if (response.contains(key)) {
                    int start = response.indexOf(":", response.indexOf(key)) + 1;
                    String val = response.substring(start, Math.min(start + 5, response.length()))
                                         .replaceAll("[^0-9.]", "");
                    scores.put(key, Double.parseDouble(val));
                } else {
                    scores.put(key, 5.0); // Fallback neutro
                }
            }
        } catch (Exception e) {
            log.error("⚠️ [SENTIMENT] Error al extraer dimensiones: {}", e.getMessage());
            for (String key : keys) scores.putIfAbsent(key, 5.0);
        }
        return scores;
    }

    private Map<String, Double> calculateDistribution(List<Feedback> feedbackList) {
        int total = feedbackList.size();
        long pos = feedbackList.stream().filter(f -> f.getScore() >= 4).count();
        long neg = feedbackList.stream().filter(f -> f.getScore() <= 2).count();
        long neu = total - (pos + neg);

        return Map.of(
                "Positivo", (double) pos / total * 100,
                "Neutro", (double) neu / total * 100,
                "Negativo", (double) neg / total * 100);
    }
}


