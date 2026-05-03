package com.integrityfamily.ai.provider.impl;

import com.integrityfamily.ai.config.AiProperties;
import com.integrityfamily.ai.dto.AiContext;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.ai.service.PromptGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * SDD-AI-04.2: Anthropic Claude Production Implementation.
 * Puente de inteligencia sistÃƒÂ©mica para el Nodo Armenia.
 */
@Service
@Primary
@Slf4j
@RequiredArgsConstructor
public class ClaudeAiProvider implements AiProvider {

    private final PromptGenerator promptGenerator;
    private final RestTemplate restTemplate;
    private final AiProperties aiProperties;

    @Override
    public String generateResponse(String userMessage, AiContext context) {
        String apiKey = aiProperties.getAnthropic().getApiKey();
        String model = aiProperties.getAnthropic().getModel();
        String baseUrl = aiProperties.getAnthropic().getBaseUrl();

        // ValidaciÃƒÂ³n de Conciencia (Secretos)
        if ("MOCK_KEY".equals(apiKey) || apiKey == null || apiKey.isEmpty()) {
            log.warn("[NODO ARMENIA] API Key de Claude ausente. Generando simulaciÃƒÂ³n.");
            
            if (userMessage.contains("JSON")) {
                return """
                [
                  {
                    "title": "Misión de Reconocimiento Emocional",
                    "description": "Dedicar 15 minutos al día para validar las emociones de los hijos sin juzgar.",
                    "dimension": "EMOCIONES",
                    "periodicityMonths": 1
                  },
                  {
                    "title": "Círculo de Comunicación Asertiva",
                    "description": "Reunión semanal para expresar necesidades usando el lenguaje del 'Yo'.",
                    "dimension": "COMUNICACION",
                    "periodicityMonths": 3
                  },
                  {
                    "title": "Ritual de Hábitos de Integridad",
                    "description": "Establecer una rutina de cena sin dispositivos electrónicos.",
                    "dimension": "HABITOS",
                    "periodicityMonths": 6
                  }
                ]
                """;
            }

            String familyName = (context != null && context.family() != null) ? context.family().name() : "Desconocido";
            return "### 💡 SIMULACIÓN (SDD)\nContexto: " + familyName +
                    "\nNarrativa: La potencia de actuar de la familia se encuentra en equilibrio latente.";
        }

        try {
            String fullPrompt = promptGenerator.buildPrompt(userMessage, context);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", "2023-06-01");

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "max_tokens", 1024,
                    "messages", List.of(Map.of("role", "user", "content", fullPrompt)));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            log.info("[NODO ARMENIA] Invocando Claude ({}) via {}...", model, baseUrl);

            String url = baseUrl + "/messages";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().get("content");
                if (content != null && !content.isEmpty()) {
                    return (String) content.get(0).get("text");
                }
            }

            return "### Ã¢Å¡Â Ã¯Â¸Â ERROR DE INFERENCIA\nStatus: " + response.getStatusCode();

        } catch (Exception e) {
            log.error("Ã¢ÂÅ’ FALLA CRÃƒÂTICA EN CLAUDE PROVIDER: {}", e.getMessage());
            return "### Ã¢ÂÅ’ ERROR DE CONEXIÃƒâ€œN SISTÃƒâ€°MICA\nEl nodo de inteligencia no respondiÃƒÂ³ al estÃƒÂ­mulo.";
        }
    }

    @Override
    public String generateRawResponse(String rawPrompt) {
        return generateResponse(rawPrompt, null);
    }

    @Override
    public String getProviderId() {
        return "CLAUDE_PRO";
    }
}


