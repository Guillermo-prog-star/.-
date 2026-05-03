package com.integrityfamily.ai.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class VoiceUnavailableException extends RuntimeException {

    public VoiceUnavailableException(String message) {
        super(message);
    }

    public static VoiceUnavailableException sttDisabled() {
        return new VoiceUnavailableException(
                "Speech-to-Text no habilitado. Active perfil 'voice' y configure OPENAI_API_KEY.");
    }

    public static VoiceUnavailableException ttsDisabled() {
        return new VoiceUnavailableException(
                "Text-to-Speech no habilitado. Active perfil 'voice' y configure ELEVENLABS_API_KEY.");
    }
}


