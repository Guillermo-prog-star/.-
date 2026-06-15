package com.integrityfamily.ai.service;

import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.domain.Family;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClaudeAiService")
class ClaudeAiServiceTest {

    @Mock AiProvider        aiProvider;
    @Mock ContextSynthesizer contextSynthesizer;
    @InjectMocks ClaudeAiService service;

    private final Family family = Family.builder().id(1L).name("García").build();

    @Test
    @DisplayName("sintetiza contexto con tipo NEUTRAL antes de invocar a AI")
    void synthesizesContextWithNeutral() {
        when(contextSynthesizer.synthesize(family, "NEUTRAL")).thenReturn(null);
        when(aiProvider.generateResponse(any(), isNull())).thenReturn("Respuesta.");

        service.generateFamilyResponse("Hola", family);

        verify(contextSynthesizer).synthesize(family, "NEUTRAL");
    }

    @Test
    @DisplayName("la instrucción enviada a AI contiene la transcripción")
    void instructionContainsTranscription() {
        when(contextSynthesizer.synthesize(family, "NEUTRAL")).thenReturn(null);
        when(aiProvider.generateResponse(any(), isNull())).thenReturn("OK");

        service.generateFamilyResponse("¿Cómo estamos?", family);

        verify(aiProvider).generateResponse(argThat(p -> p.contains("¿Cómo estamos?")), isNull());
    }

    @Test
    @DisplayName("retorna la respuesta exacta de aiProvider")
    void returnsAiProviderResponse() {
        when(contextSynthesizer.synthesize(family, "NEUTRAL")).thenReturn(null);
        when(aiProvider.generateResponse(any(), isNull())).thenReturn("Respuesta empática.");

        String result = service.generateFamilyResponse("Mensaje", family);

        assertThat(result).isEqualTo("Respuesta empática.");
    }
}
