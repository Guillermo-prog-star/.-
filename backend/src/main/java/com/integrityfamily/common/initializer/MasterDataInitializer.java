package com.integrityfamily.common.initializer;

import com.integrityfamily.common.service.QuestionBankLoaderService;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;



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
    private final MilestoneRepository milestoneRepository;
    private final FamilyRepository familyRepository;
    private final PasswordEncoder passwordEncoder;
    private final QuestionBankLoaderService questionBankLoaderService;

    @Override
    @Transactional
    public void run(String... args) {
        log.info(">>>> [SYSTEM] Iniciando Protocolo Multi-Tenant de Sincronización...");

        // 1. Asegurar Roles Base (datos maestros globales, compartidos por todas las familias)
        Role adminRole = ensureRole("ROLE_ADMIN");
        ensureRole("ROLE_USER");
        ensureRole("ROLE_FAMILY_ADMIN");
        ensureRole("ROLE_FAMILY_MEMBER");

        // 2. Asegurar usuario administrador del sistema (sin familia fija — acceso global)
        ensureSystemAdmin("william@integrity.family", "William Lopez", "admin123", adminRole);

        // 3. Sembrar Banco de Preguntas (datos maestros globales)
        seedQuestions();

        // 4. Sembrar Línea de Tiempo de Hitos (datos maestros globales)
        seedMilestones();

        log.info(">>>> [SYSTEM] Sincronización multi-tenant completada. {} familia(s) activa(s).",
                familyRepository.count());
    }

    private void cleanAndMergeDuplicateFamilies() {
        // [MULTI-TENANT] Lógica destructiva eliminada — cada familia es autónoma y sus datos son propios.
    }

    /**
     * [MULTI-TENANT] Asegura que el administrador del sistema exista.
     * El administrador global NO pertenece a ninguna familia específica —
     * FamilySecurityEvaluator le concede acceso de lectura a todos los nodos.
     * Si ya existe con familia asignada, se preserva esa asignación.
     */
    private void ensureSystemAdmin(String email, String fullName, String rawPassword, Role role) {
        userRepository.findByEmailIgnoreCase(email).ifPresentOrElse(
            user -> {
                // Solo actualizar password y rol — NUNCA tocar family_id existente
                user.setPasswordHash(passwordEncoder.encode(rawPassword));
                user.setFullName(fullName);
                user.setEnabled(true);
                if (user.getRoles() == null) user.setRoles(new java.util.ArrayList<>());
                if (user.getRoles().stream().noneMatch(r -> r.getName().equals(role.getName()))) {
                    user.getRoles().add(role);
                }
                userRepository.save(user);
                log.info(">>>> [SYSTEM] Admin del sistema verificado: {} — familia: {}",
                        email, user.getFamily() != null ? user.getFamily().getFamilyCode() : "GLOBAL");
            },
            () -> {
                // Crear admin global sin familia — podrá acceder a todas por su ROLE_ADMIN
                User newAdmin = User.builder()
                        .email(email)
                        .fullName(fullName)
                        .passwordHash(passwordEncoder.encode(rawPassword))
                        .enabled(true)
                        .roles(new java.util.ArrayList<>(java.util.List.of(role)))
                        .build();
                userRepository.save(newAdmin);
                log.info(">>>> [SYSTEM] Admin del sistema creado: {}", email);
            }
        );
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
        long existing = questionRepository.count();
        if (existing >= 1000) {
            log.info(">>>> [SEEDER] Banco de preguntas ya poblado ({} reactivos). Saltando carga.", existing);
            return;
        }

        log.info(">>>> [SEEDER] Banco incompleto ({} reactivos). Iniciando carga del banco maestro v2...", existing);

        // Cargar las 1000 preguntas desde questions-bank-v2.json
        int loaded = questionBankLoaderService.loadAll();

        log.info(">>>> [SEEDER] Banco de preguntas listo. Nuevos reactivos cargados: {}. Total BD: {}",
                loaded, questionRepository.count());
    }

    private Role ensureRole(String name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(Role.builder().name(name).build()));
    }
}
