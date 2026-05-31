package com.integrityfamily.ai.service;

import com.integrityfamily.ai.dto.SonicResponse;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.ParticipationEventType;
import com.integrityfamily.participation.service.ParticipationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

@Service
public class SonicService {

    private static final Logger log = LoggerFactory.getLogger(SonicService.class);

    private final Optional<WhisperSttService> stt;
    private final Optional<ElevenLabsTtsService> tts;
    private final AiService aiService;
    private final ParticipationService participationService;

    public SonicService(Optional<WhisperSttService> stt,
                        Optional<ElevenLabsTtsService> tts,
                        @Lazy AiService aiService,
                        ParticipationService participationService) {
        this.stt = stt;
        this.tts = tts;
        this.aiService = aiService;
        this.participationService = participationService;
    }

    public SonicResponse processVoiceChat(byte[] audioBytes,
                                          String mimeType,
                                          Family family,
                                          Long memberId) {
        Objects.requireNonNull(family, "family no puede ser null");

        WhisperSttService sttService = stt.orElseThrow(() ->
                new RuntimeException("STT no disponible"));

        String transcription = sttService.transcribe(audioBytes, mimeType);

        // Route through the full cognitive pipeline (Fases A-E): session, arc, identity, memory
        participationService.record(family.getId(), memberId, ParticipationEventType.VOICE_MESSAGE);
        String aiText = aiService.processInteractiveChat(transcription, family, memberId).getContent();

        String audioBase64 = null;
        if (tts.isPresent()) {
            try {
                byte[] audioBytesTts = tts.get().synthesize(aiText);
                if (audioBytesTts != null && audioBytesTts.length > 0) {
                    audioBase64 = Base64.getEncoder().encodeToString(audioBytesTts);
                }
            } catch (Exception e) {
                log.warn("⚠️ [SonicService] Failed to synthesize TTS via ElevenLabs: {}", e.getMessage());
            }
        }

        return new SonicResponse(transcription, aiText, audioBase64);
    }
}


