package com.integrityfamily.ai.service;

import com.integrityfamily.ai.dto.SonicResponse;
import com.integrityfamily.domain.Family;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
public class SonicService {

    private static final Logger log = LoggerFactory.getLogger(SonicService.class);

    private final Optional<WhisperSttService> stt;
    private final Optional<ElevenLabsTtsService> tts;
    private final ClaudeAiService claude;

    public SonicService(Optional<WhisperSttService> stt,
                        Optional<ElevenLabsTtsService> tts,
                        ClaudeAiService claude) {
        this.stt = stt;
        this.tts = tts;
        this.claude = claude;
    }

    public SonicResponse processVoiceChat(byte[] audioBytes,
                                          String mimeType,
                                          Family family) {
        Objects.requireNonNull(family, "family no puede ser null");
        
        // Pipeline de voz orquestado
        WhisperSttService sttService = stt.orElseThrow(() ->
                new RuntimeException("STT no disponible"));

        String transcription = sttService.transcribe(audioBytes, mimeType);
        String aiText = claude.generateFamilyResponse(transcription, family);

        // Si tts estÃƒÂ¡ presente, podrÃƒÂ­amos usarlo aquÃƒÂ­, pero el controlador 
        // ahora solo espera texto (SonicResponse).
        
        return new SonicResponse(transcription, aiText);
    }
}


