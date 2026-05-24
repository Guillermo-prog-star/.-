package com.integrityfamily.ai.service;

import com.integrityfamily.ai.config.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * SDD-STT-02: Whisper Speech-to-Text Service.
 *
 * <p>Realiza transcripción de audio vía OpenAI Whisper API (POST multipart/form-data).
 * Activo solo cuando {@code app.ai.voice.enabled=true} y {@code app.ai.openai.api-key}
 * está configurado con una clave real.
 *
 * <p>Cuando la voz está deshabilitada o la clave está ausente, devuelve una transcripción
 * simulada para no romper el pipeline de pruebas ni los entornos de desarrollo.
 */
@Service
public class WhisperSttService {

    private static final Logger log = LoggerFactory.getLogger(WhisperSttService.class);

    private final AiProperties.Voice      voiceCfg;
    private final AiProperties.Openai     openaiCfg;
    private final RestClient              http;

    /** Package-private: inyecta un RestClient preconfigurado (solo para pruebas unitarias). */
    WhisperSttService(AiProperties props, RestClient http) {
        this.voiceCfg  = props.getVoice();
        this.openaiCfg = props.getOpenai();
        this.http      = http;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public WhisperSttService(AiProperties props) {
        this.voiceCfg  = props.getVoice();
        this.openaiCfg = props.getOpenai();

        AiProperties.Openai.Whisper whisper = openaiCfg.getWhisper();

        // JdkClientHttpRequestFactory usa Java 11 HttpClient — sin dependencias externas
        HttpClient jdkClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(whisper.getTimeoutMs()))
                .build();

        this.http = RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(jdkClient))
                .baseUrl(whisper.getUrl())
                .defaultHeader("Authorization", "Bearer " + openaiCfg.getApiKey())
                .build();
    }

    /**
     * Transcribe audio bytes to text using OpenAI Whisper.
     *
     * @param audioBytes raw audio content (webm, mp3, wav, ogg, flac, m4a, mp4)
     * @param mimeType   MIME type of the audio, e.g. {@code "audio/webm"}
     * @return transcribed text, never null
     * @throws RuntimeException if the Whisper API call fails (voice is enabled and key is present)
     */
    public String transcribe(byte[] audioBytes, String mimeType) {
        String key = openaiCfg.getApiKey();
        boolean hasKey = key != null && !key.isBlank() && !"MOCK_KEY".equals(key);

        if (!voiceCfg.isEnabled() || !hasKey) {
            log.info("🤖 [STT-SIMULATION] Retornando transcripción simulada (voice.enabled={}, key={})",
                    voiceCfg.isEnabled(), hasKey ? "present" : "absent");
            return "¿Cuáles son mis misiones?";
        }

        AiProperties.Openai.Whisper cfg = openaiCfg.getWhisper();
        String ext      = extensionFor(mimeType);
        String filename = "audio." + ext;

        // Parte del archivo con Content-Type correcto (Whisper infiere formato del MIME)
        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.parseMediaType(mimeType));
        HttpEntity<ByteArrayResource> filePart = new HttpEntity<>(
                new ByteArrayResource(audioBytes) {
                    @Override
                    public String getFilename() { return filename; }
                }, fileHeaders);

        // Cuerpo multipart: Whisper requiere "file" + "model"
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file",  filePart);
        parts.add("model", cfg.getModel());

        log.debug("🎙️ [STT] Enviando {} bytes ({}) a Whisper (model={})",
                audioBytes.length, mimeType, cfg.getModel());

        try {
            WhisperApiResponse result = http.post()
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(parts)
                    .retrieve()
                    .body(WhisperApiResponse.class);

            String text = (result != null && result.text() != null) ? result.text() : "";
            log.debug("✅ [STT] Transcripción recibida ({} chars): {}", text.length(), text);
            return text;

        } catch (RestClientException e) {
            log.error("❌ [STT] Error al llamar a Whisper API: {}", e.getMessage(), e);
            throw new RuntimeException("Error al transcribir audio con Whisper: " + e.getMessage(), e);
        }
    }

    // ─── helpers ────────────────────────────────────────────────────────────────

    /** Respuesta JSON de la Whisper API: {@code {"text": "..."}}. */
    private record WhisperApiResponse(String text) {}

    /**
     * Mapea MIME types de audio a las extensiones que acepta la API de Whisper.
     * Whisper soporta: mp4, mp3, wav, ogg, flac, m4a, webm.
     */
    private static String extensionFor(String mimeType) {
        if (mimeType == null) return "webm";
        return switch (mimeType.toLowerCase()) {
            case "audio/mp4"                        -> "mp4";
            case "audio/mpeg", "audio/mp3"          -> "mp3";
            case "audio/wav", "audio/wave",
                 "audio/x-wav"                      -> "wav";
            case "audio/ogg"                        -> "ogg";
            case "audio/flac"                       -> "flac";
            case "audio/x-m4a", "audio/m4a"        -> "m4a";
            default                                 -> "webm";
        };
    }
}
