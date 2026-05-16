package com.integrityfamily.ai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * SDD-STT-02: Whisper Speech-to-Text Service.
 * ImplementaciÃƒÂ³n desacoplada mediante @Value, activa solo cuando la voz estÃƒÂ¡ habilitada globalmente.
 */
@Service
public class WhisperSttService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WhisperSttService.class);

    @Value("${app.ai.voice.enabled:false}")
    private boolean voiceEnabled;

    @Value("${app.ai.openai.whisper.url:https://api.openai.com/v1/audio/transcriptions}")
    private String whisperUrl;

    @Value("${app.ai.openai.whisper.model:whisper-1}")
    private String whisperModel;

    @Value("${app.ai.openai.api-key:}")
    private String apiKey;

    @Value("${app.ai.openai.whisper.timeout-ms:30000}")
    private long timeoutMs;

    public String transcribe(byte[] audioBytes, String mimeType) {
        if (!voiceEnabled || apiKey == null || apiKey.isBlank() || "MOCK_KEY".equals(apiKey)) {
            log.info("🤖 [STT-SIMULATION] Retornando transcripción simulada.");
            return "¿Cuáles son mis misiones?";
        }
        
        // TODO: implementación real con RestClient sobre whisperUrl,
        //       multipart upload de audioBytes con mimeType, model=whisperModel,
        //       timeout=timeoutMs, parsear respuesta JSON {"text": "..."}.
        throw new UnsupportedOperationException("transcribe() pendiente de implementación HTTP sobre " + whisperUrl);
    }
}
