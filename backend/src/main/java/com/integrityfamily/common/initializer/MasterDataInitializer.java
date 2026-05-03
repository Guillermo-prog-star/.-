package com.integrityfamily.common.initializer;

import com.integrityfamily.domain.Role;
import com.integrityfamily.domain.User;
import com.integrityfamily.domain.repository.RoleRepository;
import com.integrityfamily.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SDD: Inicializador de Datos Maestro Unificado.
 * Centraliza la creación de usuarios administrativos y roles base.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MasterDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info(">>>> [SYSTEM] Iniciando Protocolo de Sincronización de Datos Maestro...");

        // 1. Asegurar Roles Base
        Role adminRole = ensureRole("ROLE_ADMIN");
        ensureRole("ROLE_USER");
        ensureRole("ROLE_FAMILY_ADMIN");
        ensureRole("ROLE_FAMILY_MEMBER");

        // 2. Sincronizar Usuarios Administrativos
        syncAdminUser("william@integrity.family", "William Lopez", "admin123", adminRole);
        syncAdminUser("admin@integrity.family", "Administrador Demo", "Admin123*", adminRole);

        log.info(">>>> [SYSTEM] Sincronización completada satisfactoriamente.");
    }

    private Role ensureRole(String name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> {
                    log.info(">>>> [SYSTEM] Creando rol base: {}", name);
                    return roleRepository.save(Role.builder().name(name).build());
                });
    }

    private void syncAdminUser(String email, String fullName, String rawPassword, Role role) {
        userRepository.findByEmailIgnoreCase(email).ifPresentOrElse(
            user -> {
                log.info(">>>> [SYSTEM] Usuario {} ya existe.", email);
            },
            () -> {
                log.info(">>>> [SYSTEM] Creando usuario maestro: {}", email);
                User newUser = User.builder()
                        .email(email)
                        .fullName(fullName)
                        .passwordHash(passwordEncoder.encode(rawPassword))
                        .enabled(true)
                        .roles(List.of(role))
                        .build();
                userRepository.save(newUser);
            }
        );
    }
}
