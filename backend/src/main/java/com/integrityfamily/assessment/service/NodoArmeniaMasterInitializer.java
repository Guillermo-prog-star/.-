package com.integrityfamily.assessment.service;

import com.integrityfamily.auth.domain.User;
import com.integrityfamily.auth.domain.Role;
import com.integrityfamily.auth.repository.UserRepository;
import com.integrityfamily.auth.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Set;

@Component("assessmentNodoArmeniaMasterInitializer")
@RequiredArgsConstructor
@Slf4j // Corrige el error de 'cannot find symbol variable log'
public class NodoArmeniaMasterInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        Role familyRole = roleRepository.findByName("ROLE_FAMILY")
            .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_FAMILY")));
        
        Role userRole = roleRepository.findByName("ROLE_USER")
            .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_USER")));

        User william = userRepository.findByEmail("william@integrity.family")
            .orElse(new User());

        william.setEmail("william@integrity.family");
        william.setFullName("William Lopez");
        william.setPassword(passwordEncoder.encode("Admin123*"));
        william.setActive(true);
        william.setRoles(Set.of(familyRole, userRole));

        userRepository.save(william);
        log.info(">>>> [NODO ARMENIA] William Lopez SINCRONIZADO: FAMILY + USER.");
    }
}