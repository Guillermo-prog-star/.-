package com.integrityfamily.scanner.service;

import com.integrityfamily.common.service.WhatsAppService;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.scanner.dto.SubtleSignalRadarResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RadarAlertService — Unit Tests")
class RadarAlertServiceTest {

    @Mock SubtleSignalRadarService radarService;
    @Mock FamilyRepository         familyRepository;
    @Mock WhatsAppService          whatsAppService;

    @InjectMocks RadarAlertService service;

    private static final Long FAM_ID = 1L;
    private Family family;

    @BeforeEach
    void setUp() {
        family = new Family();
        family.setId(FAM_ID);
        family.setName("Familia López");
        family.setWhatsapp("+573001234567");
    }

    private void stubFamilyFound() {
        when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private SubtleSignalRadarResponse radarWith(List<SubtleSignalRadarResponse.MicroSignal> signals) {
        return new SubtleSignalRadarResponse(
                FAM_ID, 3, null, null, null, null, null,
                signals, List.of(), List.of(), 85, "Narrativa.", LocalDateTime.now()
        );
    }

    private SubtleSignalRadarResponse.MicroSignal signal(String dim, String severity) {
        return new SubtleSignalRadarResponse.MicroSignal(dim, "CODE", "Descripción de señal.", severity, 0.8);
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("checkAndAlert() — familia no encontrada")
    class FamilyNotFound {

        @Test
        @DisplayName("lanza IllegalArgumentException cuando la familia no existe")
        void throwsWhenFamilyNotFound() {
            when(familyRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.checkAndAlert(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Familia no encontrada");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("checkAndAlert() — sin señales HIGH")
    class NoHighSignals {

        @Test
        @DisplayName("devuelve false y no envía WhatsApp cuando no hay señales HIGH")
        void returnsFalseWithNoHighSignals() {
            stubFamilyFound(); when(radarService.analyze(FAM_ID)).thenReturn(radarWith(List.of(
                    signal("emociones", "LOW"),
                    signal("comunicacion", "MEDIUM")
            )));

            boolean result = service.checkAndAlert(FAM_ID);

            assertThat(result).isFalse();
            verifyNoInteractions(whatsAppService);
        }

        @Test
        @DisplayName("devuelve false y no envía WhatsApp cuando no hay microseñales")
        void returnsFalseWithEmptySignals() {
            stubFamilyFound(); when(radarService.analyze(FAM_ID)).thenReturn(radarWith(List.of()));

            boolean result = service.checkAndAlert(FAM_ID);

            assertThat(result).isFalse();
            verifyNoInteractions(whatsAppService);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("checkAndAlert() — con señales HIGH y WhatsApp configurado")
    class WithHighSignalsAndWhatsApp {

        @Test
        @DisplayName("devuelve true y llama sendToFamily cuando hay señales HIGH")
        void returnsTrueAndSendsWhatsApp() {
            stubFamilyFound(); when(radarService.analyze(FAM_ID)).thenReturn(radarWith(List.of(
                    signal("emociones", "HIGH"),
                    signal("comunicacion", "MEDIUM")
            )));

            boolean result = service.checkAndAlert(FAM_ID);

            assertThat(result).isTrue();
            verify(whatsAppService).sendToFamily(eq(family), anyString());
        }

        @Test
        @DisplayName("el mensaje incluye el nombre de la familia")
        void messageIncludesFamilyName() {
            stubFamilyFound(); when(radarService.analyze(FAM_ID)).thenReturn(radarWith(List.of(
                    signal("habitos", "HIGH")
            )));

            service.checkAndAlert(FAM_ID);

            ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
            verify(whatsAppService).sendToFamily(any(), msgCaptor.capture());
            assertThat(msgCaptor.getValue()).contains("Familia López");
        }

        @Test
        @DisplayName("el mensaje incluye la descripción de cada señal HIGH")
        void messageIncludesSignalDescriptions() {
            stubFamilyFound(); when(radarService.analyze(FAM_ID)).thenReturn(radarWith(List.of(
                    new SubtleSignalRadarResponse.MicroSignal(
                            "emociones", "CODE_1", "Tensión emocional detectada.", "HIGH", 0.9),
                    signal("comunicacion", "LOW")
            )));

            service.checkAndAlert(FAM_ID);

            ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
            verify(whatsAppService).sendToFamily(any(), msgCaptor.capture());
            assertThat(msgCaptor.getValue()).contains("Tensión emocional detectada.");
        }

        @Test
        @DisplayName("el mensaje NO incluye señales de severidad MEDIUM o LOW")
        void messageOnlyIncludesHighSignals() {
            stubFamilyFound(); when(radarService.analyze(FAM_ID)).thenReturn(radarWith(List.of(
                    new SubtleSignalRadarResponse.MicroSignal("emociones", "C1", "Solo alta.", "HIGH", 0.9),
                    new SubtleSignalRadarResponse.MicroSignal("habitos", "C2", "Solo media.", "MEDIUM", 0.5)
            )));

            service.checkAndAlert(FAM_ID);

            ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
            verify(whatsAppService).sendToFamily(any(), msgCaptor.capture());
            assertThat(msgCaptor.getValue())
                    .contains("Solo alta.")
                    .doesNotContain("Solo media.");
        }

        @Test
        @DisplayName("el mensaje incluye el porcentaje de confianza")
        void messageIncludesConfidenceScore() {
            stubFamilyFound(); when(radarService.analyze(FAM_ID)).thenReturn(radarWith(List.of(
                    signal("tiempos", "HIGH")
            )));

            service.checkAndAlert(FAM_ID);

            ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
            verify(whatsAppService).sendToFamily(any(), msgCaptor.capture());
            assertThat(msgCaptor.getValue()).contains("85%");
        }

        @Test
        @DisplayName("envía solo una vez aunque haya múltiples señales HIGH")
        void sendsOnlyOnce() {
            stubFamilyFound(); when(radarService.analyze(FAM_ID)).thenReturn(radarWith(List.of(
                    signal("emociones", "HIGH"),
                    signal("comunicacion", "HIGH"),
                    signal("habitos", "HIGH")
            )));

            service.checkAndAlert(FAM_ID);

            verify(whatsAppService, times(1)).sendToFamily(any(), any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("checkAndAlert() — sin WhatsApp configurado")
    class WithHighSignalsNoWhatsApp {

        @Test
        @DisplayName("devuelve false cuando la familia no tiene WhatsApp configurado")
        void returnsFalseWithNoWhatsApp() {
            family.setWhatsapp(null);
            stubFamilyFound(); when(radarService.analyze(FAM_ID)).thenReturn(radarWith(List.of(
                    signal("emociones", "HIGH")
            )));

            boolean result = service.checkAndAlert(FAM_ID);

            assertThat(result).isFalse();
            verifyNoInteractions(whatsAppService);
        }

        @Test
        @DisplayName("devuelve false cuando el WhatsApp está en blanco")
        void returnsFalseWithBlankWhatsApp() {
            family.setWhatsapp("   ");
            stubFamilyFound(); when(radarService.analyze(FAM_ID)).thenReturn(radarWith(List.of(
                    signal("tiempos", "HIGH")
            )));

            boolean result = service.checkAndAlert(FAM_ID);

            assertThat(result).isFalse();
            verifyNoInteractions(whatsAppService);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("checkAndAlert() — manejo de errores en WhatsApp")
    class WhatsAppErrors {

        @Test
        @DisplayName("devuelve false y no propaga excepción cuando WhatsApp falla")
        void returnsFalseWhenWhatsAppThrows() {
            stubFamilyFound(); when(radarService.analyze(FAM_ID)).thenReturn(radarWith(List.of(
                    signal("emociones", "HIGH")
            )));
            doThrow(new RuntimeException("Timeout de red"))
                    .when(whatsAppService).sendToFamily(any(), any());

            boolean result = service.checkAndAlert(FAM_ID);

            assertThat(result).isFalse();
        }
    }
}
