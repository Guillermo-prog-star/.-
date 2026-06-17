package com.integrityfamily.ai.service;

import com.integrityfamily.ai.dto.AiContext;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.ai.provider.TaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

    // Claude: priority=10 (más capaz), Gemini: priority=20 (más barato)
    @Mock AiProvider claudeProvider;
    @Mock AiProvider geminiProvider;

    AiProviderSelector selector;

    @BeforeEach
    void setUp() {
        lenient().when(claudeProvider.getPriority()).thenReturn(10);
        lenient().when(geminiProvider.getPriority()).thenReturn(20);
        lenient().when(claudeProvider.available()).thenReturn(true);
        lenient().when(geminiProvider.available()).thenReturn(true);
        selector = new AiProviderSelector(List.of(claudeProvider, geminiProvider));
    }

    @Nested
    @DisplayName("selectProvider(AiContext)")
    class SelectByContext {

        @Test
        @DisplayName("sentinelActive=true → proveedor más capaz (priority menor = Claude)")
        void sentinelActive_selectsHighCapacity() {
            AiContext ctx = mock(AiContext.class);
            when(ctx.sentinelActive()).thenReturn(true);

            AiProvider result = selector.selectProvider(ctx);

            assertThat(result).isSameAs(claudeProvider);
        }

        @Test
        @DisplayName("sentinelActive=false → proveedor más barato (priority mayor = Gemini)")
        void sentinelInactive_selectsCostOptimized() {
            AiContext ctx = mock(AiContext.class);
            when(ctx.sentinelActive()).thenReturn(false);

            AiProvider result = selector.selectProvider(ctx);

            assertThat(result).isSameAs(geminiProvider);
        }

        @Test
        @DisplayName("sentinelActive=true pero Claude no disponible → cae a Gemini")
        void sentinelActive_claudeUnavailable_fallsToGemini() {
            when(claudeProvider.available()).thenReturn(false);
            AiContext ctx = mock(AiContext.class);
            when(ctx.sentinelActive()).thenReturn(true);

            AiProvider result = selector.selectProvider(ctx);

            assertThat(result).isSameAs(geminiProvider);
        }
    }

    @Nested
    @DisplayName("selectProvider(TaskType)")
    class SelectByTaskType {

        @Test
        @DisplayName("LIGHTWEIGHT → siempre el más barato disponible (Gemini), aunque Sentinel esté activo")
        void lightweight_selectsCheapest() {
            AiProvider result = selector.selectProvider(TaskType.LIGHTWEIGHT);

            assertThat(result).isSameAs(geminiProvider);
        }

        @Test
        @DisplayName("HIGH_CAPACITY → siempre el más capaz disponible (Claude)")
        void highCapacity_selectsMostCapable() {
            AiProvider result = selector.selectProvider(TaskType.HIGH_CAPACITY);

            assertThat(result).isSameAs(claudeProvider);
        }

        @Test
        @DisplayName("LIGHTWEIGHT con Gemini no disponible → cae a Claude")
        void lightweight_geminiUnavailable_fallsToClaude() {
            when(geminiProvider.available()).thenReturn(false);

            AiProvider result = selector.selectProvider(TaskType.LIGHTWEIGHT);

            assertThat(result).isSameAs(claudeProvider);
        }
    }

    @Nested
    @DisplayName("getReportingProvider()")
    class Reporting {

        @Test
        @DisplayName("siempre selecciona el más capaz (Claude)")
        void reportingProvider_selectsMostCapable() {
            AiProvider result = selector.getReportingProvider();

            assertThat(result).isSameAs(claudeProvider);
        }

        @Test
        @DisplayName("Claude no disponible → cae a Gemini para reportes")
        void reportingProvider_claudeUnavailable_fallsToGemini() {
            when(claudeProvider.available()).thenReturn(false);

            AiProvider result = selector.getReportingProvider();

            assertThat(result).isSameAs(geminiProvider);
        }
    }
}
