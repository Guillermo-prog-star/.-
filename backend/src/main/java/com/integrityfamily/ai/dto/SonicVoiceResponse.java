package com.integrityfamily.ai.dto;

/**
 * SDD-SONIC-01: Response contract for voice-to-voice interaction.
 */
public record SonicVoiceResponse(
    String transcription,
    String aiResponse,
    String audioBase64 // MP3 data encoded in Base64
) {}


