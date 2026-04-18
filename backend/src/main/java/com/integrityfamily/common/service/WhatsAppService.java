package com.integrityfamily.common.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);
    private final RestClient restClient;

    @Value("${app.whatsapp.api-url:https://api.whatsapp.com/v1/messages}")
    private String apiUrl;

    @Value("${app.whatsapp.token:MOCK_TOKEN}")
    private String apiToken;

    public WhatsAppService() {
        this.restClient = RestClient.builder().build();
    }

    public void sendMessage(String phoneNumber, String message) {
        if ("MOCK_TOKEN".equals(apiToken)) {
            log.info("📱 [PROACTIVE-NOTIFICATION] SIMULACIÓN: Enviando a +{} >> {}", phoneNumber, message);
            return;
        }

        try {
            log.info("📱 [WHATSAPP-HUB] Despachando mensaje real a +{}", phoneNumber);
            restClient.post()
                    .uri(apiUrl)
                    .header("Authorization", "Bearer " + apiToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "messaging_product", "whatsapp",
                            "to", phoneNumber,
                            "type", "text",
                            "text", Map.of("body", message)
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("❌ [WHATSAPP-ERROR] Fallo en la entrega: {}", e.getMessage());
        }
    }
}
