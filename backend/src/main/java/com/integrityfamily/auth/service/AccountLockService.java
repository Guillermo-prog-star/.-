package com.integrityfamily.auth.service;

import com.integrityfamily.domain.repository.UserRepository;
import com.integrityfamily.domain.repository.FailedLoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountLockService {

    private final FailedLoginAttemptRepository attemptRepository;
    private final UserRepository userRepository;

    @Transactional
    public void registerFailure(String email) {
        log.warn("[SECURITY] Intento fallido para: {}", email);
        // AquÃƒÂ­ puedes aÃƒÂ±adir la lÃƒÂ³gica de incremento de intentos y bloqueo de cuenta
    }
}


