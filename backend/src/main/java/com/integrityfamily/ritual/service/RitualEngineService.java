package com.integrityfamily.ritual.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.ImprovementPlan;
import com.integrityfamily.domain.PlanTask;
import com.integrityfamily.domain.repository.*;
import com.integrityfamily.ritual.domain.FamilyRitual;
import com.integrityfamily.ritual.domain.RitualStatus;
import com.integrityfamily.ritual.domain.RitualType;
import com.integrityfamily.ritual.dto.RitualDto;
import com.integrityfamily.ritual.repository.FamilyRitualRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RitualEngineService {

    private final FamilyRitualRepository ritualRepository;
    private final FamilyRepository familyRepository;
    private final ImprovementPlanRepository planRepository;
    private final TaskEvidenceRepository evidenceRepository;
    private final FamilyLogbookRepository logbookRepository;
    private final FamilyGratitudeEntryRepository gratitudeRepository;
    private final AiProvider aiProvider;
    private final ObjectMapper objectMapper;

    // ─── Consultas para el frontend ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RitualDto> getActiveRituals(Long familyId) {
        return ritualRepository
                .findByFamilyIdAndStatusOrderByTriggeredAtDesc(familyId, RitualStatus.PENDING)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RitualDto> getHistory(Long familyId) {
        return ritualRepository
                .findByFamilyIdOrderByTriggeredAtDesc(familyId)
                .stream()
                .limit(30)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public RitualDto activate(Long ritualId) {
        FamilyRitual ritual = ritualRepository.findById(ritualId)
                .orElseThrow(() -> new IllegalArgumentException("Ritual no encontrado: " + ritualId));
        ritual.setStatus(RitualStatus.ACTIVE);
        ritual.setActivatedAt(LocalDateTime.now());
        return toDto(ritualRepository.save(ritual));
    }

    @Transactional
    public RitualDto complete(Long ritualId) {
        FamilyRitual ritual = ritualRepository.findById(ritualId)
                .orElseThrow(() -> new IllegalArgumentException("Ritual no encontrado: " + ritualId));
        ritual.setStatus(RitualStatus.COMPLETED);
        ritual.setCompletedAt(LocalDateTime.now());
        return toDto(ritualRepository.save(ritual));
    }

    @Transactional
    public void dismiss(Long ritualId) {
        FamilyRitual ritual = ritualRepository.findById(ritualId)
                .orElseThrow(() -> new IllegalArgumentException("Ritual no encontrado: " + ritualId));
        ritual.setStatus(RitualStatus.DISMISSED);
        ritual.setDismissedAt(LocalDateTime.now());
        ritualRepository.save(ritual);
    }

    // ─── Motor de detección (llamado por el scheduler) ────────────────────────

    @Transactional
    public void detectAndCreateRituals() {
        List<Family> families = familyRepository.findAll();
        log.info("[RITUAL] Detectando rituales para {} familias", families.size());

        for (Family family : families) {
            try {
                detectForFamily(family);
            } catch (Exception e) {
                log.warn("[RITUAL] Error procesando familia {}: {}", family.getId(), e.getMessage());
            }
        }
    }

    private void detectForFamily(Family family) {
        Long fid = family.getId();
        LocalDate today = LocalDate.now();

        checkBirthdays(family, today);
        checkSundayRitual(fid, today);
        checkAnniversary(family, today);
        checkMissionAchievement(fid);
        checkInactivity(fid);
        checkMonthEnd(fid, today);
        checkPositiveStreak(fid);
    }

    // ─── Detectores individuales ──────────────────────────────────────────────

    private void checkBirthdays(Family family, LocalDate today) {
        for (FamilyMember m : family.getMembers()) {
            if (!m.isActive()) continue;
            if (m.getBirthDate() == null) continue;

            LocalDate bd = m.getBirthDate();
            if (bd.getMonthValue() == today.getMonthValue()
                    && bd.getDayOfMonth() == today.getDayOfMonth()) {

                if (alreadyCreatedToday(family.getId(), RitualType.CUMPLEANOS)) continue;

                String context = "Hoy es el cumpleaños de " + m.getFullName();
                createRitual(family.getId(), RitualType.CUMPLEANOS,
                        "🎂 Celebración de " + m.getFullName(),
                        "Hoy es un día especial para honrar a " + m.getFullName() + ". La familia tiene una oportunidad única de hacer que se sienta profundamente amado.",
                        context,
                        buildBirthdaySteps(m.getFullName())
                );
            }
        }
    }

    private void checkSundayRitual(Long familyId, LocalDate today) {
        if (today.getDayOfWeek().getValue() != 7) return; // 7 = domingo
        if (alreadyCreatedThisWeek(familyId, RitualType.DOMINGO_FAMILIAR)) return;

        createRitual(familyId, RitualType.DOMINGO_FAMILIAR,
                "🌅 Ritual Familiar del Domingo",
                "El domingo es el momento perfecto para hacer una pausa y reconectar como familia.",
                "Domingo — inicio de semana familiar",
                buildSundaySteps()
        );
    }

    private void checkAnniversary(Family family, LocalDate today) {
        if (family.getCreatedAt() == null) return;
        LocalDate created = family.getCreatedAt().toLocalDate();
        if (created.getMonthValue() != today.getMonthValue()
                || created.getDayOfMonth() != today.getDayOfMonth()) return;

        long years = ChronoUnit.YEARS.between(created, today);
        if (years == 0) return;

        if (alreadyCreatedToday(family.getId(), RitualType.ANIVERSARIO)) return;

        createRitual(family.getId(), RitualType.ANIVERSARIO,
                "🎊 " + years + " año" + (years == 1 ? "" : "s") + " de evolución familiar",
                "Hoy se cumple " + years + " año" + (years == 1 ? "" : "s") + " desde que esta familia comenzó su camino de transformación en Integrity Family.",
                years + " aniversario de la familia",
                buildAnniversarySteps(years)
        );
    }

    private void checkMissionAchievement(Long familyId) {
        List<ImprovementPlan> plans = planRepository.findByFamilyId(familyId);
        if (plans.isEmpty()) return;

        ImprovementPlan latest = plans.get(plans.size() - 1);
        List<PlanTask> tasks = latest.getTasks();
        if (tasks.isEmpty()) return;

        long total = tasks.size();
        long done  = tasks.stream().filter(PlanTask::isCompleted).count();
        double rate = (double) done / total * 100.0;

        if (rate >= 80.0 && !alreadyCreatedThisMonth(familyId, RitualType.LOGRO_CELEBRADO)) {
            createRitual(familyId, RitualType.LOGRO_CELEBRADO,
                    "🏆 ¡" + (int) rate + "% de misiones completadas!",
                    "Esta familia ha completado el " + (int) rate + "% de sus misiones. Ese nivel de compromiso merece ser celebrado.",
                    "Tasa de cumplimiento: " + (int) rate + "%",
                    buildAchievementSteps((int) rate)
            );
        }
    }

    private void checkInactivity(Long familyId) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(14);

        boolean hasRecentEvidence = evidenceRepository.findByFamilyId(familyId).stream()
                .anyMatch(e -> e.getCreatedAt() != null && e.getCreatedAt().isAfter(threshold));
        boolean hasRecentLogbook = logbookRepository.findByFamilyId(familyId).stream()
                .anyMatch(l -> l.getCreatedAt() != null && l.getCreatedAt().isAfter(threshold));
        boolean hasRecentGratitude = gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(familyId).stream()
                .anyMatch(g -> g.getCreatedAt() != null && g.getCreatedAt().isAfter(threshold));

        if (!hasRecentEvidence && !hasRecentLogbook && !hasRecentGratitude) {
            if (alreadyCreatedThisMonth(familyId, RitualType.SIN_ACTIVIDAD)) return;
            createRitual(familyId, RitualType.SIN_ACTIVIDAD,
                    "🌱 ¿Cómo están? Hace 14 días sin actividad",
                    "La familia no ha registrado actividad en los últimos 14 días. Un pequeño ritual de reconexión puede reencender el camino.",
                    "14+ días sin actividad registrada",
                    buildReconnectSteps()
            );
        }
    }

    private void checkMonthEnd(Long familyId, LocalDate today) {
        // Último día del mes
        if (today.getDayOfMonth() != today.lengthOfMonth()) return;
        if (alreadyCreatedToday(familyId, RitualType.FIN_DE_MES)) return;

        createRitual(familyId, RitualType.FIN_DE_MES,
                "🌙 Ritual de Cierre de " + today.getMonth().getDisplayName(java.time.format.TextStyle.FULL, new Locale("es")),
                "Último día del mes: el momento ideal para celebrar los avances, reconocer los aprendizajes y preparar el corazón para el mes siguiente.",
                "Fin de " + today.getMonth().getDisplayName(java.time.format.TextStyle.FULL, new Locale("es")),
                buildMonthEndSteps()
        );
    }

    private void checkPositiveStreak(Long familyId) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        long recentEvents = 0;
        recentEvents += evidenceRepository.findByFamilyId(familyId).stream()
                .filter(e -> e.getCreatedAt() != null && e.getCreatedAt().isAfter(sevenDaysAgo)).count();
        recentEvents += gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(familyId).stream()
                .filter(g -> g.getCreatedAt() != null && g.getCreatedAt().isAfter(sevenDaysAgo)).count();

        if (recentEvents >= 7 && !alreadyCreatedThisWeek(familyId, RitualType.RACHA_POSITIVA)) {
            createRitual(familyId, RitualType.RACHA_POSITIVA,
                    "🔥 ¡7 días de racha positiva!",
                    "Esta familia lleva una semana de presencia activa y constante. Eso no es un accidente — es el resultado de una decisión diaria de crecer juntos.",
                    "7 eventos registrados en los últimos 7 días",
                    buildStreakSteps()
            );
        }
    }

    // ─── Creación de rituales con pasos guiados por IA ────────────────────────

    private void createRitual(Long familyId, RitualType type, String title,
                               String description, String context, List<String> defaultSteps) {
        // Intenta enriquecer los pasos con IA; si falla, usa los pasos por defecto
        List<String> steps = enrichStepsWithAi(type, title, context, defaultSteps);

        FamilyRitual ritual = FamilyRitual.builder()
                .familyId(familyId)
                .ritualType(type)
                .status(RitualStatus.PENDING)
                .title(title)
                .description(description)
                .guidedSteps(toJson(steps))
                .triggerContext(context)
                .triggeredAt(LocalDateTime.now())
                .build();

        ritualRepository.save(ritual);
        log.info("[RITUAL] Creado: {} para familia {}", type, familyId);
    }

    private List<String> enrichStepsWithAi(RitualType type, String title, String context, List<String> defaults) {
        try {
            String prompt = """
                    Eres el motor de rituales de Integrity Family.
                    Genera exactamente 4 pasos guiados para este ritual familiar.
                    Cada paso debe ser una acción concreta, cálida y de baja fricción (máximo 15 palabras).

                    Ritual: %s
                    Contexto: %s

                    Responde SOLO con un JSON array de 4 strings. Sin texto adicional.
                    Ejemplo: ["Paso 1", "Paso 2", "Paso 3", "Paso 4"]
                    """.formatted(title, context);

            String raw = aiProvider.generateRawResponse(prompt);
            String cleaned = raw.strip();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```json\\s*", "").replaceAll("^```\\s*", "").replaceAll("\\s*```$", "").strip();
            }
            int start = cleaned.indexOf('[');
            int end   = cleaned.lastIndexOf(']');
            if (start >= 0 && end > start) {
                cleaned = cleaned.substring(start, end + 1);
            }
            List<String> result = objectMapper.readValue(cleaned, new TypeReference<>() {});
            return result.isEmpty() ? defaults : result;
        } catch (Exception e) {
            log.warn("[RITUAL] IA no disponible para enriquecer pasos: {}", e.getMessage());
            return defaults;
        }
    }

    // ─── Pasos por defecto por tipo de ritual ────────────────────────────────

    private List<String> buildBirthdaySteps(String name) {
        return List.of(
                "Reuníos en un círculo y digan una cualidad que admiran de " + name,
                "Cada miembro escribe o dice: «Lo que más me alegra de tenerte es...»",
                "Hagan una foto juntos para la historia familiar",
                "El guardián registra este momento como cápsula en la plataforma"
        );
    }

    private List<String> buildSundaySteps() {
        return List.of(
                "Reúnanse sin teléfonos durante 20 minutos",
                "Cada miembro comparte: ¿cuál fue tu momento favorito de esta semana?",
                "Definan una intención compartida para la semana que comienza",
                "Cierren con un abrazo colectivo o gesto familiar"
        );
    }

    private List<String> buildAnniversarySteps(long years) {
        return List.of(
                "Revisen juntos las evidencias y recuerdos del último año en Integrity Family",
                "Cada miembro comparte: ¿qué ha cambiado en mí gracias a esta familia?",
                "El guardián lee la constitución familiar en voz alta",
                "Registren una carta al futuro para ser abierta en " + (years + 1) + " años"
        );
    }

    private List<String> buildAchievementSteps(int rate) {
        return List.of(
                "Anuncien el logro en voz alta: «Completamos el " + rate + "% de nuestras misiones»",
                "Cada miembro nombra a quien más le inspiró durante este período",
                "Celebren con algo sencillo que les guste hacer juntos",
                "Suban una evidencia de la celebración como memoria de este hito"
        );
    }

    private List<String> buildReconnectSteps() {
        return List.of(
                "Reúnanse durante 10 minutos y pregunten: ¿cómo está cada uno?",
                "Elijan UNA misión pequeña para completar esta semana",
                "El guardián registra una gratitud en nombre de la familia",
                "Acuerden un día y hora para su próxima actividad familiar"
        );
    }

    private List<String> buildMonthEndSteps() {
        return List.of(
                "Cada miembro comparte: ¿cuál fue tu aprendizaje más valioso del mes?",
                "Celebren un logro compartido, por pequeño que sea",
                "Elijan una palabra que defina el mes que termina",
                "Definan una palabra que quieran vivir el mes siguiente"
        );
    }

    private List<String> buildStreakSteps() {
        return List.of(
                "Anuncien la racha: «7 días activos como familia»",
                "Cada miembro menciona qué hábito nuevo está sintiendo",
                "Tomen una foto espontánea del momento presente",
                "El guardián activa el daily de hoy como celebración"
        );
    }

    // ─── Utilidades ───────────────────────────────────────────────────────────

    private boolean alreadyCreatedToday(Long familyId, RitualType type) {
        return ritualRepository.existsByFamilyIdAndRitualTypeAndTriggeredAtAfter(
                familyId, type, LocalDateTime.now().toLocalDate().atStartOfDay());
    }

    private boolean alreadyCreatedThisWeek(Long familyId, RitualType type) {
        return ritualRepository.existsByFamilyIdAndRitualTypeAndTriggeredAtAfter(
                familyId, type, LocalDateTime.now().minusDays(7));
    }

    private boolean alreadyCreatedThisMonth(Long familyId, RitualType type) {
        return ritualRepository.existsByFamilyIdAndRitualTypeAndTriggeredAtAfter(
                familyId, type, LocalDateTime.now().minusDays(30));
    }

    private String toJson(List<String> list) {
        try { return objectMapper.writeValueAsString(list); }
        catch (Exception e) { return "[]"; }
    }

    @SuppressWarnings("unchecked")
    private List<String> parseSteps(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (Exception e) { return Collections.emptyList(); }
    }

    // ─── Context block para ContextSynthesizer ────────────────────────────────

    @Transactional(readOnly = true)
    public String buildRitualContextBlock(Long familyId) {
        try {
            List<FamilyRitual> active = ritualRepository
                    .findByFamilyIdAndStatusOrderByTriggeredAtDesc(familyId, RitualStatus.PENDING);
            if (active.isEmpty()) return null;

            StringBuilder sb = new StringBuilder("Rituales activos:\n");
            for (FamilyRitual r : active) {
                sb.append("  • ").append(r.getTitle());
                if (r.getTriggerContext() != null) sb.append(" — ").append(r.getTriggerContext());
                sb.append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("[RITUAL] No se pudo construir bloque de contexto: {}", e.getMessage());
            return null;
        }
    }

    // ─── Mapper ───────────────────────────────────────────────────────────────

    private RitualDto toDto(FamilyRitual r) {
        return new RitualDto(
                r.getId(), r.getFamilyId(), r.getRitualType(), r.getStatus(),
                r.getTitle(), r.getDescription(), parseSteps(r.getGuidedSteps()),
                r.getTriggerContext(), r.getTriggeredAt(), r.getCompletedAt()
        );
    }
}
