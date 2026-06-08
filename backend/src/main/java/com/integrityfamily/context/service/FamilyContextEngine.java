package com.integrityfamily.context.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.context.domain.FamilyContextSnapshot;
import com.integrityfamily.context.dto.FamilyContextDto;
import com.integrityfamily.context.repository.FamilyContextRepository;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import com.integrityfamily.ritual.repository.FamilyRitualRepository;
import com.integrityfamily.ritual.domain.RitualStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FamilyContextEngine {

    private final FamilyContextRepository       contextRepository;
    private final FamilyRepository              familyRepository;
    private final FamilyLongitudinalStateRepository ltsRepository;
    private final FamilyGratitudeEntryRepository    gratitudeRepository;
    private final TaskEvidenceRepository            evidenceRepository;
    private final FamilyLogbookRepository           logbookRepository;
    private final CriticalDayRepository             crisisRepository;
    private final FamilySprintRepository            sprintRepository;
    private final FamilyRitualRepository            ritualRepository;
    private final ObjectMapper objectMapper;

    // El contexto es válido durante 2 horas antes de recomputarse
    private static final int CACHE_HOURS = 2;

    // ─── Consulta pública ─────────────────────────────────────────────────────

    @Transactional
    public FamilyContextDto getContext(Long familyId) {
        return contextRepository.findByFamilyId(familyId)
                .filter(snap -> snap.getComputedAt().isAfter(LocalDateTime.now().minusHours(CACHE_HOURS)))
                .map(snap -> toDto(snap, false))
                .orElseGet(() -> compute(familyId, true));
    }

    @Transactional
    public FamilyContextDto refresh(Long familyId) {
        return compute(familyId, true);
    }

    // ─── Motor de cómputo ─────────────────────────────────────────────────────

    @Transactional
    public FamilyContextDto compute(Long familyId, boolean save) {
        log.info("[CTX] Computando contexto para familia {}", familyId);

        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new IllegalArgumentException("Familia no encontrada: " + familyId));

        var lts       = ltsRepository.findByFamilyId(familyId).orElse(null);
        var gratitudes = gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(familyId);
        var evidences  = evidenceRepository.findByFamilyId(familyId);
        var logbooks   = logbookRepository.findByFamilyIdOrderByCreatedAtDesc(familyId);
        var crises     = crisisRepository.findByFamilyIdOrderByCreatedAtDesc(familyId);
        var activeRituals = ritualRepository.findByFamilyIdAndStatusOrderByTriggeredAtDesc(familyId, RitualStatus.PENDING);

        // ── Señales básicas ──────────────────────────────────────────────────
        int daysWithoutActivity = computeDaysWithoutActivity(gratitudes, evidences, logbooks);
        int streak              = computeStreak(gratitudes, evidences, logbooks);

        String connection    = computeConnectionLevel(gratitudes, evidences, logbooks);
        String stress        = computeStressLevel(lts, crises);
        String communication = computeCommunicationTrend(lts);
        String participation = computeParticipationLevel(family, daysWithoutActivity);
        String trend         = lts != null && lts.getRiskTrend() != null ? mapTrend(lts.getRiskTrend()) : "ESTABLE";
        String mood          = computeMood(stress, connection, trend, streak);

        // ── Sprint progress ──────────────────────────────────────────────────
        Double sprintProgress = computeSprintProgress(familyId);

        // ── Alertas y recomendaciones ────────────────────────────────────────
        List<String> alerts          = buildAlerts(lts, crises, daysWithoutActivity, connection);
        List<String> recommendations = buildRecommendations(connection, stress, communication, streak, activeRituals.size());

        FamilyContextSnapshot snapshot = contextRepository.findByFamilyId(familyId)
                .orElse(FamilyContextSnapshot.builder().familyId(familyId).build());

        snapshot.setConnectionLevel(connection);
        snapshot.setStressLevel(stress);
        snapshot.setCommunicationTrend(communication);
        snapshot.setParticipationLevel(participation);
        snapshot.setOverallTrend(trend);
        snapshot.setOverallMood(mood);
        snapshot.setIcfCurrent(lts != null ? lts.getIcfCurrent() : null);
        snapshot.setRiskLevel(lts != null ? lts.getCurrentRiskLevel() : null);
        snapshot.setDaysWithoutActivity(daysWithoutActivity);
        snapshot.setCurrentStreak(streak);
        snapshot.setActiveRitualsCount(activeRituals.size());
        snapshot.setSprintProgress(sprintProgress);
        snapshot.setAlerts(toJson(alerts));
        snapshot.setRecommendations(toJson(recommendations));
        snapshot.setComputedAt(LocalDateTime.now());

        if (save) contextRepository.save(snapshot);

        return new FamilyContextDto(
                familyId, family.getName(),
                connection, stress, communication, participation, trend, mood,
                snapshot.getIcfCurrent(), snapshot.getRiskLevel(),
                daysWithoutActivity, streak, activeRituals.size(), sprintProgress,
                alerts, recommendations,
                snapshot.getComputedAt(), true
        );
    }

    // ─── Señales individuales ─────────────────────────────────────────────────

    private String computeConnectionLevel(List<FamilyGratitudeEntry> gratitudes,
                                          List<TaskEvidence> evidences,
                                          List<FamilyLogbookEntry> logbooks) {
        LocalDateTime week = LocalDateTime.now().minusDays(7);
        long events = countRecent(gratitudes, g -> g.getCreatedAt(), week)
                    + countRecent(evidences,  e -> e.getCreatedAt(), week)
                    + countRecent(logbooks,   l -> l.getCreatedAt(), week);
        if (events >= 7) return "ALTA";
        if (events >= 3) return "MEDIA";
        return "BAJA";
    }

    private String computeStressLevel(FamilyLongitudinalState lts, List<CriticalDay> crises) {
        if (lts == null) {
            if (!crises.isEmpty() && crises.get(0).getCreatedAt().isAfter(LocalDateTime.now().minusDays(7)))
                return "ALTO";
            return "BAJO";
        }
        if (lts.isInActiveCrisis()) return "CRITICO";
        if (Boolean.TRUE.equals(lts.getCommunicationCollapseActive())) return "ALTO";
        int deteriorations = lts.getConsecutiveDeteriorations() != null ? lts.getConsecutiveDeteriorations() : 0;
        int crises30d      = lts.getCrisisCount30d() != null ? lts.getCrisisCount30d() : 0;
        if (crises30d >= 2 || deteriorations >= 3) return "ALTO";
        if (crises30d >= 1 || deteriorations >= 1) return "MODERADO";
        return "BAJO";
    }

    private String computeCommunicationTrend(FamilyLongitudinalState lts) {
        if (lts == null) return "ESTABLE";
        if (Boolean.TRUE.equals(lts.getCommunicationCollapseActive())) return "DETERIORANDO";
        int improvements   = lts.getConsecutiveImprovements()   != null ? lts.getConsecutiveImprovements()   : 0;
        int deteriorations = lts.getConsecutiveDeteriorations() != null ? lts.getConsecutiveDeteriorations() : 0;
        if (improvements >= 2)   return "MEJORANDO";
        if (deteriorations >= 2) return "DETERIORANDO";
        return "ESTABLE";
    }

    private String computeParticipationLevel(Family family, int daysWithoutActivity) {
        int activeMembers = (int) family.getMembers().stream().filter(FamilyMember::isActive).count();
        if (activeMembers == 0) return "BAJA";
        if (daysWithoutActivity > 14) return "BAJA";
        if (daysWithoutActivity > 7)  return "MEDIA";
        return "ALTA";
    }

    private String computeMood(String stress, String connection, String trend, int streak) {
        if ("CRITICO".equals(stress))    return "EN_CRISIS";
        if ("ALTO".equals(stress))       return "TENSO";
        if (streak >= 7 && "ALTA".equals(connection)) return "CELEBRANDO";
        if ("ASCENDENTE".equals(trend) && "ALTA".equals(connection)) return "CRECIENDO";
        if ("BAJA".equals(connection) && "DESCENDENTE".equals(trend)) return "TENSO";
        return "SERENO";
    }

    private Double computeSprintProgress(Long familyId) {
        try {
            return sprintRepository.findActiveSprintForFamily(familyId)
                    .map(sprint -> {
                        List<SprintMission> missions = sprint.getMissions();
                        if (missions.isEmpty()) return null;
                        long done = missions.stream().filter(m -> "COMPLETED".equals(m.getStatus())).count();
                        return (double) done / missions.size() * 100.0;
                    }).orElse(null);
        } catch (Exception e) {
            log.warn("[CTX] No se pudo calcular progreso del sprint: {}", e.getMessage());
            return null;
        }
    }

    private int computeDaysWithoutActivity(List<FamilyGratitudeEntry> gratitudes,
                                           List<TaskEvidence> evidences,
                                           List<FamilyLogbookEntry> logbooks) {
        Optional<LocalDateTime> last = java.util.stream.Stream.of(
                gratitudes.isEmpty() ? Optional.<LocalDateTime>empty() : Optional.ofNullable(gratitudes.get(0).getCreatedAt()),
                evidences.stream().map(TaskEvidence::getCreatedAt).filter(Objects::nonNull).max(Comparator.naturalOrder()),
                logbooks.isEmpty() ? Optional.<LocalDateTime>empty() : Optional.ofNullable(logbooks.get(0).getCreatedAt())
        ).filter(Optional::isPresent).map(Optional::get).max(Comparator.naturalOrder());

        return last.map(t -> (int) ChronoUnit.DAYS.between(t, LocalDateTime.now()))
                   .orElse(999);
    }

    private int computeStreak(List<FamilyGratitudeEntry> gratitudes,
                               List<TaskEvidence> evidences,
                               List<FamilyLogbookEntry> logbooks) {
        Set<java.time.LocalDate> activeDays = new HashSet<>();
        gratitudes.forEach(g -> { if (g.getCreatedAt() != null) activeDays.add(g.getCreatedAt().toLocalDate()); });
        evidences.forEach(e  -> { if (e.getCreatedAt() != null) activeDays.add(e.getCreatedAt().toLocalDate()); });
        logbooks.forEach(l  -> { if (l.getCreatedAt() != null) activeDays.add(l.getCreatedAt().toLocalDate()); });

        int streak = 0;
        java.time.LocalDate check = java.time.LocalDate.now();
        while (activeDays.contains(check)) {
            streak++;
            check = check.minusDays(1);
        }
        return streak;
    }

    // ─── Alertas ─────────────────────────────────────────────────────────────

    private List<String> buildAlerts(FamilyLongitudinalState lts, List<CriticalDay> crises,
                                     int daysWithoutActivity, String connection) {
        List<String> alerts = new ArrayList<>();

        if (lts != null && lts.isInActiveCrisis())
            alerts.add("Crisis familiar activa — el sistema está en modo de acompañamiento");
        if (lts != null && Boolean.TRUE.equals(lts.getCommunicationCollapseActive()))
            alerts.add("Señal de colapso comunicacional detectada");
        if (daysWithoutActivity >= 14)
            alerts.add(daysWithoutActivity + " días sin actividad registrada");
        else if (daysWithoutActivity >= 7)
            alerts.add("Una semana sin registrar actividad familiar");
        if ("BAJA".equals(connection))
            alerts.add("Nivel de conexión familiar bajo esta semana");
        if (lts != null && lts.getConsecutiveDeteriorations() != null && lts.getConsecutiveDeteriorations() >= 3)
            alerts.add("Tendencia de deterioro sostenida en los últimos registros");
        if (!crises.isEmpty() && crises.get(0).getCreatedAt().isAfter(LocalDateTime.now().minusDays(3)))
            alerts.add("Crisis registrada hace menos de 3 días — seguimiento cercano recomendado");

        return alerts;
    }

    // ─── Recomendaciones ─────────────────────────────────────────────────────

    private List<String> buildRecommendations(String connection, String stress,
                                               String communication, int streak,
                                               int activeRituals) {
        List<String> recs = new ArrayList<>();

        if ("EN_CRISIS".equals(stress) || "CRITICO".equals(stress)) {
            recs.add("Activa el modo de crisis familiar y sigue los pasos de contención");
            return recs;
        }
        if ("BAJA".equals(connection)) {
            recs.add("Realiza una actividad presencial juntos esta semana");
            recs.add("Completa el daily familiar hoy");
        }
        if ("DETERIORANDO".equals(communication)) {
            recs.add("Dedica 10 minutos a escuchar sin interrumpir a cada miembro");
        }
        if (streak >= 7) {
            recs.add("¡7 días de racha! Celebra este logro como familia");
        }
        if (activeRituals > 0) {
            recs.add("Tienes " + activeRituals + " ritual" + (activeRituals > 1 ? "es" : "") + " pendiente" + (activeRituals > 1 ? "s" : "") + " de vivir");
        }
        if ("ALTA".equals(connection) && "MEJORANDO".equals(communication)) {
            recs.add("Momento ideal para registrar una gratitud o subir una evidencia");
        }
        if (recs.isEmpty()) {
            recs.add("Mantén el ritmo — pequeñas acciones diarias construyen grandes familias");
        }
        return recs;
    }

    // ─── Contexto para IA ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public String buildContextBlock(Long familyId) {
        try {
            return contextRepository.findByFamilyId(familyId)
                    .map(snap -> String.format(
                        """
                        Estado Familiar Actual:
                          Conexión: %s | Estrés: %s | Comunicación: %s | Participación: %s
                          Tendencia: %s | Estado general: %s
                          Racha actual: %d días | Sin actividad: %d días
                          ICF: %s | Nivel de riesgo: %s
                        """,
                        snap.getConnectionLevel(),
                        snap.getStressLevel(),
                        snap.getCommunicationTrend(),
                        snap.getParticipationLevel(),
                        snap.getOverallTrend(),
                        snap.getOverallMood(),
                        snap.getCurrentStreak(),
                        snap.getDaysWithoutActivity(),
                        snap.getIcfCurrent() != null ? String.format("%.1f", snap.getIcfCurrent()) : "N/D",
                        snap.getRiskLevel() != null ? snap.getRiskLevel() : "N/D"
                    ))
                    .orElse(null);
        } catch (Exception e) {
            log.warn("[CTX] Error construyendo bloque de contexto: {}", e.getMessage());
            return null;
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private <T> long countRecent(List<T> list, java.util.function.Function<T, LocalDateTime> dateGetter,
                                  LocalDateTime since) {
        return list.stream()
                .filter(item -> { var d = dateGetter.apply(item); return d != null && d.isAfter(since); })
                .count();
    }

    private String mapTrend(String ltsRiskTrend) {
        return switch (ltsRiskTrend) {
            case "IMPROVING"    -> "ASCENDENTE";
            case "DETERIORATING"-> "DESCENDENTE";
            case "CRITICAL"     -> "CRITICA";
            default             -> "ESTABLE";
        };
    }

    private String toJson(List<String> list) {
        try { return objectMapper.writeValueAsString(list); }
        catch (Exception e) { return "[]"; }
    }

    private FamilyContextDto toDto(FamilyContextSnapshot snap, boolean fresh) {
        return new FamilyContextDto(
                snap.getFamilyId(),
                familyRepository.findById(snap.getFamilyId()).map(Family::getName).orElse(""),
                snap.getConnectionLevel(), snap.getStressLevel(),
                snap.getCommunicationTrend(), snap.getParticipationLevel(),
                snap.getOverallTrend(), snap.getOverallMood(),
                snap.getIcfCurrent(), snap.getRiskLevel(),
                snap.getDaysWithoutActivity(), snap.getCurrentStreak(),
                snap.getActiveRitualsCount(), snap.getSprintProgress(),
                parseJson(snap.getAlerts()), parseJson(snap.getRecommendations()),
                snap.getComputedAt(), fresh
        );
    }

    private List<String> parseJson(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (Exception e) { return Collections.emptyList(); }
    }
}
