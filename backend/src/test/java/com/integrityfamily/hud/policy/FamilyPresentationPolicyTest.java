package com.integrityfamily.hud.policy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FamilyPresentationPolicy — Unit Tests")
class FamilyPresentationPolicyTest {

    @Test
    @DisplayName("CRITICO y ALTO devuelven el mensaje de riesgo elevado")
    void criticoYAltoDevuelvenMensajeDeRiesgoElevado() {
        String critico = FamilyPresentationPolicy.formatRisk("CRITICO");
        String alto = FamilyPresentationPolicy.formatRisk("ALTO");

        assertThat(critico).isEqualTo(alto);
        assertThat(critico).contains("pedir apoyo");
    }

    @Test
    @DisplayName("MEDIO y MODERADO devuelven el mensaje de oportunidad de comunicacion")
    void medioYModeradoDevuelvenMensajeDeOportunidad() {
        String medio = FamilyPresentationPolicy.formatRisk("MEDIO");
        String moderado = FamilyPresentationPolicy.formatRisk("MODERADO");

        assertThat(medio).isEqualTo(moderado);
        assertThat(medio).contains("fortalecer la comunicación");
    }

    @Test
    @DisplayName("BAJO devuelve el mensaje de ambiente estable")
    void bajoDevuelveMensajeDeAmbienteEstable() {
        assertThat(FamilyPresentationPolicy.formatRisk("BAJO")).contains("estable y protector");
    }

    @Test
    @DisplayName("valores en ingles (HIGH/MEDIUM) ya no coinciden — regresion de ADR-002")
    void valoresEnInglesNoDisparanElMensajeDeRiesgoElevado() {
        assertThat(FamilyPresentationPolicy.formatRisk("HIGH")).contains("estable y protector");
        assertThat(FamilyPresentationPolicy.formatRisk("MEDIUM")).contains("estable y protector");
    }
}
