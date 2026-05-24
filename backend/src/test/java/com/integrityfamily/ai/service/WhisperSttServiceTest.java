package com.integrityfamily.ai.service;

import com.integrityfamily.ai.config.AiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de WhisperSttService.
 *
 * Cubre:
 *   1. Ruta de simulación — retorna transcripción fija cuando voice está deshabilitado
 *      o la api-key está ausente / es un valor MOCK_KEY.
 *   2. Ruta de simulación — instanciación exitosa sin lanzar excepciones.
 *   3. Verificación de que el constructor no lanza cuando los valores por defecto aplican.
 *
 * Nota: la ruta HTTP real (POST a api.openai.com) no se prueba aquí porque requeriría
 * un servidor mock externo (MockWebServer / WireMock). Esa cobertura corresponde a
 * una prueba de integración. Los tests aquí validan la lógica de negocio del servicio.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WhisperSttService — Unit Tests")
class WhisperSttServiceTest {

    private AiProperties props;

    @BeforeEach
    void buildProperties() {
        props = new AiProperties();
        // Defaults: voice.enabled=false, openai.apiKey="", whisper.url set, model="whisper-1"
    }

    // ─── helpers ────────────────────────────────────────────────────────────

    /** Crea un servicio con las propiedades dadas. */
    private WhisperSttService service(boolean voiceEnabled, String apiKey) {
        props.getVoice().setEnabled(voiceEnabled);
        props.getOpenai().setApiKey(apiKey);
        return new WhisperSttService(props);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  1. Simulation path — voice disabled
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Ruta de simulación")
    class SimulationPath {

        @Test
        @DisplayName("voice.enabled=false → retorna transcripción simulada sin llamar a la API")
        void shouldReturnSimulatedTranscription_whenVoiceIsDisabled() {
            WhisperSttService svc = service(false, "sk-realkey12345678901234567890");

            String result = svc.transcribe(new byte[]{1, 2, 3}, "audio/webm");

            assertThat(result)
                    .isNotBlank()
                    .isEqualTo("¿Cuáles son mis misiones?");
        }

        @Test
        @DisplayName("voice.enabled=true pero api-key vacío → retorna transcripción simulada")
        void shouldReturnSimulatedTranscription_whenApiKeyIsBlank() {
            WhisperSttService svc = service(true, "");

            String result = svc.transcribe(new byte[]{1, 2, 3}, "audio/wav");

            assertThat(result).isEqualTo("¿Cuáles son mis misiones?");
        }

        @Test
        @DisplayName("voice.enabled=true y api-key=MOCK_KEY → retorna transcripción simulada")
        void shouldReturnSimulatedTranscription_whenApiKeyIsMockKey() {
            WhisperSttService svc = service(true, "MOCK_KEY");

            String result = svc.transcribe(new byte[]{1, 2, 3}, "audio/mp3");

            assertThat(result).isEqualTo("¿Cuáles son mis misiones?");
        }

        @Test
        @DisplayName("api-key=null → retorna transcripción simulada sin NPE")
        void shouldReturnSimulatedTranscription_whenApiKeyIsNull() {
            WhisperSttService svc = service(false, null);

            String result = svc.transcribe(new byte[]{0}, "audio/ogg");

            assertThat(result).isEqualTo("¿Cuáles son mis misiones?");
        }

        @Test
        @DisplayName("audioBytes vacío + voz deshabilitada → retorna simulación (no exception)")
        void shouldHandleEmptyAudioBytes_withSimulation() {
            WhisperSttService svc = service(false, "");

            String result = svc.transcribe(new byte[0], "audio/webm");

            assertThat(result).isNotBlank();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  2. Constructor — no lanza con valores por defecto
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("Construcción con defaults de AiProperties → no lanza excepción")
        void shouldConstructSuccessfully_withDefaultProperties() {
            // AiProperties tiene url de whisper válida por defecto, timeout=30000
            // Si el constructor lanza, el test falla automáticamente
            WhisperSttService svc = new WhisperSttService(props);
            assertThat(svc).isNotNull();
        }

        @Test
        @DisplayName("Construcción con timeout personalizado → no lanza excepción")
        void shouldConstructSuccessfully_withCustomTimeout() {
            props.getOpenai().getWhisper().setTimeoutMs(5000);
            WhisperSttService svc = new WhisperSttService(props);
            assertThat(svc).isNotNull();
        }

        @Test
        @DisplayName("Construcción con URL personalizada → no lanza excepción")
        void shouldConstructSuccessfully_withCustomUrl() {
            props.getOpenai().getWhisper().setUrl("https://custom.whisper.endpoint/v1/audio/transcriptions");
            WhisperSttService svc = new WhisperSttService(props);
            assertThat(svc).isNotNull();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  3. Simulation path — mimeType null no lanza NPE
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("mimeType null + voz deshabilitada → retorna simulación sin NPE")
    void shouldHandleNullMimeType_inSimulationPath() {
        WhisperSttService svc = service(false, "");

        String result = svc.transcribe(new byte[]{1}, null);

        assertThat(result).isNotBlank();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  4. Ruta HTTP real — RestClient mock con cadena explícita
    // ═══════════════════════════════════════════════════════════════════════

    // Lenient: @BeforeEach wires all stubs; individual tests only use a subset
    @org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
    @Nested
    @DisplayName("Ruta real — POST a Whisper API (RestClient mock)")
    class RealApiPath {

        // Spring 6.x: body() is generic — RETURNS_SELF handles it without type-inference issues
        @Mock RestClient                                            mockHttp;
        @Mock RestClient.RequestBodyUriSpec                        postSpec;
        @Mock(answer = Answers.RETURNS_SELF) RestClient.RequestBodySpec bodySpec;
        @Mock RestClient.ResponseSpec                              responseSpec;

        @Captor ArgumentCaptor<MultiValueMap<String, Object>> bodyCaptor;

        private WhisperSttService svc;

        @BeforeEach
        @SuppressWarnings("unchecked")
        void setUp() {
            AiProperties p = new AiProperties();
            p.getVoice().setEnabled(true);
            p.getOpenai().setApiKey("sk-real-0000000000000000000000000000");
            svc = new WhisperSttService(p, mockHttp);

            when(mockHttp.post()).thenReturn(postSpec);
            when(postSpec.contentType(MediaType.MULTIPART_FORM_DATA)).thenReturn(bodySpec);
            // bodySpec.body(parts) → bodySpec handled by RETURNS_SELF
            when(bodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(any(Class.class))).thenReturn(null); // null → ""
        }

        @Test
        @DisplayName("POST es invocado y la respuesta null se mapea a cadena vacía")
        void shouldCallPost_andMapNullResponseToEmptyString() {
            String result = svc.transcribe(new byte[]{1, 2, 3}, "audio/webm");

            assertThat(result).isEmpty();
            verify(mockHttp).post();
        }

        @Test
        @DisplayName("El cuerpo multipart contiene las partes 'file' y 'model'")
        void shouldSendMultipartBodyWithFileAndModelParts() {
            svc.transcribe(new byte[]{10, 20, 30}, "audio/webm");

            verify(bodySpec).body(bodyCaptor.capture());
            MultiValueMap<String, Object> parts = bodyCaptor.getValue();
            assertThat(parts).containsKey("file");
            assertThat(parts).containsKey("model");
        }

        @Test
        @DisplayName("RestClientException se envuelve en RuntimeException con mensaje descriptivo")
        void shouldWrapRestClientException_asRuntimeException() {
            when(bodySpec.retrieve()).thenThrow(new RestClientException("network error"));

            assertThatThrownBy(() -> svc.transcribe(new byte[]{1, 2, 3}, "audio/wav"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Whisper")
                    .hasCauseInstanceOf(RestClientException.class);
        }
    }
}
