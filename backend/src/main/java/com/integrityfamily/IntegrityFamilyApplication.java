package com.integrityfamily;

import com.integrityfamily.auth.domain.*;
import com.integrityfamily.auth.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Set;
import java.util.HashSet;

@SpringBootApplication
public class IntegrityFamilyApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntegrityFamilyApplication.class, args);
    }

    @Bean
    public CommandLineRunner identitySync(UserRepository ur, RoleRepository rr, PasswordEncoder pe) {
        return args -> {
            // 1. Asegurar roles
            Role fRole = rr.findByName("ROLE_FAMILY").orElseGet(() -> rr.save(new Role(null, "ROLE_FAMILY")));
            Role uRole = rr.findByName("ROLE_USER").orElseGet(() -> rr.save(new Role(null, "ROLE_USER")));

            // 2. Sincronizar Usuario William
            User william = ur.findByEmail("william@integrity.family").orElse(new User());
            william.setEmail("william@integrity.family");
            william.setFullName("William Lopez");
            william.setPassword(pe.encode("Admin123*"));
            william.setActive(true);
            
            Set<Role> roles = new HashSet<>();
            roles.add(fRole);
            roles.add(uRole);
            william.setRoles(roles);

            ur.save(william);
            System.out.println(">>>> [NODO ARMENIA] William Sincronizado. Entidades listas.");
        };
    }
}