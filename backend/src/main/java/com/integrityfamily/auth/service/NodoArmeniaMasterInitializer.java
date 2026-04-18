package com.integrityfamily.auth.service;

// VERIFICA ESTA LÍNEA: Debe coincidir con la ubicación real de tu clase User
import com.integrityfamily.auth.domain.User;
import com.integrityfamily.auth.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component("authNodoArmeniaMasterInitializer")
@RequiredArgsConstructor
@Slf4j
public class NodoArmeniaMasterInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info(">>>> [NODO ARMENIA] Verificando usuarios maestros...");

        // Usuario William (propietario del nodo)
        if (userRepository.findByEmail("william@integrity.family").isEmpty()) {
            User master = new User();
            master.setFullName("William Lopez");
            master.setEmail("william@integrity.family");
            master.setPassword(passwordEncoder.encode("Admin123*"));
            master.setActive(true);
            userRepository.save(master);
            log.info(">>>> [NODO ARMENIA] William Lopez sincronizado.");
        }

        // Usuario admin demo (credenciales que muestra el login)
        if (userRepository.findByEmail("admin@integrityfamily.com").isEmpty()) {
            User admin = new User();
            admin.setFullName("Administrador Demo");
            admin.setEmail("admin@integrityfamily.com");
            admin.setPassword(passwordEncoder.encode("Admin123*"));
            admin.setActive(true);
            userRepository.save(admin);
            log.info(">>>> [NODO ARMENIA] Admin demo sincronizado.");
        }
    }
}