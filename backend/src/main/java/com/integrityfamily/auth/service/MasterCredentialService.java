package com.integrityfamily.auth.service;

import com.integrityfamily.domain.User;
import com.integrityfamily.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MasterCredentialService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void provisionMasterAdmin() {
        log.info("Ã°Å¸â€â€˜ [SECURITY] Iniciando provisiÃƒÂ³n de Credenciales Maestras...");

        // 2. Crear usuario William si no existe
        String adminEmail = "william@integrity.ia";
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User william = User.builder()
                    .fullName("William | Arquitecto de Integridad")
                    .email(adminEmail)
                    .passwordHash(passwordEncoder.encode("MasterArch2026!")) // PWD Maestra Temporal
                    .roles(java.util.List.of(com.integrityfamily.domain.Role.builder().name("ADMIN").build()))
                    .enabled(true)
                    .build();
            
            userRepository.save(william);
            log.info("Ã°Å¸ÂÂ [MASTER-ADMIN] Cuenta de William creada exitosamente.");
        } else {
            log.info("Ã¢â€žÂ¹Ã¯Â¸Â [MASTER-ADMIN] La cuenta de William ya existe.");
        }
    }
}


