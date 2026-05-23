package com.integrityfamily.milestone.service;

import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.EvaluationStatus;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.Milestone;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.MilestoneRepository;
import com.integrityfamily.domain.repository.PlanTaskRepository;
import com.integrityfamily.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.*;

/**
 * Motor de Avance Automático de Hitos — Taxonomía v2.
 *
 * Un hito se avanza automáticamente cuando la familia cumple los tres criterios:
 *
 *   1. TIEMPO  — lleva al menos (durationDays × 0.85) días en el hito actual.
 *                (Se admite 15% de margen para evaluar ligeramente antes del límite.)
 *
 *   2. ICF     — el promedio de ICF de las evaluaciones finalizadas en este hito
 *                supera el umbral mínimo definido por fase de consciencia.
 *
 *   3. TAREAS  — al menos el % mínimo de tareas del hito actual están completadas
 *                (umbral adaptativo según nivel de riesgo último conocido).
 *
 * Si la familia no tiene evaluaciones ni tareas en el hito (recién llegó),
 * sólo aplica el criterio de TIEMPO + al menos 1 evaluación finalizada.
 *
 * Avance por scheduler:
 *   Corre cada 24 horas. Evalúa todas las familias activas.
 *   También se puede invocar directamente desde EvaluationService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MilestoneService {

    private final MilestoneRepository    milestoneRepository;
    private final FamilyRepository       familyRepository;
    private final EvaluationRepository   evaluationRepository;
    private final PlanTaskRepository     planTaskRepository;

    // ─── Umbrales de ICF mínimo por hito ────────────────────────────────────
    private static final Map<String, Double> ICF_MIN_BY_MILESTONE = Map.of(
            "W1",  40.0,
            "M1",  45.0,
            "M3",  50.0,
            "M6",  52.0,
            "M9",  55.0,
            "M12", 58.0,
            "M18", 60.0,
            "M24", 62.0,
            "M30", 65.0,
            "M36", 70.0   // terminal — nunca se avanza, pero se checkea igual
    );

    /** Umbral de completitud de tareas (%) según riesgo */
    private static final Map<String, Double> TASK_COMPLETION_THRESHOLD = Map.of(
            "CRITICO",  0.30,  // 30% — familia en crisis, ayuda máxima para avanzar
            "ALTO",     0.45,
            "MODERADO", 0.60,
            "BAJO",     0.70
    );

    /** Orden canónico de hitos */
    private static final List<String> MILESTONE_ORDER =
            List.of("W1","M1","M3","M6","M9","M12","M18","M24","M30","M36");

    // ─── API Pública ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Milestone findById(Long id) {
        return milestoneRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Hito no encontrado con ID: " + id));
    }

    @Transactional
    public Milestone create(Milestone milestone) {
        return milestoneRepository.save(milestone);
    }

    @Transactional
    public Milestone update(Long id, Milestone milestone) {
        Milestone existing = findById(id);
        existing.setTitle(milestone.getTitle());
        existing.setLabel(milestone.getLabel());
        existing.setDurationDays(milestone.getDurationDays());
        existing.setOrderIndex(milestone.getOrderIndex());
        existing.setDescription(milestone.getDescription());
        return milestoneRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!milestoneRepository.existsById(id))
            throw new NotFoundException("No se puede eliminar: Hito no existe.");
        milestoneRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Milestone> findAll() {
        return milestoneRepository.findAll().stream()
                .sorted(Comparator.comparingInt(m -> m.getOrderIndex() != null ? m.getOrderIndex() : 99))
                .toList();
    }

    @Transactional(readOnly = true)
    public String getCurrentMilestoneLabel(Long familyId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new NotFoundException("Familia no encontrada"));
        String code = family.getCurrentMilestone();
        return milestoneRepository.findByCode(code)
                .map(m -> m.getLabel() != null ? m.getLabel() : m.getTitle())
                .orElse("Hito " + code);
    }

    // ─── Evaluación de criterios ──────────────────────────────────────────────

    /**
     * Evalúa si una familia cumple los tres criterios de avance.
     * No hace cambios en BD — solo análisis.
     */
    @Transactional(readOnly = true)
    public AdvancementEvaluation evaluate(Long familyId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new NotFoundException("Familia no encontrada: " + familyId));

        String current = normalizedMilestone(family.getCurrentMilestone());

        // Terminal: M36 no avanza
        if ("M36".equals(current)) {
            return AdvancementEvaluation.terminal(familyId, current);
        }

        // ── Criterio 1: TIEMPO ───────────────────────────────────────────────
        Milestone milestoneDef = milestoneRepository.findByCode(current).orElse(null);
        int requiredDays = milestoneDef != null && milestoneDef.getDurationDays() != null
                ? milestoneDef.getDurationDays() : fallbackDays(current);
        int minDays = (int) Math.floor(requiredDays * 0.85);

        LocalDateTime startedAt = family.getMilestoneStartedAt() != null
                ? family.getMilestoneStartedAt()
                : family.getCreatedAt() != null ? family.getCreatedAt() : LocalDateTime.now().minusDays(999);

        long daysElapsed = ChronoUnit.DAYS.between(startedAt, LocalDateTime.now());
        boolean timeMet = daysElapsed >= minDays;

        // ── Criterio 2: ICF ──────────────────────────────────────────────────
        List<Evaluation> evals = evaluationRepository
                .findByFamilyIdAndMilestoneKeyAndStatus(familyId, current, EvaluationStatus.FINALIZED);
        boolean hasEvals = !evals.isEmpty();

        double icfAvg = evals.stream()
                .filter(e -> e.getIcf() != null)
                .mapToDouble(Evaluation::getIcf)
                .average()
                .orElse(0.0);

        double icfMin = ICF_MIN_BY_MILESTONE.getOrDefault(current, 50.0);
        // icfMet = umbral superado; requiere al menos 1 evaluación Y que el promedio alcance el mínimo.
        // Se evalúa así para que el log y el registro AdvancementEvaluation sean semánticamente claros:
        // icfMet=false + hasEvals=false → aún sin datos; icfMet=false + hasEvals=true → datos insuficientes.
        boolean icfMet = hasEvals && icfAvg >= icfMin;

        // ── Criterio 3: TAREAS ───────────────────────────────────────────────
        long totalTasks     = planTaskRepository.countByFamilyIdAndMilestoneCode(familyId, current);
        long completedTasks = planTaskRepository.countCompletedByFamilyIdAndMilestoneCode(familyId, current);

        String lastRisk = evals.stream()
                .filter(e -> e.getRiskLevel() != null)
                .max(Comparator.comparing(Evaluation::getFinalizedAt))
                .map(Evaluation::getRiskLevel)
                .orElse("MODERADO");

        double taskThreshold = TASK_COMPLETION_THRESHOLD.getOrDefault(lastRisk.toUpperCase(), 0.60);
        double completionRatio = totalTasks > 0 ? (double) completedTasks / totalTasks : 1.0;
        boolean tasksMet = totalTasks == 0 || completionRatio >= taskThreshold;

        // Avance requiere: tiempo suficiente + al menos 1 evaluación finalizada + ICF ≥ umbral + tareas
        boolean canAdvance = timeMet && hasEvals && icfMet && tasksMet;

        log.info("[MILESTONE-EVAL] Familia={} | Hito={} | Días={}/{} (time={}) | ICF={}/{}(icf={}) | Tareas={}/{}@{}%(task={}) → AVANCE={}",
                familyId, current,
                daysElapsed, minDays, timeMet,
                String.format("%.1f", icfAvg), icfMin, icfMet,
                completedTasks, totalTasks, String.format("%.0f", taskThreshold * 100), tasksMet,
                canAdvance);

        return new AdvancementEvaluation(
                familyId, current, canAdvance,
                timeMet, icfMet, tasksMet,
                daysElapsed, minDays,
                icfAvg, icfMin,
                completedTasks, totalTasks,
                completionRatio, taskThreshold,
                lastRisk, false
        );
    }

    /**
     * Intenta avanzar el hito de una familia.
     * Llamado desde EvaluationService tras cada evaluación finalizada.
     *
     * @return código del nuevo hito si hubo avance, o el mismo código si no aplica
     */
    @Transactional
    public String advanceMilestone(Long familyId) {
        AdvancementEvaluation result = evaluate(familyId);

        if (result.terminal()) {
            log.info("[MILESTONE] Familia {} ya está en hito terminal M36.", familyId);
            return result.currentMilestone();
        }

        if (!result.canAdvance()) {
            logBlockedReason(result);
            return result.currentMilestone();
        }

        return doAdvance(familyId, result.currentMilestone());
    }

    /**
     * Job diario: avanza todas las familias que califican.
     * Corre a las 03:00 AM cada día.
     *
     * Procesa las familias en páginas de 50 para evitar OOM en instalaciones con
     * muchas familias activas. Cada página abre/cierra su propia transacción
     * (propagación REQUIRED desde evaluate/doAdvance).
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void scheduledAdvancement() {
        log.info("[MILESTONE-SCHEDULER] Iniciando avance automático diario de hitos...");
        int advanced  = 0;
        int evaluated = 0;
        int page      = 0;
        final int PAGE_SIZE = 50;

        Page<Family> batch;
        do {
            batch = familyRepository.findAll(PageRequest.of(page, PAGE_SIZE));
            for (Family family : batch.getContent()) {
                try {
                    evaluated++;
                    AdvancementEvaluation result = evaluate(family.getId());
                    if (result.canAdvance() && !result.terminal()) {
                        doAdvance(family.getId(), result.currentMilestone());
                        advanced++;
                    }
                } catch (Exception e) {
                    log.warn("[MILESTONE-SCHEDULER] Error evaluando familia ID={}: {}",
                            family.getId(), e.getMessage());
                }
            }
            page++;
        } while (batch.hasNext());

        log.info("[MILESTONE-SCHEDULER] Completado. Familias evaluadas={} | Avanzadas={}", evaluated, advanced);
    }

    /**
     * Devuelve el diagnóstico completo de avance para el endpoint de consulta.
     */
    @Transactional(readOnly = true)
    public AdvancementEvaluation getAdvancementStatus(Long familyId) {
        return evaluate(familyId);
    }

    // ─── Helpers privados ─────────────────────────────────────────────────────

    private String doAdvance(Long familyId, String currentMilestone) {
        Family family = familyRepository.findById(familyId).orElseThrow();

        int currentIdx = MILESTONE_ORDER.indexOf(currentMilestone);
        if (currentIdx < 0 || currentIdx >= MILESTONE_ORDER.size() - 1) {
            log.warn("[MILESTONE] Familia {} en hito desconocido o terminal: {}", familyId, currentMilestone);
            return currentMilestone;
        }

        String next = MILESTONE_ORDER.get(currentIdx + 1);
        family.setCurrentMilestone(next);
        family.setMilestoneStartedAt(LocalDateTime.now());
        family.setMilestoneIcfAvg(null); // Resetear para el nuevo hito
        familyRepository.save(family);

        log.info("[MILESTONE] AVANCE CONFIRMADO — Familia '{}' (ID={}) :: {} → {}",
                family.getName(), familyId, currentMilestone, next);
        return next;
    }

    /** Normaliza hitos legado al formato v2 */
    private String normalizedMilestone(String raw) {
        if (raw == null || raw.isBlank() || "MES_00_DIAGNOSTICO".equals(raw) || "M00".equals(raw))
            return "W1";
        return raw;
    }

    private int fallbackDays(String milestone) {
        return switch (milestone) {
            case "W1"  -> 7;
            case "M1"  -> 30;
            case "M3"  -> 90;
            case "M6"  -> 180;
            case "M9"  -> 270;
            case "M12" -> 365;
            case "M18" -> 540;
            case "M24" -> 730;
            case "M30" -> 910;
            case "M36" -> 1095;
            default    -> 30;
        };
    }

    private void logBlockedReason(AdvancementEvaluation r) {
        List<String> blocked = new ArrayList<>();
        if (!r.timeMet())  blocked.add(String.format("tiempo(%d/%d días)", r.daysElapsed(), r.minDays()));
        if (!r.icfMet())   blocked.add(String.format("ICF(%.1f/%.1f)", r.icfAvg(), r.icfMin()));
        if (!r.tasksMet()) blocked.add(String.format("tareas(%.0f%%/%.0f%%)", r.completionRatio()*100, r.taskThreshold()*100));
        log.info("[MILESTONE] Familia {} bloqueada en {}. Pendiente: {}", r.familyId(), r.currentMilestone(), blocked);
    }

    // ─── Resultado ────────────────────────────────────────────────────────────

    /**
     * Resultado completo de la evaluación de avance de hito.
     *
     * @param familyId          ID de la familia evaluada
     * @param currentMilestone  Hito actual normalizado
     * @param canAdvance        true si los tres criterios se cumplen
     * @param timeMet           Criterio 1: tiempo suficiente en el hito
     * @param icfMet            Criterio 2: ICF promedio ≥ umbral del hito
     * @param tasksMet          Criterio 3: % tareas completadas ≥ umbral de riesgo
     * @param daysElapsed       Días transcurridos en el hito actual
     * @param minDays           Días mínimos requeridos (durationDays × 0.85)
     * @param icfAvg            ICF promedio de las evaluaciones en este hito
     * @param icfMin            ICF mínimo requerido para este hito
     * @param completedTasks    Tareas completadas del hito actual
     * @param totalTasks        Total de tareas del hito actual
     * @param completionRatio   Ratio completadas/total (0.0 – 1.0)
     * @param taskThreshold     Umbral de completitud requerido según riesgo
     * @param riskLevel         Nivel de riesgo más reciente de la familia
     * @param terminal          true si está en M36 (hito final)
     */
    public record AdvancementEvaluation(
            Long   familyId,
            String currentMilestone,
            boolean canAdvance,
            boolean timeMet,
            boolean icfMet,
            boolean tasksMet,
            long   daysElapsed,
            int    minDays,
            double icfAvg,
            double icfMin,
            long   completedTasks,
            long   totalTasks,
            double completionRatio,
            double taskThreshold,
            String riskLevel,
            boolean terminal
    ) {
        /** Constructor para el caso terminal (M36) */
        static AdvancementEvaluation terminal(Long familyId, String milestone) {
            return new AdvancementEvaluation(
                    familyId, milestone, false, true, true, true,
                    0, 0, 0.0, 0.0, 0L, 0L, 1.0, 0.0, "BAJO", true);
        }

        public String summary() {
            if (terminal) return "Hito terminal M36 alcanzado.";
            return String.format("Hito=%s | Avance=%s | tiempo=%s | ICF=%s | tareas=%s",
                    currentMilestone, canAdvance,
                    timeMet ? "OK" : "PENDIENTE",
                    icfMet  ? "OK" : String.format("%.1f/%.1f", icfAvg, icfMin),
                    tasksMet? "OK" : String.format("%.0f%%/%.0f%%", completionRatio*100, taskThreshold*100));
        }
    }
}
