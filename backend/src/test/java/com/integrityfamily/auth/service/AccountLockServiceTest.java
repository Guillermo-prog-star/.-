package com.integrityfamily.auth.service;

import com.integrityfamily.domain.repository.FailedLoginAttemptRepository;
import com.integrityfamily.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Smoke test para AccountLockService.
 * registerFailure() es actualmente un stub de log — el contrato esencial es que
 * no lance excepciones y no interactúe con repositorios de forma incorrecta.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountLockService")
class AccountLockServiceTest {

    @Mock FailedLoginAttemptRepository attemptRepository;
    @Mock UserRepository               userRepository;

    @InjectMocks AccountLockService service;

    @Test
    @DisplayName("registerFailure no lanza excepcion con email valido")
    void registerFailure_validEmail_doesNotThrow() {
        assertThatNoException().isThrownBy(() ->
                service.registerFailure("user@test.com")
        );
    }

    @Test
    @DisplayName("registerFailure no lanza excepcion con email null")
    void registerFailure_nullEmail_doesNotThrow() {
        assertThatNoException().isThrownBy(() ->
                service.registerFailure(null)
        );
    }
}
