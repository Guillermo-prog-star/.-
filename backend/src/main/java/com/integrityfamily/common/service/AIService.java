package com.integrityfamily.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiService {

    private final RestClient restClient;

    @Value("${app.ai.gemini.api-key:YOUR_KEY_HERE}")
    private String apiKey;

    private static final String GEMINI_URL = 
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    public AiService() {
        this.restClient = RestClient.builder().build();
    }

    /**
     * Genera una síntesis espiritual y psicológica basada en el diagnóstico.
     */
    public String generateSynthesis(Map<String, Object> context) {
        String prompt = "Actúa como Terapeuta Familiar. Analiza estos resultados: " + context.toString() + 
                        ". Genera una síntesis cálida de 3 párrafos.";
        return callGemini(prompt, "La IA está procesando otros diagnósticos. Revisa el gráfico de radar.");
    }

    /**
     * Genera misiones específicas para el plan de acción en formato JSON.
     */
    public String generateMissions(Map<String, Object> context) {
        String prompt = "Genera 3 misiones familiares para riesgo " + context.get("riskLevel") + 
                        " y crisis " + context.get("hasCrisis") + ". Responde SOLO un JSON array: [{\"title\":\"...\", \"description\":\"...\"}]";
        return callGemini(prompt, "[]");
    }

    /**
     * Genera un mensaje cálido para WhatsApp.
     */
    public String generateNotificationCopy(String familyName, String topic) {
        String prompt = "Escribe un mensaje de WhatsApp cálido para la familia " + familyName + " sobre: " + topic;
        return callGemini(prompt, "¡Hola! Tienes un nuevo plan de bienestar listo.");
    }

    private String callGemini(String prompt, String fallback) {
        if ("YOUR_KEY_HERE".equals(apiKey)) {
            log.warn("⚠️ [AI-GATEWAY] API Key de Gemini no configurada. Operando en modo silencioso.");
            return fallback;
        }

        try {
            log.info("🚀 [AI-GATEWAY] Consultando a Gemini...");
            
            Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                    Map.of("parts", List.of(
                        Map.of("text", prompt)
                    ))
                )
            );

            Map<String, Object> response = restClient.post()
                    .uri(GEMINI_URL + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                Map<String, Object> firstCandidate = candidates.get(0);
                Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                return (String) parts.get(0).get("text");
            }
        } catch (Exception e) {
            log.error("❌ [AI-GATEWAY] Error de conexión con Google: {}", e.getMessage());
        }
        return fallback;
    }
}
