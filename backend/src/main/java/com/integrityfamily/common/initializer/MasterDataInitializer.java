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
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info(">>>> [SYSTEM] Iniciando Protocolo de Sincronización de Datos Maestro...");

        // SDD-Proactive-Self-Healing: Enforce unicidad y resolver colisiones de familias antes de continuar
        cleanAndMergeDuplicateFamilies();

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

    private void cleanAndMergeDuplicateFamilies() {
        log.info(">>>> [SELF-HEALING] Verificando duplicados de la Familia Lopez Rivera...");
        
        // 1. Desactivar temporalmente códigos duplicados en otras familias para evitar ConstraintViolationException
        List<Family> allFamilies = familyRepository.findAll();
        for (Family f : allFamilies) {
            if (f.getId() != null && !f.getId().equals(1L)) {
                boolean updated = false;
                if ("IF-CO-QUI-2026-0004".equals(f.getFamilyCode())) {
                    log.warn("⚠️ [CLEANUP] Detectada familia duplicada con código IF-CO-QUI-2026-0004 en ID: {}. Renombrando código...", f.getId());
                    f.setFamilyCode("DUPLICATE-CODE-" + f.getId() + "-" + System.currentTimeMillis());
                    updated = true;
                }
                if ("Familia Lopez Rivera".equals(f.getName())) {
                    log.warn("⚠️ [CLEANUP] Detectada familia duplicada con nombre Familia Lopez Rivera en ID: {}. Renombrando nombre...", f.getId());
                    f.setName("Duplicada " + f.getId() + " - Familia Lopez Rivera");
                    updated = true;
                }
                if (updated) {
                    familyRepository.saveAndFlush(f);
                }
            }
        }

        // 2. Ahora que las restricciones de clave única están liberadas, buscamos si existe la de ID 1.
        Family targetFamily = familyRepository.findById(1L).orElse(null);
        if (targetFamily == null) {
            log.info(">>>> [SELF-HEALING] No existe la familia base ID 1. Se creará en ensureDefaultFamily.");
            return;
        }

        // 3. Migrar cualquier miembro o usuario de las familias duplicadas renombradas a la familia ID 1
        allFamilies = familyRepository.findAll();
        for (Family f : allFamilies) {
            if (f.getId() != null && !f.getId().equals(1L) && f.getName() != null && f.getName().startsWith("Duplicada ")) {
                log.info("🏥 [SELF-HEALING] Migrando recursos de familia duplicada ID {} a la familia principal ID 1...", f.getId());
                
                // Migrar miembros
                if (f.getMembers() != null && !f.getMembers().isEmpty()) {
                    List<FamilyMember> membersToMigrate = new java.util.ArrayList<>(f.getMembers());
                    f.getMembers().clear();
                    familyRepository.saveAndFlush(f);
                    
                    for (FamilyMember member : membersToMigrate) {
                        member.setFamily(targetFamily);
                        memberRepository.save(member);
                    }
                    log.info("🏥 [SELF-HEALING] Migrados {} miembros.", membersToMigrate.size());
                }
                
                // Migrar usuarios
                List<User> usersToMigrate = userRepository.findAll();
                int userCount = 0;
                for (User user : usersToMigrate) {
                    if (user.getFamily() != null && user.getFamily().getId().equals(f.getId())) {
                        user.setFamily(targetFamily);
                        userRepository.save(user);
                        userCount++;
                    }
                }
                if (userCount > 0) {
                    log.info("🏥 [SELF-HEALING] Migrados {} usuarios.", userCount);
                }

                // No intentar eliminación física para evitar marcar la transacción como rollback-only en caso de restricciones de clave ajena
                log.info("🏥 [SELF-HEALING] Familia duplicada ID {} neutralizada y renombrada con éxito.", f.getId());
            }
        }
    }

    private Family ensureDefaultFamily() {
        Family family = familyRepository.findById(1L).orElse(null);
        if (family == null) {
            List<Family> families = familyRepository.findByName("Familia Lopez Rivera");
            if (!families.isEmpty()) {
                family = families.get(0);
            } else {
                log.info(">>>> [SEEDER] Creando familia base: Familia Lopez Rivera");
                family = familyRepository.save(Family.builder()
                        .name("Familia Lopez Rivera")
                        .description("Nodo de prueba inicial para el sistema de integridad.")
                        .familyCode("IF-CO-QUI-2026-0004")
                        .pin("1234")
                        .currentMilestone("W1")
                        .sentinelActive(true)
                        .municipio("Armenia")
                        .build());
            }
        } else {
            // Self-Healing: Asegurar consistencia absoluta del Nodo Armenia de forma proactiva
            boolean needsUpdate = false;
            if (!"Familia Lopez Rivera".equals(family.getName())) {
                family.setName("Familia Lopez Rivera");
                needsUpdate = true;
            }
            if (!"IF-CO-QUI-2026-0004".equals(family.getFamilyCode())) {
                family.setFamilyCode("IF-CO-QUI-2026-0004");
                needsUpdate = true;
            }
            if (needsUpdate) {
                family = familyRepository.save(family);
                log.info("🏥 [SELF-HEALING] Familia base ID 1 corregida de forma proactiva a: Familia Lopez Rivera (IF-CO-QUI-2026-0004)");
            }
        }
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

        log.info(">>>> [SEEDER] Poblando banco de preguntas adaptativo longitudinal formal...");
        
        // --- 1. CORE Longitudinales ---
        saveExtendedQuestion("Q-CORE-001", "¿Consideran que en su hogar hay espacios seguros para hablar de lo que cada uno siente sin temor a ser criticado?", "comunicacion", "M00", "inconsciencia", "CORE", 0.6, false, false, false, "seguridad emocional", null, null);
        saveExtendedQuestion("Q-CORE-002", "¿Logran resolver las discusiones familiares antes de que escalen a gritos o silencios castigadores?", "emociones", "M00", "reactividad", "CORE", 0.8, true, false, false, "resolucion de conflictos", "gritos, silencios", "conductual");
        saveExtendedQuestion("Q-CORE-003", "¿El cansancio o mal humor de un miembro suele alterar el clima de paz de toda la casa?", "emociones", "M00", "reactividad", "CORE", 0.7, true, false, false, "regulacion emocional", "irritabilidad", null);
        saveExtendedQuestion("Q-CORE-004", "¿Los acuerdos de convivencia se respetan de manera voluntaria y fluida por todos?", "habitos", "M00", "consciencia", "CORE", 0.6, false, false, false, "acuerdos", null, "bitacora");
        saveExtendedQuestion("Q-CORE-005", "¿Encuentran momentos de conexión profunda e individual con cada miembro de la familia?", "tiempos", "M00", "primeros_cambios", "CORE", 0.5, false, false, false, "presencia", null, null);
        saveExtendedQuestion("Q-CORE-006", "¿Se sienten escuchados y valorados en su hogar cuando proponen un cambio?", "comunicacion", "M00", "consciencia", "CORE", 0.6, false, false, false, "escucha activa", null, null);

        // --- 2. ADAPTIVAS por Riesgo ---
        saveExtendedQuestion("Q-ADAP-COM-001", "¿Al discutir, escuchas con la intención de comprender en lugar de preparar tu defensa?", "comunicacion", null, "reactividad", "ADAPTIVE", 0.7, false, false, false, "escucha activa", "defensa", null);
        saveExtendedQuestion("Q-ADAP-COM-002", "¿Utilizan palabras hirientes o reproches del pasado cuando discuten por algo sencillo?", "comunicacion", null, "reactividad", "ADAPTIVE", 0.9, true, false, false, "lenguaje asertivo", "insultos, reproches", "conductual");
        saveExtendedQuestion("Q-ADAP-EMO-001", "¿Eres capaz de notar la tensión en tu cuerpo antes de responder con impaciencia en casa?", "emociones", null, "consciencia", "ADAPTIVE", 0.7, false, false, false, "autoconsciencia", "tension", null);
        saveExtendedQuestion("Q-ADAP-EMO-002", "¿Se habla de la tristeza, miedo o frustración de forma natural y comprensiva en el hogar?", "emociones", null, "consciencia", "ADAPTIVE", 0.6, false, false, false, "expresion emocional", "silencio", null);
        saveExtendedQuestion("Q-ADAP-HAB-001", "¿Las rutinas del hogar se coordinan con calma en lugar de prisa e irritabilidad?", "habitos", null, "primeros_cambios", "ADAPTIVE", 0.6, false, true, false, "rutinas", "prisa", "fotografica");
        saveExtendedQuestion("Q-ADAP-TIE-001", "¿Dedican tiempo de calidad juntos sin distractores tecnológicos como celulares o televisión?", "tiempos", null, "primeros_cambios", "ADAPTIVE", 0.5, false, true, false, "presencia", "celular", "bitacora");

        // --- 3. FASE / PILAR Temporales ---
        saveExtendedQuestion("Q-PIL-W1-001", "¿Han establecido una rutina básica de diálogo para calmar los momentos de tensión emocional?", "comunicacion", "W1", "reactividad", "FASE_PILLAR", 0.6, false, false, false, "contencion", "tension", null);
        saveExtendedQuestion("Q-PIL-M1-001", "¿Comienzan a identificar de manera grupal los patrones repetitivos que inician los desacuerdos?", "emociones", "M1", "consciencia", "FASE_PILLAR", 0.7, false, false, false, "patrones", "discusion", null);
        saveExtendedQuestion("Q-PIL-M3-001", "¿Sienten que los vínculos de confianza se están cimentando sobre bases de respeto cotidiano?", "comunicacion", "M3", "primeros_cambios", "FASE_PILLAR", 0.6, false, false, false, "confianza", null, null);
        saveExtendedQuestion("Q-PIL-M6-001", "¿Han logrado sostener cambios profundos en la convivencia que antes parecían imposibles?", "habitos", "M6", "consolidacion", "FASE_PILLAR", 0.8, false, true, false, "cambio sostenible", null, "conductual");
        saveExtendedQuestion("Q-PIL-M12-001", "¿La autorregulación familiar fluye de manera autónoma sin necesidad de intervención externa?", "emociones", "M12", "plenitud", "FASE_PILLAR", 0.9, false, false, false, "autorregulacion", null, null);

        // --- 4. CONTRASTE / ESPEJO (Detectar simulación) ---
        saveExtendedQuestion("Q-MIR-001", "¿En nuestro hogar nunca se presentan desacuerdos ni discusiones de ningún tipo?", "comunicacion", null, "inconsciencia", "MIRROR", 0.4, false, false, true, "sinceridad", null, null);
        saveExtendedQuestion("Q-MIR-002", "¿Siempre reaccionamos con perfecta paz y amor, sin que nadie pierda nunca la paciencia?", "emociones", null, "inconsciencia", "MIRROR", 0.4, false, false, true, "sinceridad", null, null);

        // --- 5. EXPLORATORIAS IA ---
        saveExtendedQuestion("Q-EXP-001", "¿Sienten que existen tensiones silenciosas o temas tabú de los que nadie se atreve a hablar?", "comunicacion", null, "reactividad", "EXPLORATORY", 0.8, true, false, false, "temas tabu", "tension silenciosa", null);
        saveExtendedQuestion("Q-EXP-002", "¿El uso individual de las pantallas está reemplazando las comidas o charlas compartidas?", "tiempos", null, "reactividad", "EXPLORATORY", 0.7, false, false, false, "distraccion digital", "pantallas", null);
    }

    private void saveExtendedQuestion(
            String key, String text, String dimension, String pillar, 
            String phase, String type, Double severityWeight, 
            boolean detectsRelapse, boolean requiresEvidence, boolean reverseQuestion, 
            String category, String triggers, String evidenceType) {
            
        Question q = Question.builder()
                .questionKey(key)
                .text(text)
                .dimension(dimension)
                .pillar(pillar)
                .phase(phase)
                .type(type)
                .severityWeight(severityWeight)
                .detectsRelapse(detectsRelapse)
                .requiresEvidence(requiresEvidence)
                .reverseQuestion(reverseQuestion)
                .category(category)
                .adaptiveTriggers(triggers)
                .evidenceType(evidenceType)
                .active(true)
                .build();
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

        // Sincronizar también en la tabla de miembros de familia para consistencia absoluta
        memberRepository.findByEmail(email).ifPresent(member -> {
            member.setFamily(family);
            memberRepository.save(member);
        });
    }
}
