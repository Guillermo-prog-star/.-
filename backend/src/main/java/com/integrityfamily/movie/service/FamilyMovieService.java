package com.integrityfamily.movie.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import com.integrityfamily.movie.domain.FamilyMovie;
import com.integrityfamily.movie.dto.FamilyMovieDto;
import com.integrityfamily.movie.repository.FamilyMovieRepository;
import com.integrityfamily.ritual.domain.FamilyRitual;
import com.integrityfamily.ritual.domain.RitualStatus;
import com.integrityfamily.ritual.repository.FamilyRitualRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FamilyMovieService {

    private final FamilyMovieRepository      movieRepository;
    private final FamilyRepository           familyRepository;
    private final FamilyGratitudeEntryRepository gratitudeRepository;
    private final TaskEvidenceRepository     evidenceRepository;
    private final FamilyLogbookRepository    logbookRepository;
    private final CriticalDayRepository      crisisRepository;
    private final FamilySprintRepository     sprintRepository;
    private final ImprovementPlanRepository  planRepository;
    private final FamilyRitualRepository     ritualRepository;
    private final AiProvider                 aiProvider;
    private final ObjectMapper               objectMapper;

    private static final Locale SPANISH = new Locale("es", "CO");

    // ─── Consultas ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<FamilyMovieDto> listMovies(Long familyId) {
        Family family = familyRepository.findById(familyId).orElseThrow();
        return movieRepository.findByFamilyIdOrderByPeriodStartDesc(familyId)
                .stream().map(m -> toDto(m, family.getName()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<FamilyMovieDto> getLatest(Long familyId) {
        Family family = familyRepository.findById(familyId).orElseThrow();
        return movieRepository.findFirstByFamilyIdOrderByPeriodStartDesc(familyId)
                .map(m -> toDto(m, family.getName()));
    }

    // ─── Generación ───────────────────────────────────────────────────────────

    /**
     * Genera la película del trimestre actual.
     * Si ya existe para este período, devuelve la existente.
     */
    @Transactional
    public FamilyMovieDto generateCurrentQuarter(Long familyId) {
        LocalDate[] quarter = currentQuarterRange();
        return generate(familyId, quarter[0], quarter[1]);
    }

    /**
     * Genera la película para un período personalizado.
     */
    @Transactional
    public FamilyMovieDto generate(Long familyId, LocalDate from, LocalDate to) {
        // Devuelve la existente si ya fue generada para este período
        if (movieRepository.existsByFamilyIdAndPeriodStartAndPeriodEnd(familyId, from, to)) {
            return movieRepository.findByFamilyIdOrderByPeriodStartDesc(familyId)
                    .stream()
                    .filter(m -> m.getPeriodStart().equals(from) && m.getPeriodEnd().equals(to))
                    .findFirst()
                    .map(m -> toDto(m, familyRepository.findById(familyId).map(Family::getName).orElse("")))
                    .orElseThrow();
        }

        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new IllegalArgumentException("Familia no encontrada: " + familyId));

        log.info("[MOVIE] Generando película para familia {} período {} → {}", familyId, from, to);

        // ── Recopila datos del período ───────────────────────────────────────
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end   = to.atTime(23, 59, 59);

        List<FamilyGratitudeEntry> gratitudes = gratitudeRepository
                .findByFamilyIdOrderByCreatedAtDesc(familyId).stream()
                .filter(g -> inRange(g.getCreatedAt(), start, end)).toList();

        List<TaskEvidence> evidences = evidenceRepository.findByFamilyId(familyId).stream()
                .filter(e -> inRange(e.getCreatedAt(), start, end)).toList();

        List<FamilyLogbookEntry> logbooks = logbookRepository.findByFamilyIdOrderByCreatedAtDesc(familyId).stream()
                .filter(l -> inRange(l.getCreatedAt(), start, end)).toList();

        List<CriticalDay> crises = crisisRepository.findByFamilyIdOrderByCreatedAtDesc(familyId).stream()
                .filter(c -> inRange(c.getCreatedAt(), start, end)).toList();

        List<FamilyRitual> ritualsCompleted = ritualRepository
                .findByFamilyIdOrderByTriggeredAtDesc(familyId).stream()
                .filter(r -> RitualStatus.COMPLETED.equals(r.getStatus()))
                .filter(r -> inRange(r.getCompletedAt(), start, end)).toList();

        int missionsCompleted = countCompletedMissions(familyId, start, end);
        int daysActive        = countActiveDays(gratitudes, evidences, logbooks);
        int bestStreak        = computeBestStreak(gratitudes, evidences, logbooks, from, to);
        Double icfStart       = getIcfAtDate(familyId, from);
        Double icfEnd         = getIcfAtDate(familyId, to);
        Double icfDelta       = (icfStart != null && icfEnd != null) ? icfEnd - icfStart : null;

        String periodLabel = buildPeriodLabel(from, to);

        // ── Genera narrativa con Claude ──────────────────────────────────────
        String prompt = buildNarrativePrompt(family, periodLabel, gratitudes, evidences,
                logbooks, crises, ritualsCompleted, missionsCompleted, daysActive,
                bestStreak, icfStart, icfEnd, icfDelta);

        String rawJson = aiProvider.generateRawResponse(prompt);
        Map<String, String> narrative = parseNarrative(rawJson);

        FamilyMovie movie = FamilyMovie.builder()
                .familyId(familyId)
                .periodLabel(periodLabel)
                .periodStart(from)
                .periodEnd(to)
                .evidencesCount(evidences.size())
                .gratitudesCount(gratitudes.size())
                .missionsCompleted(missionsCompleted)
                .crisesCount(crises.size())
                .ritualsCompleted(ritualsCompleted.size())
                .daysActive(daysActive)
                .bestStreak(bestStreak)
                .icfStart(icfStart)
                .icfEnd(icfEnd)
                .icfDelta(icfDelta)
                .openingLine(narrative.getOrDefault("openingLine", ""))
                .chapter1(narrative.getOrDefault("chapter1", ""))
                .chapter2(narrative.getOrDefault("chapter2", ""))
                .chapter3(narrative.getOrDefault("chapter3", ""))
                .mentorLetter(narrative.getOrDefault("mentorLetter", ""))
                .highlightQuote(narrative.getOrDefault("highlightQuote", ""))
                .generatedAt(LocalDateTime.now())
                .generationModel("claude")
                .build();

        movieRepository.save(movie);
        log.info("[MOVIE] Película guardada para familia {} — id={}", familyId, movie.getId());
        return toDto(movie, family.getName());
    }

    // ─── Construcción del prompt narrativo ────────────────────────────────────

    private String buildNarrativePrompt(Family family, String period,
            List<FamilyGratitudeEntry> gratitudes, List<TaskEvidence> evidences,
            List<FamilyLogbookEntry> logbooks, List<CriticalDay> crises,
            List<FamilyRitual> rituals, int missions, int daysActive,
            int bestStreak, Double icfStart, Double icfEnd, Double icfDelta) {

        String gratitudeSamples = gratitudes.stream().limit(5)
                .map(g -> "• " + g.getFromMember() + " -> " + g.getToMember() + ": \"" + truncate(g.getDescription(), 80) + "\"")
                .collect(Collectors.joining("\n"));

        String evidenceSamples = evidences.stream().limit(5)
                .map(e -> "• " + (e.getTitle() != null ? e.getTitle() : e.getEvidenceType().name().toLowerCase())
                        + (e.getEmotion() != null ? " (emoción: " + e.getEmotion() + ")" : ""))
                .collect(Collectors.joining("\n"));

        String crisisSamples = crises.stream().limit(3)
                .map(c -> "• " + c.getCategory() + ": " + truncate(c.getDescription(), 80))
                .collect(Collectors.joining("\n"));

        String ritualSamples = rituals.stream().limit(4)
                .map(r -> "• " + r.getTitle())
                .collect(Collectors.joining("\n"));

        String icfText = (icfStart != null && icfEnd != null)
                ? String.format("ICF inicio: %.1f → ICF fin: %.1f (delta: %+.1f)", icfStart, icfEnd, icfDelta != null ? icfDelta : 0.0)
                : "Sin datos ICF para este período";

        return """
            Eres el Narrador de Integrity Family. Tu tarea es crear la Película Familiar
            de esta familia para el período indicado — como si fuera un Spotify Wrapped
            pero familiar, profundo y emocionalmente resonante.

            FAMILIA: %s
            PERÍODO: %s
            ESTADÍSTICAS:
              Evidencias registradas: %d
              Gratitudes enviadas: %d
              Misiones completadas: %d
              Crisis registradas: %d
              Rituales vividos: %d
              Días activos: %d
              Mejor racha: %d días seguidos
              %s

            GRATITUDES DESTACADAS:
            %s

            EVIDENCIAS DESTACADAS:
            %s

            %s

            RITUALES VIVIDOS:
            %s

            Genera el siguiente JSON exacto (sin texto adicional fuera del JSON):
            {
              "openingLine": "Una sola frase cinematográfica que capture la esencia de este período para esta familia. Poética, específica, memorable. Máx 25 palabras.",
              "chapter1": "Los momentos que los conectaron. 2-3 frases cálidas sobre las gratitudes y evidencias. Menciona detalles reales del período.",
              "chapter2": "Los desafíos que enfrentaron. 2-3 frases sobre las crisis o dificultades superadas (si no hubo crisis, habla del esfuerzo sostenido). Nunca dramatices sin datos.",
              "chapter3": "Lo que construyeron juntos. 2-3 frases sobre misiones completadas, rituales vividos y el crecimiento del ICF. Concluye con esperanza.",
              "mentorLetter": "Carta personal del Mentor a la familia. 4-5 oraciones. Tono cálido, específico, que reconozca el esfuerzo real. Usa 'ustedes' y 'su familia'. Cita al menos un dato real del período.",
              "highlightQuote": "La frase más memorable que podría definir este período. Puede ser una reflexión, un aprendizaje o una celebración. Máx 20 palabras."
            }
            """.formatted(
                family.getName(), period,
                evidences.size(), gratitudes.size(), missions,
                crises.size(), rituals.size(), daysActive, bestStreak,
                icfText,
                gratitudeSamples.isEmpty() ? "Sin gratitudes registradas este período." : gratitudeSamples,
                evidenceSamples.isEmpty()  ? "Sin evidencias registradas este período." : evidenceSamples,
                crises.isEmpty() ? "" : "CRISIS/DESAFÍOS:\n" + crisisSamples,
                ritualSamples.isEmpty() ? "Sin rituales completados este período." : ritualSamples
        );
    }

    // ─── Utilidades de cómputo ────────────────────────────────────────────────

    private int countCompletedMissions(Long familyId, LocalDateTime start, LocalDateTime end) {
        return planRepository.findByFamilyId(familyId).stream()
                .flatMap(p -> p.getTasks().stream())
                .filter(PlanTask::isCompleted)
                .mapToInt(t -> 1)
                .sum()
                + sprintRepository.findByFamilyIdOrderByCreatedAtDesc(familyId).stream()
                .flatMap(s -> s.getMissions().stream())
                .filter(m -> "COMPLETED".equals(m.getStatus())
                          && m.getCompletedAt() != null
                          && inRange(m.getCompletedAt(), start, end))
                .mapToInt(m -> 1).sum();
    }

    private int countActiveDays(List<FamilyGratitudeEntry> g, List<TaskEvidence> e, List<FamilyLogbookEntry> l) {
        Set<LocalDate> days = new HashSet<>();
        g.forEach(x -> { if (x.getCreatedAt() != null) days.add(x.getCreatedAt().toLocalDate()); });
        e.forEach(x -> { if (x.getCreatedAt() != null) days.add(x.getCreatedAt().toLocalDate()); });
        l.forEach(x -> { if (x.getCreatedAt() != null) days.add(x.getCreatedAt().toLocalDate()); });
        return days.size();
    }

    private int computeBestStreak(List<FamilyGratitudeEntry> g, List<TaskEvidence> e,
                                   List<FamilyLogbookEntry> l, LocalDate from, LocalDate to) {
        Set<LocalDate> active = new HashSet<>();
        g.forEach(x -> { if (x.getCreatedAt() != null) active.add(x.getCreatedAt().toLocalDate()); });
        e.forEach(x -> { if (x.getCreatedAt() != null) active.add(x.getCreatedAt().toLocalDate()); });
        l.forEach(x -> { if (x.getCreatedAt() != null) active.add(x.getCreatedAt().toLocalDate()); });

        int best = 0, current = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            if (active.contains(d)) { current++; best = Math.max(best, current); }
            else current = 0;
        }
        return best;
    }

    private Double getIcfAtDate(Long familyId, LocalDate date) {
        // Simplificado: ICF del snapshot más cercano a la fecha
        return null; // LTS no tiene historial por fecha — se podría enriquecer con RiskSnapshot
    }

    private boolean inRange(LocalDateTime dt, LocalDateTime start, LocalDateTime end) {
        return dt != null && !dt.isBefore(start) && !dt.isAfter(end);
    }

    private String buildPeriodLabel(LocalDate from, LocalDate to) {
        if (from.getMonth() == to.getMonth()) {
            return from.getMonth().getDisplayName(TextStyle.FULL, SPANISH) + " " + from.getYear();
        }
        return from.getMonth().getDisplayName(TextStyle.SHORT, SPANISH) + " – " +
               to.getMonth().getDisplayName(TextStyle.FULL, SPANISH) + " " + to.getYear();
    }

    private LocalDate[] currentQuarterRange() {
        LocalDate now = LocalDate.now();
        int q = (now.getMonthValue() - 1) / 3;
        LocalDate start = LocalDate.of(now.getYear(), q * 3 + 1, 1);
        LocalDate end   = start.plusMonths(3).minusDays(1);
        return new LocalDate[]{ start, end };
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseNarrative(String raw) {
        try {
            String cleaned = raw.strip();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```json\\s*", "").replaceAll("^```\\s*", "").replaceAll("\\s*```$", "").strip();
            }
            int s = cleaned.indexOf('{'), e = cleaned.lastIndexOf('}');
            if (s >= 0 && e > s) cleaned = cleaned.substring(s, e + 1);
            return objectMapper.readValue(cleaned, new TypeReference<>() {});
        } catch (Exception ex) {
            log.error("[MOVIE] Error parseando narrativa IA: {}", ex.getMessage());
            return Map.of(
                "openingLine",    "Un período que merece ser recordado.",
                "chapter1",       "Esta familia registró momentos significativos.",
                "chapter2",       "Enfrentaron desafíos con valentía.",
                "chapter3",       "Lo que construyeron juntos perdura.",
                "mentorLetter",   "Gracias por el esfuerzo de cada día.",
                "highlightQuote", "Cada momento juntos construye legado."
            );
        }
    }

    // ─── Mapper ───────────────────────────────────────────────────────────────

    private FamilyMovieDto toDto(FamilyMovie m, String familyName) {
        return new FamilyMovieDto(
                m.getId(), m.getFamilyId(), familyName,
                m.getPeriodLabel(), m.getPeriodStart(), m.getPeriodEnd(),
                m.getEvidencesCount(), m.getGratitudesCount(), m.getMissionsCompleted(),
                m.getCrisesCount(), m.getRitualsCompleted(), m.getDaysActive(), m.getBestStreak(),
                m.getIcfStart(), m.getIcfEnd(), m.getIcfDelta(),
                m.getOpeningLine(), m.getChapter1(), m.getChapter2(),
                m.getChapter3(), m.getMentorLetter(), m.getHighlightQuote(),
                m.getGeneratedAt()
        );
    }
}
