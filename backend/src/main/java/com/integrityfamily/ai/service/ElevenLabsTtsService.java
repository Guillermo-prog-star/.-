package com.integrityfamily.ai.service;

import com.integrityfamily.ai.config.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@ConditionalOnProperty(prefix = "app.ai.elevenlabs", name = "enabled", havingValue = "true")
public class ElevenLabsTtsService {

    private static final Logger log = LoggerFactory.getLogger(ElevenLabsTtsService.class);

    private final AiProperties.Elevenlabs config;
    private final RestClient http;

    public ElevenLabsTtsService(AiProperties props) {
        this.config = props.getElevenlabs();
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            log.warn("ElevenLabsTtsService habilitado pero api-key vacÃƒÂ­o.");
        }
        this.http = RestClient.builder()
                .baseUrl(config.getBaseUrl())
                .defaultHeader("xi-api-key", config.getApiKey())
                .build();
    }

    public String synthesize(String text) {
        // ImplementaciÃƒÂ³n real de la llamada a ElevenLabs.
        throw new UnsupportedOperationException("Implementar llamada TTS a " + config.getBaseUrl());
    }
}


