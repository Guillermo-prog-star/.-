package com.integrityfamily.common.initializer;

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
                .orElseGet(() -> roleRepository.save(Role.builder().name(name).build()));
    }
}
