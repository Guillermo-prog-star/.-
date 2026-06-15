package com.integrityfamily.ai.service;

import com.integrityfamily.ai.dto.AiContext;
import com.integrityfamily.ai.provider.AiProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiProviderSelector")
class AiProviderSelectorTest {

    @Mock AiProvider claudeProvider;
    @Mock AiProvider geminiProvider;

    AiProviderSelector selector;

    @BeforeEach
    void setUp() {
        // lenient: el stream puede encontrar el primero y no llamar al segundo
        lenient().when(claudeProvider.getProviderId()).thenReturn("CLAUDE_3_5_SONNET");
        lenient().when(geminiProvider.getProviderId()).thenReturn("GEMINI_1_5_FLASH");
        // Claude primero en la lista → es el fallback
        selector = new AiProviderSelector(List.of(claudeProvider, geminiProvider));
    }

    @Test
    @DisplayName("sentinelActive=true → selecciona CLAUDE_3_5_SONNET")
    void sentinelActive_selectsClaude() {
        AiContext ctx = mock(AiContext.class);
        when(ctx.sentinelActive()).thenReturn(true);

        AiProvider result = selector.selectProvider(ctx);

        assertThat(result).isSameAs(claudeProvider);
    }

    @Test
    @DisplayName("sentinelActive=false → selecciona GEMINI_1_5_FLASH")
    void sentinelInactive_selectsGemini() {
        AiContext ctx = mock(AiContext.class);
        when(ctx.sentinelActive()).thenReturn(false);

        AiProvider result = selector.selectProvider(ctx);

        assertThat(result).isSameAs(geminiProvider);
    }

    @Test
    @DisplayName("getReportingProvider → siempre selecciona CLAUDE_3_5_SONNET")
    void reportingProvider_selectsClaude() {
        AiProvider result = selector.getReportingProvider();

        assertThat(result).isSameAs(claudeProvider);
    }

    @Test
    @DisplayName("proveedor buscado no encontrado → fallback al primero de la lista")
    void providerNotFound_fallsBackToFirst() {
        // Selector con solo Gemini en la lista; busca CLAUDE → no encontrado → devuelve Gemini (primero)
        AiProviderSelector selectorSingleProvider = new AiProviderSelector(List.of(geminiProvider));
        AiContext ctx = mock(AiContext.class);
        when(ctx.sentinelActive()).thenReturn(true); // pide Claude

        AiProvider result = selectorSingleProvider.selectProvider(ctx);

        assertThat(result).isSameAs(geminiProvider); // fallback al primero
    }
}
