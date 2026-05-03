package com.integrityfamily.ai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * SDD-STT-02: Whisper Speech-to-Text Service.
 * ImplementaciÃƒÂ³n desacoplada mediante @Value, activa solo cuando la voz estÃƒÂ¡ habilitada globalmente.
 */
@Service
@ConditionalOnProperty(name = "app.ai.voice.enabled", havingValue = "true")
public class WhisperSttService {

    @Value("${app.ai.openai.whisper.url:https://api.openai.com/v1/audio/transcriptions}")
    private String whisperUrl;

    @Value("${app.ai.openai.whisper.model:whisper-1}")
    private String whisperModel;

    @Value("${app.ai.openai.api-key:}")
    private String apiKey;

    @Value("${app.ai.openai.whisper.timeout-ms:30000}")
    private long timeoutMs;

    public String transcribe(byte[] audioBytes, String mimeType) {
        if (apiKey == null || apiKey.isBlank() || "MOCK_KEY".equals(apiKey)) {
            throw new IllegalStateException(
                "OPENAI_API_KEY no configurada; deshabilita app.ai.voice.enabled o provee la clave real");
        }
        
        // TODO: implementaciÃƒÂ³n real con RestClient sobre whisperUrl,
        //       multipart upload de audioBytes con mimeType, model=whisperModel,
        //       timeout=timeoutMs, parsear respuesta JSON {"text": "..."}.
        throw new UnsupportedOperationException("transcribe() pendiente de implementaciÃƒÂ³n HTTP sobre " + whisperUrl);
    }
}


