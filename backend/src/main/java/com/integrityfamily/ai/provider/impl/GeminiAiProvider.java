package com.integrityfamily.ai.provider.impl;

import com.integrityfamily.ai.config.AiProperties;
import com.integrityfamily.ai.dto.AiContext;
import com.integrityfamily.ai.provider.AiProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * SDD-AI-04.3: Google Gemini provider — cost-optimized for routine tasks.
 * Uses generateContent REST API (no SDK dependency).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GeminiAiProvider implements AiProvider {

    private final RestTemplate restTemplate;
    private final AiProperties aiProperties;

    @Override
    public String generateResponse(String userMessage, AiContext context) {
        return callGemini(userMessage);
    }

    @Override
    public String generateRawResponse(String rawPrompt) {
        return callGemini(rawPrompt);
    }

    @Override
    public String generateWithFullPrompt(String fullPrompt) {
        return callGemini(fullPrompt);
    }

    @Override
    public String getProviderId() {
        return "GEMINI";
    }

    @Override
    public boolean available() {
        String key = aiProperties.getGemini().getApiKey();
        return aiProperties.getGemini().isEnabled()
                && key != null && !key.isBlank() && !"MOCK_KEY".equals(key);
    }

    @Override
    public int getPriority() {
        return 20;
    }

    private String callGemini(String prompt) {
        AiProperties.Gemini cfg = aiProperties.getGemini();
        String url = cfg.getBaseUrl() + "/models/" + cfg.getModel() + ":generateContent?key=" + cfg.getApiKey();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of(
                            "parts", List.of(Map.of("text", prompt)))));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            log.info("[GEMINI] Invocando {} ...", cfg.getModel());

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> candidates =
                        (List<Map<String, Object>>) response.getBody().get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    String text = (String) parts.get(0).get("text");
                    log.info("[GEMINI] Respuesta recibida ({} chars)", text != null ? text.length() : 0);
                    return text;
                }
            }
            return "### ERROR GEMINI\nStatus: " + response.getStatusCode();

        } catch (Exception e) {
            log.error("[GEMINI] Error: {}", e.getMessage());
            throw new RuntimeException("Gemini unavailable: " + e.getMessage(), e);
        }
    }
}
