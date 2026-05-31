package com.integrityfamily.ai.service;

import com.integrityfamily.ai.dto.SonicResponse;
import com.integrityfamily.domain.Family;
import com.integrityfamily.participation.service.ParticipationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests para SonicService — actualizado para firma actual:
 *   SonicService(stt, tts, aiService, participationService)
 *   processVoiceChat(audioBytes, mimeType, family, memberId)
 */
@ExtendWith(MockitoExtension.class)
class SonicServiceTest {

    @Mock private WhisperSttService        stt;
    @Mock private ElevenLabsTtsService     tts;
    @Mock private AiService                aiService;
    @Mock private ParticipationService     participationService;

    @Test
    void processVoiceChat_lanzaExcepcion_siSttDeshabilitado() {
        SonicService service = new SonicService(
                Optional.empty(), Optional.of(tts), aiService, participationService);

        Family family = new Family();
        family.setId(1L);

        assertThatThrownBy(() -> service.processVoiceChat(
                new byte[]{1, 2, 3}, "audio/webm", family, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("STT no disponible");
    }
}
