package com.integrityfamily.cognitive.service;

import com.integrityfamily.domain.*;
import com.integrityfamily.domain.NarrativeChapter.NarrativePhase;
import com.integrityfamily.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * SDD Fase 4 — Motor de Evolución Narrativa Familiar.
 *
 * Construye y actualiza la historia de la familia como una secuencia de capítulos.
 * Cada capítulo representa una fase de transformación real, detectada a partir
 * de datos objetivos (ICF, adherencia, reflexiones).
 *
 * Responsabilidades:
 *  1. Detectar si la evaluación actual abre un nuevo capítulo o continúa el vigente.
 *  2. Detectar puntos de inflexión (crisis, recuperación, breakthrough).
 *  3. Generar títulos y cuerpos narrativos legibles por el dashboard y el copiloto.
 *  4. Cerrar el capítulo anterior al abrir uno nuevo.
 *  5. Devolver el `NarrativeSnapshot` que el copiloto IA consume.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NarrativeEvolutionEngine {

    private final NarrativeChapterRepository chapterRepository;
    private final EvaluationRepository evaluationRepository;
    private final FamilyRepository familyRepository;
    private final FamilyIdentityProfileRepository identityRepository;

    // ─── Punto de entrada ────────────────────────────────────────────────────

    /**
     * Actualiza la narrativa de la familia tras una evaluación.
     * Abre un nuevo capítulo si procede, cierra el anterior y detecta turning points.
     */
    @Transactional
    public NarrativeSnapshot evolve(Long familyId, Evaluation latestEval) {
        log.info("📖 [NARRATIVE] Evolucionando historia para familia ID: {}", familyId);

        List<Evaluation> history = evaluationRepository
                .findByFamilyIdOrderByFinalizedAtAsc(familyId)
                .stream()
                .filter(e -> e.getStatus() == EvaluationStatus.FINALIZED && e.getIcf() != null)
                .toList();

        Optional<NarrativeChapter> currentChapterOpt =
                chapterRepository.findOpenChapterByFamilyId(familyId);

        NarrativePhase targetPhase = resolvePhase(familyId, history, latestEval);
        boolean isTurningPoint = detectTurningPoint(history, latestEval);

        NarrativeChapter chapter;

        if (currentChapterOpt.isEmpty()) {
            // Primera vez — abrir capítulo inicial
            chapter = openNewChapter(familyId, targetPhase, latestEval, isTurningPoint, 1);
        } else {
            NarrativeChapter current = currentChapterOpt.get();
            NarrativePhase currentPhase = current.getPhase();

            boolean phaseChanged = targetPhase != currentPhase;
            boolean forcedByTurningPoint = isTurningPoint
                    && (targetPhase == NarrativePhase.CRISIS || targetPhase == NarrativePhase.RECOVERY);

            if (phaseChanged || forcedByTurningPoint) {
                // Cerrar capítulo actual y abrir uno nuevo
                closeChapter(current, latestEval.getIcf());
                int nextNumber = chapterRepository.countChaptersByFamilyId(familyId) + 1;
                chapter = openNewChapter(familyId, targetPhase, latestEval, isTurningPoint, nextNumber);
                log.info("📚 [NARRATIVE] Nuevo capítulo #{} abierto: fase {} → {}",
                        nextNumber, currentPhase, targetPhase);
            } else {
                // Actualizar cuerpo del capítulo vigente
                chapter = refreshChapterBody(current, history, latestEval);
                if (isTurningPoint) chapter.setTurningPoint(true);
                chapterRepository.save(chapter);
                log.info("✏️ [NARRATIVE] Capítulo #{} actualizado (fase: {})",
                        current.getChapterNumber(), currentPhase);
            }
        }

        List<NarrativeChapter> allChapters =
                chapterRepository.findByFamilyIdOrderByChapterNumberAsc(familyId);

        NarrativeSnapshot snapshot = new NarrativeSnapshot(
                familyId,
                allChapters,
                chapter,
                isTurningPoint,
                buildStoryArcSummary(allChapters, latestEval)
        );

        log.info("✅ [NARRATIVE] Historia actualizada. Capítulos totales: {} | Turning point: {}",
                allChapters.size(), isTurningPoint);
        return snapshot;
    }

    // ─── Resolución de fase ──────────────────────────────────────────────────

    private NarrativePhase resolvePhase(Long familyId, List<Evaluation> history, Evaluation latest) {
        double icf = latest.getIcf();
        int totalEvals = history.size();

        // Crisis: ICF < 40 o hasCrisis activo
        if (Boolean.TRUE.equals(latest.getHasCrisis()) || icf < 40) {
            return NarrativePhase.CRISIS;
        }

        // Recovery: viene de una crisis previa y el ICF remonta
        if (chapterRepository.existsByFamilyIdAndPhase(familyId, NarrativePhase.CRISIS)) {
            double prevIcf = getPreviousIcf(history, latest);
            if (icf > prevIcf + 10 && icf >= 50) {
                return NarrativePhase.RECOVERY;
            }
        }

        // Autonomía: ICF ≥ 80 y muchas evaluaciones
        if (icf >= 80 && totalEvals >= 10) {
            return NarrativePhase.AUTONOMY;
        }

        // Consolidación: ICF ≥ 70 sostenido en últimas 3 evaluaciones
        if (icf >= 70 && totalEvals >= 3) {
            List<Evaluation> last3 = history.subList(Math.max(0, totalEvals - 3), totalEvals);
            boolean allAbove70 = last3.stream().allMatch(e -> e.getIcf() >= 65);
            if (allAbove70) return NarrativePhase.CONSOLIDATION;
        }

        // Transición: entre evaluación 3 y 8, con mejora visible
        if (totalEvals >= 3) {
            double firstIcf = history.get(0).getIcf();
            if (icf > firstIcf + 5) return NarrativePhase.TRANSITION;
        }

        // Discovery: evaluaciones 2-3
        if (totalEvals >= 2) return NarrativePhase.DISCOVERY;

        // Awakening: primera evaluación
        return NarrativePhase.AWAKENING;
    }

    // ─── Detección de turning point ──────────────────────────────────────────

    private boolean detectTurningPoint(List<Evaluation> history, Evaluation latest) {
        if (history.size() < 2) return false;

        double prevIcf = getPreviousIcf(history, latest);
        double delta = latest.getIcf() - prevIcf;

        // Delta ICF significativo (±15 puntos)
        if (Math.abs(delta) >= 15) return true;

        // Crisis activa
        if (Boolean.TRUE.equals(latest.getHasCrisis())) return true;

        // Salto a zona segura desde zona de riesgo
        if (prevIcf < 50 && latest.getIcf() >= 65) return true;

        return false;
    }

    private double getPreviousIcf(List<Evaluation> history, Evaluation latest) {
        return history.stream()
                .filter(e -> !e.getId().equals(latest.getId()))
                .mapToDouble(Evaluation::getIcf)
                .reduce((first, second) -> second)
                .orElse(latest.getIcf());
    }

    // ─── Apertura y cierre de capítulos ─────────────────────────────────────

    private NarrativeChapter openNewChapter(Long familyId, NarrativePhase phase,
                                             Evaluation eval, boolean turningPoint, int number) {
        Family family = familyRepository.getReferenceById(familyId);
        String title = buildTitle(phase, eval, number);
        String body = buildBody(phase, eval, List.of());
        String keyEvent = buildKeyEvent(phase, eval);

        NarrativeChapter chapter = NarrativeChapter.builder()
                .family(family)
                .chapterNumber(number)
                .title(title)
                .body(body)
                .phase(phase)
                .icfAtOpen(eval.getIcf())
                .keyEvent(keyEvent)
                .turningPoint(turningPoint)
                .build();

        return chapterRepository.save(chapter);
    }

    private void closeChapter(NarrativeChapter chapter, double closingIcf) {
        chapter.setClosedAt(LocalDateTime.now());
        chapter.setIcfAtClose(closingIcf);
        chapterRepository.save(chapter);
    }

    private NarrativeChapter refreshChapterBody(NarrativeChapter chapter,
                                                  List<Evaluation> history, Evaluation latest) {
        chapter.setBody(buildBody(chapter.getPhase(), latest, history));
        return chapter;
    }

    // ─── Generación de texto narrativo ──────────────────────────────────────

    private String buildTitle(NarrativePhase phase, Evaluation eval, int chapter) {
        return switch (phase) {
            case AWAKENING     -> "Capítulo " + chapter + ": El Despertar";
            case DISCOVERY     -> "Capítulo " + chapter + ": Los Primeros Patrones";
            case TRANSITION    -> "Capítulo " + chapter + ": El Cambio en Marcha";
            case CONSOLIDATION -> "Capítulo " + chapter + ": Consolidando el Progreso";
            case CRISIS        -> "Capítulo " + chapter + ": La Tormenta";
            case RECOVERY      -> "Capítulo " + chapter + ": Resurgiendo";
            case AUTONOMY      -> "Capítulo " + chapter + ": Vuelo Propio";
        };
    }

    private String buildBody(NarrativePhase phase, Evaluation latest, List<Evaluation> history) {
        double icf = latest.getIcf();
        String dimension = latest.getCriticalDimension() != null
                ? latest.getCriticalDimension() : "varias dimensiones";
        String risk = latest.getRiskLevel() != null ? latest.getRiskLevel() : "MODERADO";

        return switch (phase) {
            case AWAKENING -> String.format(
                "La familia inicia su proceso de diagnóstico con un ICF de %.1f. " +
                "El sistema detecta que '%s' es el área que requiere mayor atención. " +
                "Este primer ciclo establece la línea base desde la cual mediremos toda evolución futura.",
                icf, dimension);

            case DISCOVERY -> String.format(
                "Con %.1f puntos de ICF y nivel de riesgo %s, los primeros patrones emergen. " +
                "La dimensión '%s' continúa siendo el foco central. " +
                "La familia comienza a reconocer sus dinámicas internas y los ciclos que las sostienen.",
                icf, risk.toLowerCase(), dimension);

            case TRANSITION -> String.format(
                "El cambio es visible: ICF %.1f. La familia está en proceso activo de transformación. " +
                "Las intervenciones sobre '%s' están produciendo resultados medibles. " +
                "Este capítulo documenta el esfuerzo sostenido que convierte la intención en hábito.",
                icf, dimension);

            case CONSOLIDATION -> String.format(
                "ICF de %.1f sostenido — la familia ha cruzado el umbral de la consolidación. " +
                "Los cambios ya no son esfuerzos conscientes sino parte del funcionamiento cotidiano. " +
                "La dimensión '%s' muestra estabilidad duradera.",
                icf, dimension);

            case CRISIS -> String.format(
                "⚠️ ICF crítico: %.1f. Nivel de riesgo: %s. La familia atraviesa su momento más desafiante. " +
                "La dimensión '%s' reporta la mayor tensión. " +
                "El sistema activa protocolos de contención y soporte intensivo.",
                icf, risk, dimension);

            case RECOVERY -> String.format(
                "La familia remonta desde la crisis: ICF %.1f en recuperación. " +
                "El trabajo sobre '%s' está dando resultados. " +
                "Este capítulo documenta la resiliencia: la capacidad de levantarse y reencuadrar.",
                icf, dimension);

            case AUTONOMY -> String.format(
                "ICF %.1f — la familia opera de forma autónoma y sostenida. " +
                "El acompañamiento intensivo ya no es necesario. " +
                "Este capítulo cierra un ciclo completo de transformación familiar consciente.",
                icf);
        };
    }

    private String buildKeyEvent(NarrativePhase phase, Evaluation eval) {
        return switch (phase) {
            case AWAKENING     -> "Primera evaluación — línea base ICF: " + eval.getIcf();
            case DISCOVERY     -> "Patrones identificados en dimensión: " + eval.getCriticalDimension();
            case TRANSITION    -> "Mejora sostenida detectada — ICF: " + eval.getIcf();
            case CONSOLIDATION -> "Umbral de consolidación alcanzado — ICF ≥ 70 sostenido";
            case CRISIS        -> Boolean.TRUE.equals(eval.getHasCrisis())
                    ? "Crisis activa reportada — intervención urgente activada"
                    : "ICF crítico < 40: " + eval.getIcf();
            case RECOVERY      -> "Remontada desde crisis — ICF: " + eval.getIcf();
            case AUTONOMY      -> "ICF ≥ 80 sostenido — ciclos completos alcanzados";
        };
    }

    // ─── Story Arc Summary ───────────────────────────────────────────────────

    private String buildStoryArcSummary(List<NarrativeChapter> chapters, Evaluation latest) {
        if (chapters.isEmpty()) return "Historia en construcción.";

        NarrativeChapter first = chapters.get(0);
        NarrativeChapter current = chapters.stream()
                .filter(NarrativeChapter::isOpen).findFirst()
                .orElse(chapters.get(chapters.size() - 1));

        long turningPoints = chapters.stream().filter(c -> Boolean.TRUE.equals(c.getTurningPoint())).count();

        return String.format(
            "Historia en %d capítulos. Inicio: ICF %.1f (%s) → Actual: ICF %.1f (%s). " +
            "Puntos de inflexión: %d. Fase actual: %s.",
            chapters.size(),
            first.getIcfAtOpen() != null ? first.getIcfAtOpen() : 0.0,
            first.getPhase(),
            latest.getIcf(),
            current.getPhase(),
            turningPoints,
            current.getPhase()
        );
    }

    // ─── Consulta pública ────────────────────────────────────────────────────

    /**
     * Devuelve el snapshot narrativo actual sin modificar nada.
     * Usado por el copiloto IA y el dashboard.
     */
    @Transactional(readOnly = true)
    public NarrativeSnapshot getSnapshot(Long familyId) {
        List<NarrativeChapter> chapters =
                chapterRepository.findByFamilyIdOrderByChapterNumberAsc(familyId);
        NarrativeChapter current = chapterRepository.findOpenChapterByFamilyId(familyId)
                .orElse(chapters.isEmpty() ? null : chapters.get(chapters.size() - 1));

        List<Evaluation> history = evaluationRepository
                .findByFamilyIdOrderByFinalizedAtAsc(familyId)
                .stream()
                .filter(e -> e.getStatus() == EvaluationStatus.FINALIZED && e.getIcf() != null)
                .toList();

        double latestIcf = history.isEmpty() ? 0.0 : history.get(history.size() - 1).getIcf();
        String arc = history.isEmpty() ? "Sin evaluaciones aún." : buildStoryArcSummaryRaw(chapters, latestIcf);

        return new NarrativeSnapshot(familyId, chapters, current, false, arc);
    }

    private String buildStoryArcSummaryRaw(List<NarrativeChapter> chapters, double latestIcf) {
        if (chapters.isEmpty()) return "Historia en construcción.";
        NarrativeChapter first = chapters.get(0);
        NarrativeChapter last = chapters.get(chapters.size() - 1);
        long tp = chapters.stream().filter(c -> Boolean.TRUE.equals(c.getTurningPoint())).count();
        return String.format(
            "Historia en %d capítulos. Inicio ICF: %.1f → Actual: %.1f. Puntos de inflexión: %d. Fase: %s.",
            chapters.size(), first.getIcfAtOpen() != null ? first.getIcfAtOpen() : 0.0,
            latestIcf, tp, last.getPhase()
        );
    }

    // ─── Tipos de datos ──────────────────────────────────────────────────────

    public record NarrativeSnapshot(
            Long familyId,
            List<NarrativeChapter> chapters,
            NarrativeChapter currentChapter,
            boolean turningPointDetected,
            String storyArcSummary
    ) {
        public int totalChapters() { return chapters.size(); }
        public boolean hasHistory() { return !chapters.isEmpty(); }
        public NarrativePhase currentPhase() {
            return currentChapter != null ? currentChapter.getPhase() : NarrativePhase.AWAKENING;
        }
    }
}
