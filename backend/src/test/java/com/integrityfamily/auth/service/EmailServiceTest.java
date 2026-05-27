package com.integrityfamily.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Smoke tests para EmailService.
 * Ambos métodos son stubs de log — el contrato esencial es que no lancen excepciones.
 */
@DisplayName("EmailService")
class EmailServiceTest {

    private final EmailService service = new EmailService();

    @Test
    @DisplayName("sendPasswordResetEmail no lanza excepcion con email y token validos")
    void sendPasswordResetEmail_doesNotThrow() {
        assertThatNoException().isThrownBy(() ->
                service.sendPasswordResetEmail("user@test.com", "abc123token")
        );
    }

    @Test
    @DisplayName("sendPasswordResetEmail no lanza excepcion con token null")
    void sendPasswordResetEmail_nullToken_doesNotThrow() {
        assertThatNoException().isThrownBy(() ->
                service.sendPasswordResetEmail("user@test.com", null)
        );
    }

    @Test
    @DisplayName("sendInvitation no lanza excepcion con parametros validos")
    void sendInvitation_doesNotThrow() {
        assertThatNoException().isThrownBy(() ->
                service.sendInvitation("invite@test.com", "María", "Los García", "FAM-001")
        );
    }
}
