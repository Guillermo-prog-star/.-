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
 * SDD-AI-04.4: DeepSeek provider — lowest cost, OpenAI-compatible API.
 * Fallback when Gemini is unavailable and Claude is too expensive.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeepSeekAiProvider implements AiProvider {

    private final RestTemplate restTemplate;
    private final AiProperties aiProperties;

    @Override
    public String generateResponse(String userMessage, AiContext context) {
        return callDeepSeek(userMessage);
    }

    @Override
    public String generateRawResponse(String rawPrompt) {
        return callDeepSeek(rawPrompt);
    }

    @Override
    public String generateWithFullPrompt(String fullPrompt) {
        return callDeepSeek(fullPrompt);
    }

    @Override
    public String getProviderId() {
        return "DEEPSEEK";
    }

    @Override
    public boolean available() {
        String key = aiProperties.getDeepseek().getApiKey();
        return aiProperties.getDeepseek().isEnabled()
                && key != null && !key.isBlank() && !"MOCK_KEY".equals(key);
    }

    @Override
    public int getPriority() {
        return 30;
    }

    private String callDeepSeek(String prompt) {
        AiProperties.Deepseek cfg = aiProperties.getDeepseek();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(cfg.getApiKey());

            Map<String, Object> body = Map.of(
                    "model", cfg.getModel(),
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "max_tokens", 1024);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            log.info("[DEEPSEEK] Invocando {} ...", cfg.getModel());

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    cfg.getBaseUrl() + "/chat/completions", entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices =
                        (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String text = (String) message.get("content");
                    log.info("[DEEPSEEK] Respuesta recibida ({} chars)", text != null ? text.length() : 0);
                    return text;
                }
            }
            return "### ERROR DEEPSEEK\nStatus: " + response.getStatusCode();

        } catch (Exception e) {
            log.error("[DEEPSEEK] Error: {}", e.getMessage());
            throw new RuntimeException("DeepSeek unavailable: " + e.getMessage(), e);
        }
    }
}
