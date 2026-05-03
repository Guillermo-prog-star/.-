package com.integrityfamily.ai.dto;

public record VoiceChatResponse(
        String transcription,
        String aiResponseText,
        byte[] aiResponseAudio,
        String audioMimeType,
        Long familyId
) {
    public static VoiceChatResponse of(String transcription,
                                       String aiResponseText,
                                       byte[] aiResponseAudio,
                                       Long familyId) {
        return new VoiceChatResponse(
                transcription,
                aiResponseText,
                aiResponseAudio,
                "audio/mpeg",
                familyId
        );
    }
}


