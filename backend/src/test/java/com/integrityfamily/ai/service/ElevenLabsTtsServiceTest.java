package com.integrityfamily.ai.service;

import com.integrityfamily.ai.config.AiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas unitarias de ElevenLabsTtsService.
 *
 * El servicio está marcado con @ConditionalOnProperty(enabled=true), así que se instancia
 * directamente aquí sin contexto Spring.
 *
 * Cubre:
 *   1. Constructor — no lanza con diferentes configuraciones de propiedades.
 *   2. synthesize(text) — texto vacío retorna array vacío sin llamar a la API.
 *   3. synthesize(text) — voice-id no configurado lanza IllegalStateException.
 *   4. synthesize(text) — texto null no lanza NullPointerException (retorna array vacío).
 */
@DisplayName("ElevenLabsTtsService — Unit Tests")
class ElevenLabsTtsServiceTest {

    private AiProperties props;

    @BeforeEach
    void buildProperties() {
        props = new AiProperties();
        // Por defecto elevenlabs.enabled=false, pero instanciamos directamente
        // sin contexto Spring, así que @ConditionalOnProperty no aplica aquí.
        props.getElevenlabs().setApiKey("test-xi-api-key-1234");
        props.getElevenlabs().setVoiceId("voice-abc123");
        props.getElevenlabs().setModel("eleven_multilingual_v2");
        props.getElevenlabs().setBaseUrl("https://api.elevenlabs.io/v1");
        props.getElevenlabs().setTimeoutMs(5000);
    }

    // ─── helpers ────────────────────────────────────────────────────────────

    private ElevenLabsTtsService service() {
        return new ElevenLabsTtsService(props);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  1. Constructor
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("Configuración completa → instancia sin excepción")
        void shouldConstruct_withFullConfiguration() {
            ElevenLabsTtsService svc = service();
            assertThat(svc).isNotNull();
        }

        @Test
        @DisplayName("api-key vacío → instancia sin excepción (warning en log)")
        void shouldConstruct_withEmptyApiKey() {
            props.getElevenlabs().setApiKey("");
            assertThat(new ElevenLabsTtsService(props)).isNotNull();
        }

        @Test
        @DisplayName("voice-id vacío → instancia sin excepción (warning en log)")
        void shouldConstruct_withEmptyVoiceId() {
            props.getElevenlabs().setVoiceId("");
            assertThat(new ElevenLabsTtsService(props)).isNotNull();
        }

        @Test
        @DisplayName("api-key null → instancia sin NullPointerException")
        void shouldConstruct_withNullApiKey() {
            props.getElevenlabs().setApiKey(null);
            assertThat(new ElevenLabsTtsService(props)).isNotNull();
        }

        @Test
        @DisplayName("timeout personalizado (1000 ms) → instancia sin excepción")
        void shouldConstruct_withShortTimeout() {
            props.getElevenlabs().setTimeoutMs(1000);
            assertThat(new ElevenLabsTtsService(props)).isNotNull();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  2. synthesize() — guardas de entrada (sin llamada HTTP)
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("synthesize() — guardas de entrada")
    class SynthesizeGuards {

        @Test
        @DisplayName("Texto vacío → retorna array vacío sin llamar a la API")
        void shouldReturnEmptyArray_whenTextIsBlank() {
            byte[] result = service().synthesize("   ");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Texto null → retorna array vacío sin NullPointerException")
        void shouldReturnEmptyArray_whenTextIsNull() {
            byte[] result = service().synthesize(null);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Voice-id no configurado → RuntimeException descriptivo")
        void shouldThrowIllegalState_whenVoiceIdIsBlank() {
            props.getElevenlabs().setVoiceId("");
            ElevenLabsTtsService svc = new ElevenLabsTtsService(props);

            assertThatThrownBy(() -> svc.synthesize("Hola familia"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("voice-id");
        }

        @Test
        @DisplayName("Voice-id null → RuntimeException descriptivo")
        void shouldThrowIllegalState_whenVoiceIdIsNull() {
            props.getElevenlabs().setVoiceId(null);
            ElevenLabsTtsService svc = new ElevenLabsTtsService(props);

            assertThatThrownBy(() -> svc.synthesize("Texto de prueba"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("voice-id");
        }
    }
}
