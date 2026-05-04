package com.integrityfamily.common.initializer;

import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SDD: Inicializador de Datos Maestro Unificado (v4.3).
 * Soporta el Plan Híbrido mediante el sembrado de Hitos Temporales.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MasterDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final QuestionRepository questionRepository;
    private final MilestoneRepository milestoneRepository; // Inyección de hitos
    private final FamilyRepository familyRepository;
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

        // 5. Asegurar Familia de Prueba para el Nodo Armenia
        Family defaultFamily = ensureDefaultFamily();
        
        // 2. Sincronizar Usuarios Administrativos (Vinculado a Familia)
        syncAdminUser("william@integrity.family", "William Lopez", "admin123", adminRole, defaultFamily);

        // 3. Sembrar Banco de Preguntas
        seedQuestions();

        // 4. Sembrar Línea de Tiempo (Hitos)
        seedMilestones();

        log.info(">>>> [SYSTEM] Sincronización completada satisfactoriamente.");
    }

    private Family ensureDefaultFamily() {
        Family family = familyRepository.findByName("Familia López Rivera")
                .orElseGet(() -> {
                    log.info(">>>> [SEEDER] Creando familia base: Familia López Rivera");
                    return familyRepository.save(Family.builder()
                            .name("Familia López Rivera")
                            .description("Nodo de prueba inicial para el sistema de integridad.")
                            .familyCode("FAM-001")
                            .pin("1234")
                            .currentMilestone("W1")
                            .sentinelActive(true)
                            .municipio("Armenia")
                            .build());
                });
        log.info(">>>> [SYSTEM] Familia Base Identificada: {} (ID: {})", family.getName(), family.getId());
        return family;
    }

    private void seedMilestones() {
        if (milestoneRepository.count() > 0) return;

        log.info(">>>> [SEEDER] Poblando línea de tiempo de hitos (W1 -> M36)...");
        
        saveMilestone("W1", "Estabilización", 7, 1);
        saveMilestone("M1", "Conciencia Inicial", 30, 2);
        saveMilestone("M3", "Cimentación de Vínculos", 90, 3);
        saveMilestone("M6", "Transformación Profunda", 180, 4);
        saveMilestone("M9", "Consolidación de Hábitos", 270, 5);
        saveMilestone("M12", "Integridad Plena", 365, 6);
        saveMilestone("M18", "Crecimiento Generacional", 540, 7);
        saveMilestone("M24", "Legado Familiar", 730, 8);
        saveMilestone("M30", "Trascendencia", 910, 9);
        saveMilestone("M36", "Plenitud Total", 1095, 10);
    }

    private void saveMilestone(String code, String label, int days, int order) {
        milestoneRepository.save(Milestone.builder()
                .code(code)
                .label(label)
                .durationDays(days)
                .orderIndex(order) // Corregido a camelCase
                .build());
    }

    private void seedQuestions() {
        if (questionRepository.count() > 0) return;

        log.info(">>>> [SEEDER] Poblando banco de preguntas fundamental...");
        
        saveQuestion("RECONOCIMIENTO", 1, "¿Qué tan conscientes somos de las necesidades emocionales de cada integrante sin que tengan que pedirlas?");
        saveQuestion("RECONOCIMIENTO", 2, "¿Con qué frecuencia celebramos los logros individuales como si fueran victorias de todo el equipo familiar?");
        saveQuestion("AMOR", 1, "¿Nuestra comunicación en momentos de estrés mantiene el respeto y la ternura como prioridad?");
        saveQuestion("AMOR", 2, "¿Qué tan seguros se sienten los integrantes para expresar vulnerabilidad sin temor a ser juzgados?");
        saveQuestion("COMPROMISO", 1, "¿Las responsabilidades del hogar se asumen como una contribución al bienestar común o como una carga impuesta?");
        saveQuestion("COMPROMISO", 2, "¿Existe claridad en los acuerdos de convivencia y consecuencias naturales cuando estos no se cumplen?");
    }

    private void saveQuestion(String dimension, int vertice, String text) {
        Question q = new Question();
        q.setDimension(dimension);
        q.setVertice(vertice);
        q.setText(text);
        q.setActive(true);
        questionRepository.save(q);
    }

    private Role ensureRole(String name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> {
                    return roleRepository.save(Role.builder().name(name).build());
                });
    }

    private void syncAdminUser(String email, String fullName, String rawPassword, Role role, Family family) {
        userRepository.findByEmailIgnoreCase(email).ifPresentOrElse(
            user -> {
                user.setPasswordHash(passwordEncoder.encode(rawPassword));
                user.setFullName(fullName);
                user.setEnabled(true);
                user.setFamily(family);
                if (user.getRoles() == null) user.setRoles(new java.util.ArrayList<>());
                if (!user.getRoles().contains(role)) user.getRoles().add(role);
                userRepository.save(user);
            },
            () -> {
                User newUser = User.builder()
                        .email(email)
                        .fullName(fullName)
                        .passwordHash(passwordEncoder.encode(rawPassword))
                        .enabled(true)
                        .family(family)
                        .roles(new java.util.ArrayList<>(java.util.List.of(role)))
                        .build();
                userRepository.save(newUser);
            }
        );
    }
}
