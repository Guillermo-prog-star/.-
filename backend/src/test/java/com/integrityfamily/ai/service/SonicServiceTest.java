package com.integrityfamily.ai.service;

import com.integrityfamily.ai.dto.SonicResponse;
import com.integrityfamily.domain.Family;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SonicServiceTest {

    @Mock private WhisperSttService stt;
    @Mock private ElevenLabsTtsService tts;
    @Mock private ClaudeAiService claude;

    @Test
    void processVoiceChat_lanzaExcepcion_siSttDeshabilitado() {
        SonicService service = new SonicService(Optional.empty(), Optional.of(tts), claude);
        Family family = new Family();
        family.setId(1L);

        assertThatThrownBy(() -> service.processVoiceChat(new byte[]{1, 2, 3}, "audio/webm", family))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("STT no disponible");
    }

    @Test
    void processVoiceChat_orquestaPipelineCompleto_cuandoTodoHabilitado() {
        SonicService service = new SonicService(Optional.of(stt), Optional.of(tts), claude);
        Family family = new Family();
        family.setId(42L);
        byte[] audio = new byte[]{1, 2, 3};

        when(stt.transcribe(audio, "audio/webm")).thenReturn("hola familia");
        when(claude.generateFamilyResponse("hola familia", family)).thenReturn("Hola, ¿cómo están?");

        SonicResponse response = service.processVoiceChat(audio, "audio/webm", family);

        assertThat(response.transcript()).isEqualTo("hola familia");
        assertThat(response.assistantReply()).isEqualTo("Hola, ¿cómo están?");
    }
}
