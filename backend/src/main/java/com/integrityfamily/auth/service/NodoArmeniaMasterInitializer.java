package com.integrityfamily.auth.service;

import com.integrityfamily.domain.User;
import com.integrityfamily.domain.Role;
import com.integrityfamily.domain.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Collections;

// @Component("authNodoArmeniaMasterInitializer")
@RequiredArgsConstructor
@Slf4j
public class NodoArmeniaMasterInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // SDD: Lógica movida a MasterDataInitializer para centralización arquitectónica.
        log.debug(">>>> [SYSTEM] Inicializador legacy (Auth) omitido.");
    }
}
