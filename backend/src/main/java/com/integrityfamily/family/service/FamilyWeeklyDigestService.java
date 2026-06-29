package com.integrityfamily.family.service;

import com.integrityfamily.common.service.WhatsAppService;
import com.integrityfamily.domain.EvaluationStatus;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyJourneySnapshot;
import com.integrityfamily.domain.repository.*;
import com.integrityfamily.family.dto.FamilyHealthSummaryResponse;
import com.integrityfamily.scanner.dto.SubtleSignalRadarResponse;
import com.integrityfamily.scanner.service.SubtleSignalRadarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Genera y envía un resumen semanal de progreso familiar vía WhatsApp.
 *
 * El digest incluye:
 *  - ICF actual y delta de los últimos 7 días
 *  - Nivel del viaje y si se subió de nivel esta semana
 *  - Fase de evolución (radar)
 *  - Tasa de completitud de tareas del plan
 *  - Sprint activo (si lo hay)
 *  - Próximo paso concreto
 *
 * Se envía solo a familias con WhatsApp configurado.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FamilyWeeklyDigestService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("d 'de' MMMM", new Locale("es", "CO"));

    private final FamilyRepository              familyRepository;
    private final EvaluationRepository          evaluationRepository;
    private final PlanTaskRepository            planTaskRepository;
    private final FamilySprintRepository        sprintRepository;
    private final FamilyJourneySnapshotRepository snapshotRepository;
    private final SubtleSignalRadarService      radarService;
    private final FamilyJourneyService          journeyService;
    private final WhatsAppService               whatsAppService;

    /**
     * Envía el digest semanal a una familia.
     *
     * @return true si se envió correctamente.
     */
    @Transactional(readOnly = true)
    public boolean sendDigest(Long familyId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new IllegalArgumentException("Familia no encontrada: " + familyId));

        if (family.getWhatsapp() == null || family.getWhatsapp().isBlank()) {
            log.debug("[DIGEST] Familia {} sin WhatsApp — omitida.", familyId);
            return false;
        }

        try {
            DigestData data = collectData(family);
            String message = buildMessage(family.getName(), data);
            whatsAppService.sendToFamily(family, message);
            log.info("[DIGEST] ✅ Digest enviado a familia {} (nivel {}, ICF {})",
                    familyId, data.journeyLevel, data.currentIcf);
            return true;
        } catch (Exception e) {
            log.warn("[DIGEST] Error enviando digest a familia {}: {}", familyId, e.getMessage());
            return false;
        }
    }

    // ─── Recopilación de datos ────────────────────────────────────────────────

    private DigestData collectData(Family family) {
        Long familyId = family.getId();
        DigestData d = new DigestData();

        // ICF actual y delta 7 días
        var evals = evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(familyId)
                .stream().filter(e -> e.getStatus() == EvaluationStatus.FINALIZED
                        && e.getIcf() != null).toList();
        if (!evals.isEmpty()) {
            d.currentIcf = evals.get(evals.size() - 1).getIcf();
            var cutoff = LocalDate.now().minusDays(7).atStartOfDay();
            evals.stream()
                    .filter(e -> e.getFinalizedAt() != null && e.getFinalizedAt().isBefore(cutoff))
                    .max(Comparator.comparing(e -> e.getFinalizedAt()))
                    .ifPresent(old -> d.icfDelta7d = d.currentIcf - old.getIcf());
        }

        // Viaje
        var journey = journeyService.evaluate(familyId);
        d.journeyLevel    = journey.currentLevel();
        d.journeyProgress = journey.journeyProgress();
        d.nextAction      = journey.nextAction();

        // Level-up esta semana
        d.levelUpThisWeek = snapshotRepository
                .findByFamilyIdOrderBySnapshotDateAsc(familyId).stream()
                .filter(s -> s.isLevelUp()
                        && s.getSnapshotDate().isAfter(LocalDate.now().minusDays(7)))
                .findFirst()
                .map(FamilyJourneySnapshot::getJourneyLevel)
                .orElse(null);

        // Tareas completadas vs total
        d.totalTasks     = planTaskRepository.countByFamilyId(familyId);
        d.completedTasks = planTaskRepository.countCompletedByFamilyId(familyId);

        // Sprint activo
        sprintRepository.findByFamilyIdOrderByCreatedAtDesc(familyId).stream()
                .filter(s -> "ACTIVE".equals(s.getStatus()))
                .findFirst()
                .ifPresent(s -> d.activeSprintStatus = s.getStatus());

        // Fase de evolución (radar, silencioso si falla)
        if (evals.size() >= 2) {
            try {
                var radar = radarService.analyze(familyId);
                if (radar.icfOverall() != null) {
                    d.evolutionPhase = radar.icfOverall().evolutionPhase();
                }
                d.highSignals = (int) radar.microSignals().stream()
                        .filter(s -> "HIGH".equals(s.severity())).count();
            } catch (Exception ignored) {
                log.debug("[DIGEST] Radar no disponible para familia {}", familyId);
            }
        }

        return d;
    }

    // ─── Construcción del mensaje ─────────────────────────────────────────────

    private String buildMessage(String familyName, DigestData d) {
        var sb = new StringBuilder();
        var today = LocalDate.now().format(DATE_FMT);

        sb.append("📋 *Resumen Semanal — ").append(familyName).append("*\n");
        sb.append("_").append(today).append("_\n\n");

        // ICF
        if (d.currentIcf != null) {
            sb.append("📊 *Índice ICF:* ").append(String.format("%.0f", d.currentIcf))
              .append(" — ").append(FamilyHealthSummaryResponse.icfLabel(d.currentIcf));
            if (d.icfDelta7d != null) {
                String sign = d.icfDelta7d >= 0 ? "+" : "";
                sb.append(" (").append(sign).append(String.format("%.1f", d.icfDelta7d)).append(" esta semana)");
            }
            sb.append("\n");
        }

        // Fase
        if (d.evolutionPhase != null) {
            String phaseEmoji = switch (d.evolutionPhase.toLowerCase()) {
                case "pleno" -> "✨";
                case "consciente" -> "🧠";
                case "reactivo" -> "😤";
                default -> "😶";
            };
            sb.append(phaseEmoji).append(" *Fase de evolución:* ").append(capitalize(d.evolutionPhase)).append("\n");
        }

        // Señales altas
        if (d.highSignals > 0) {
            sb.append("🔴 *Señales de alta intensidad:* ").append(d.highSignals)
              .append(" — revisa el Radar en la app\n");
        }

        sb.append("\n");

        // Viaje
        sb.append("🧭 *Viaje Familiar:* Nivel ").append(d.journeyLevel)
          .append(" · ").append(d.journeyProgress).append("% del camino\n");
        if (d.levelUpThisWeek != null) {
            sb.append("🎉 *¡Subiste al nivel ").append(d.levelUpThisWeek).append(" esta semana!*\n");
        }

        // Tareas
        if (d.totalTasks > 0) {
            int pct = (int) Math.round((d.completedTasks * 100.0) / d.totalTasks);
            sb.append("✅ *Plan familiar:* ").append(d.completedTasks).append("/")
              .append(d.totalTasks).append(" tareas (").append(pct).append("%)\n");
        }

        // Sprint
        if (d.activeSprintStatus != null) {
            sb.append("⚡ *Sprint activo* — el equipo está en movimiento\n");
        }

        sb.append("\n");

        // Próximo paso
        sb.append("👉 *Próximo paso:*\n").append(d.nextAction).append("\n\n");

        sb.append("_Abre la app para ver tu mapa completo de viaje y salud familiar._");
        return sb.toString();
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ─── DTO interno ─────────────────────────────────────────────────────────

    static class DigestData {
        Double  currentIcf;
        Double  icfDelta7d;
        int     journeyLevel;
        int     journeyProgress;
        String  nextAction;
        Integer levelUpThisWeek;  // nivel alcanzado esta semana, o null
        long    totalTasks;
        long    completedTasks;
        String  activeSprintStatus;
        String  evolutionPhase;
        int     highSignals;
    }
}
