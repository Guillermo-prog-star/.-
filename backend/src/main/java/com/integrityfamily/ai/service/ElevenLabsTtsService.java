package com.integrityfamily.ai.service;

import com.integrityfamily.ai.config.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

/**
 * SDD-TTS-01: ElevenLabs Text-to-Speech Service.
 *
 * <p>Convierte texto en audio MP3 vía la API de ElevenLabs:
 * {@code POST /v1/text-to-speech/{voice_id}}.
 *
 * <p>Se activa únicamente cuando {@code app.ai.elevenlabs.enabled=true}.
 * En entornos de desarrollo o cuando el bean no está presente, el pipeline de voz
 * (SonicService) continúa devolviendo solo texto.
 */
@Service
@ConditionalOnProperty(prefix = "app.ai.elevenlabs", name = "enabled", havingValue = "true")
public class ElevenLabsTtsService {

    private static final Logger log = LoggerFactory.getLogger(ElevenLabsTtsService.class);

    // Parámetros de voz recomendados por ElevenLabs para conversación natural
    private static final double DEFAULT_STABILITY        = 0.5;
    private static final double DEFAULT_SIMILARITY_BOOST = 0.75;

    private final AiProperties.Elevenlabs config;
    private final RestClient              http;

    public ElevenLabsTtsService(AiProperties props) {
        this.config = props.getElevenlabs();

        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            log.warn("⚠️ [TTS] ElevenLabsTtsService habilitado pero api-key está vacío.");
        }
        if (config.getVoiceId() == null || config.getVoiceId().isBlank()) {
            log.warn("⚠️ [TTS] ElevenLabsTtsService habilitado pero voice-id está vacío.");
        }

        HttpClient jdkClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getTimeoutMs()))
                .build();

        this.http = RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(jdkClient))
                .baseUrl(config.getBaseUrl())
                .defaultHeader("xi-api-key", config.getApiKey() != null ? config.getApiKey() : "")
                .build();
    }

    /**
     * Sintetiza texto en audio MP3 usando ElevenLabs.
     *
     * @param text texto a sintetizar (no puede ser nulo ni vacío)
     * @return bytes del audio MP3 generado
     * @throws IllegalArgumentException si el voice-id no está configurado
     * @throws RuntimeException         si la llamada a la API falla
     */
    public byte[] synthesize(String text) {
        if (config.getVoiceId() == null || config.getVoiceId().isBlank()) {
            throw new RuntimeException("ElevenLabs voice-id no está configurado — "
                    + "establece app.ai.elevenlabs.voice-id en la configuración.");
        }
        if (text == null || text.isBlank()) {
            log.warn("⚠️ [TTS] Se recibió texto vacío — retornando audio vacío.");
            return new byte[0];
        }

        // ElevenLabs requiere: { text, model_id, voice_settings: { stability, similarity_boost } }
        Map<String, Object> requestBody = Map.of(
                "text",           text,
                "model_id",       config.getModel(),
                "voice_settings", Map.of(
                        "stability",        DEFAULT_STABILITY,
                        "similarity_boost", DEFAULT_SIMILARITY_BOOST
                )
        );

        log.debug("🔊 [TTS] Sintetizando {} chars — modelo={}, voz={}",
                text.length(), config.getModel(), config.getVoiceId());

        try {
            byte[] audioBytes = http.post()
                    .uri("/text-to-speech/{voiceId}", config.getVoiceId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.parseMediaType("audio/mpeg"))
                    .body(requestBody)
                    .retrieve()
                    .body(byte[].class);

            if (audioBytes == null || audioBytes.length == 0) {
                throw new RuntimeException("ElevenLabs retornó audio vacío para el texto proporcionado.");
            }

            log.debug("✅ [TTS] Audio recibido: {} bytes", audioBytes.length);
            return audioBytes;

        } catch (RestClientException e) {
            log.error("❌ [TTS] Error al llamar a ElevenLabs API: {}", e.getMessage(), e);
            throw new RuntimeException("Error en síntesis de voz con ElevenLabs: " + e.getMessage(), e);
        }
    }
}
