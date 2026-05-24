package com.integrityfamily.cognitive.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.FamilyMemory.MemoryType;
import com.integrityfamily.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * SDD Fase 3 — Capa de Reflexión Autónoma del Sistema Cognitivo.
 *
 * El sistema evalúa su propio desempeño y aprende de él.
 * No es un chatbot reflexivo — es un motor de autoevaluación operacional.
 *
 * Responsabilidades:
 *  1. Evaluar la efectividad de intervenciones pasadas (¿mejoró la familia?).
 *  2. Generar "lessons learned" como nuevos patrones semánticos.
 *  3. Detectar patrones de abandono antes de que ocurran.
 *  4. Actualizar la narrativa identitaria de la familia.
 *  5. Producir un ReflectionReport estructurado para el dashboard.
 *
 * Trigger:
 *  - Post-evaluación (EvaluationService.processPostFinalization)
 *  - Semanal vía CognitiveReflectionScheduler
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FamilyReflectionService {

    private final FamilyMemoryRepository memoryRepository;
    private final FamilyIdentityProfileRepository identityRepository;
    private final LearnedSkillRepository skillRepository;
    private final ReflectionRepository reflectionRepository;
    private final LearningEntryRepository learningEntryRepository;
    private final EvaluationRepository evaluationRepository;
    private final FamilyMetricsSnapshotRepository snapshotRepository;
    private final FamilyRepository familyRepository;
    private final ObjectMapper objectMapper;

    /** Cache de última reflexión por familia (sólo en memoria, se pierde en restart). */
    private final Map<Long, ReflectionReport> latestReportCache = new ConcurrentHashMap<>();

    // ─── Punto de entrada principal ──────────────────────────────────────────

    /**
     * Ejecuta el ciclo completo de reflexión autónoma para una familia.
     * Devuelve un reporte estructurado con hallazgos y acciones recomendadas.
     */
    @Transactional
    public ReflectionReport reflect(Long familyId) {
        log.info("🪞 [REFLECTION] Iniciando ciclo de autoevaluación para familia ID: {}", familyId);

        InterventionEffectiveness effectiveness = evaluateInterventionEffectiveness(familyId);
        AbandonmentRisk abandonmentRisk = detectAbandonmentRisk(familyId);
        Optional<String> lessonLearned = generateLessonLearned(familyId, effectiveness);
        String updatedNarrative = updateIdentityNarrative(familyId, effectiveness, abandonmentRisk);

        lessonLearned.ifPresent(lesson ->
            persistLessonAsSemanticMemory(familyId, lesson, effectiveness));

        ReflectionReport report = new ReflectionReport(
                familyId,
                LocalDateTime.now(),
                effectiveness,
                abandonmentRisk,
                lessonLearned.orElse(null),
                updatedNarrative
        );

        log.info("✅ [REFLECTION] Ciclo completo. Efectividad: {} | Riesgo abandono: {} | Lección: {}",
                effectiveness.level(), abandonmentRisk.level(),
                lessonLearned.map(l -> l.substring(0, Math.min(60, l.length())) + "...").orElse("ninguna"));

        latestReportCache.put(familyId, report);
        return report;
    }

    /**
     * Devuelve la última reflexión calculada para la familia.
     * Si no hay caché (primer acceso o reinicio), realiza un análisis de sólo lectura
     * (sin persistir lecciones ni actualizar narrativa) para que el banner pueda
     * cargarse sin disparar el ciclo completo.
     */
    @Transactional(readOnly = true)
    public ReflectionReport getLatest(Long familyId) {
        ReflectionReport cached = latestReportCache.get(familyId);
        if (cached != null) {
            log.debug("📋 [REFLECTION] Devolviendo reporte cacheado para familia ID: {}", familyId);
            return cached;
        }

        log.info("🔍 [REFLECTION] Sin caché para familia ID: {} — análisis read-only", familyId);
        InterventionEffectiveness effectiveness = evaluateInterventionEffectiveness(familyId);
        AbandonmentRisk abandonmentRisk = detectAbandonmentRisk(familyId);

        // Read-only: no se persiste lección ni se actualiza narrativa
        ReflectionReport report = new ReflectionReport(
                familyId,
                LocalDateTime.now(),
                effectiveness,
                abandonmentRisk,
                null,
                null
        );
        latestReportCache.put(familyId, report);
        return report;
    }

    // ─── 1. Efectividad de intervenciones ────────────────────────────────────

    /**
     * Mide si las intervenciones del sistema (planes, misiones, reflexiones)
     * produjeron mejora real y sostenida en la familia.
     */
    private InterventionEffectiveness evaluateInterventionEffectiveness(Long familyId) {
        List<Evaluation> evaluations = evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(familyId)
                .stream()
                .filter(e -> e.getStatus() == EvaluationStatus.FINALIZED && e.getIcf() != null)
                .toList();

        List<FamilyMetricsSnapshot> snapshots = snapshotRepository
                .findByFamilyIdOrderBySnapshotDateAsc(familyId);

        List<Reflection> reflections = reflectionRepository.findByFamilyId(familyId);

        // Sin datos suficientes
        if (evaluations.size() < 2 && snapshots.size() < 2) {
            return new InterventionEffectiveness(EffectivenessLevel.INSUFFICIENT_DATA,
                    0, 0.0, 0.0, 0.0, "Datos insuficientes para evaluar efectividad.");
        }

        // Tendencia ICF
        double icfTrend = 0.0;
        if (evaluations.size() >= 2) {
            double first = evaluations.get(0).getIcf();
            double last = evaluations.get(evaluations.size() - 1).getIcf();
            icfTrend = last - first;
        }

        // Adherencia promedio
        double avgAdherence = snapshots.stream()
                .filter(s -> s.getAdherence() != null)
                .mapToDouble(FamilyMetricsSnapshot::getAdherence)
                .average().orElse(50.0);

        // Tasa de reflexiones completadas
        long completed = reflections.stream()
                .filter(r -> r.getStatus() == ReflectionStatus.COMPLETED).count();
        double reflectionRate = reflections.isEmpty() ? 0.0
                : (double) completed / reflections.size();

        // Impacto emocional promedio
        double avgEmotionalImpact = reflections.stream()
                .filter(r -> r.getEmotionalImpact() != null)
                .mapToInt(Reflection::getEmotionalImpact)
                .average().orElse(3.0);

        EffectivenessLevel level = calculateEffectivenessLevel(icfTrend, avgAdherence, reflectionRate);

        String summary = buildEffectivenessSummary(level, icfTrend, avgAdherence, reflectionRate, avgEmotionalImpact);

        return new InterventionEffectiveness(
                level, evaluations.size(), icfTrend, avgAdherence, reflectionRate, summary
        );
    }

    private EffectivenessLevel calculateEffectivenessLevel(double icfTrend, double adherence, double reflectionRate) {
        int score = 0;
        if (icfTrend > 10) score += 3;
        else if (icfTrend > 0) score += 1;
        else if (icfTrend < -10) score -= 2;

        if (adherence > 70) score += 2;
        else if (adherence > 50) score += 1;
        else if (adherence < 30) score -= 1;

        if (reflectionRate > 0.7) score += 2;
        else if (reflectionRate > 0.4) score += 1;

        if (score >= 5) return EffectivenessLevel.HIGH;
        if (score >= 2) return EffectivenessLevel.MODERATE;
        if (score >= 0) return EffectivenessLevel.LOW;
        return EffectivenessLevel.REGRESSING;
    }

    private String buildEffectivenessSummary(EffectivenessLevel level, double icfTrend,
            double adherence, double reflectionRate, double emotionalImpact) {
        return String.format(
            "Nivel: %s | ICF Δ: %+.1f puntos | Adherencia: %.0f%% | " +
            "Tasa reflexión: %.0f%% | Impacto emocional promedio: %.1f/5",
            level, icfTrend, adherence, reflectionRate * 100, emotionalImpact
        );
    }

    // ─── 2. Detección de riesgo de abandono ──────────────────────────────────

    /**
     * Detecta señales tempranas de abandono antes de que la familia desactive.
     * Analiza: inactividad, caída de reflexiones, descenso ICF sostenido.
     */
    private AbandonmentRisk detectAbandonmentRisk(Long familyId) {
        List<FamilyMetricsSnapshot> snapshots = snapshotRepository
                .findByFamilyIdOrderBySnapshotDateAsc(familyId);

        List<Reflection> recentReflections = reflectionRepository.findByFamilyId(familyId)
                .stream()
                .filter(r -> r.getCreatedAt().isAfter(LocalDateTime.now().minusDays(30)))
                .toList();

        List<LearningEntry> recentLearnings = learningEntryRepository.findByFamilyId(familyId)
                .stream()
                .filter(l -> l.getCreatedAt().isAfter(LocalDateTime.now().minusDays(30)))
                .toList();

        List<String> signals = new ArrayList<>();
        int riskScore = 0;

        // Señal 1: Sin actividad en 14+ días
        boolean recentlyActive = !recentReflections.isEmpty() || !recentLearnings.isEmpty();
        if (!recentlyActive) {
            signals.add("INACTIVITY_14D");
            riskScore += 3;
        }

        // Señal 2: Caída de adherencia en snapshots recientes
        if (snapshots.size() >= 3) {
            List<Double> recentAdherence = snapshots.subList(
                    Math.max(0, snapshots.size() - 3), snapshots.size())
                    .stream()
                    .map(FamilyMetricsSnapshot::getAdherence)
                    .filter(Objects::nonNull)
                    .toList();

            if (recentAdherence.size() >= 2) {
                double trend = recentAdherence.get(recentAdherence.size() - 1)
                        - recentAdherence.get(0);
                if (trend < -20) {
                    signals.add("ADHERENCE_DROP_SEVERE");
                    riskScore += 3;
                } else if (trend < -10) {
                    signals.add("ADHERENCE_DROP_MODERATE");
                    riskScore += 2;
                }
            }
        }

        // Señal 3: Reflexiones con intent negativo (no quieren repetir)
        long negativeIntent = recentReflections.stream()
                .filter(r -> Boolean.FALSE.equals(r.getRepeatIntent())).count();
        if (negativeIntent >= 2) {
            signals.add("NEGATIVE_REPEAT_INTENT");
            riskScore += 2;
        }

        // Señal 4: ICF descendente en últimas 2 evaluaciones
        List<Evaluation> recentEvals = evaluationRepository
                .findByFamilyIdOrderByFinalizedAtAsc(familyId)
                .stream()
                .filter(e -> e.getStatus() == EvaluationStatus.FINALIZED && e.getIcf() != null)
                .toList();

        if (recentEvals.size() >= 2) {
            Evaluation prev = recentEvals.get(recentEvals.size() - 2);
            Evaluation last = recentEvals.get(recentEvals.size() - 1);
            if (last.getIcf() < prev.getIcf() - 15) {
                signals.add("ICF_DECLINING");
                riskScore += 2;
            }
        }

        AbandonmentLevel level;
        if (riskScore >= 6) level = AbandonmentLevel.CRITICAL;
        else if (riskScore >= 3) level = AbandonmentLevel.HIGH;
        else if (riskScore >= 1) level = AbandonmentLevel.MODERATE;
        else level = AbandonmentLevel.LOW;

        return new AbandonmentRisk(level, signals, riskScore);
    }

    // ─── 3. Generación de lección aprendida ──────────────────────────────────

    /**
     * Sintetiza una lección operacional concreta a partir del análisis de efectividad.
     * Si la lección es suficientemente sólida, se convierte en candidata a LearnedSkill.
     */
    private Optional<String> generateLessonLearned(Long familyId, InterventionEffectiveness effectiveness) {
        if (effectiveness.level() == EffectivenessLevel.INSUFFICIENT_DATA) return Optional.empty();

        List<Reflection> reflections = reflectionRepository.findByFamilyId(familyId);
        List<LearningEntry> learnings = learningEntryRepository.findByFamilyId(familyId);

        // Construir lección basada en el nivel de efectividad
        String lesson = switch (effectiveness.level()) {
            case HIGH -> buildHighEffectivenessLesson(effectiveness, reflections);
            case MODERATE -> buildModerateEffectivenessLesson(effectiveness, reflections);
            case LOW -> buildLowEffectivenessLesson(effectiveness, learnings);
            case REGRESSING -> buildRegressionLesson(effectiveness);
            default -> null;
        };

        // Proponer nueva skill si la lección es de efectividad alta
        if (lesson != null && effectiveness.level() == EffectivenessLevel.HIGH
                && effectiveness.reflectionRate() > 0.6) {
            proposeSkillFromLesson(familyId, lesson, effectiveness);
        }

        return Optional.ofNullable(lesson);
    }

    private String buildHighEffectivenessLesson(InterventionEffectiveness e, List<Reflection> reflections) {
        long communicationWins = reflections.stream()
                .filter(r -> Boolean.TRUE.equals(r.getCommunicationImproved())).count();
        return String.format(
            "Intervención efectiva detectada: ICF mejoró %+.1f puntos con %.0f%% de adherencia. " +
            "%d reflexiones reportaron mejora en comunicación. " +
            "Patrón consolidado: alta adherencia + reflexiones frecuentes = transformación sostenida.",
            e.icfTrend(), e.avgAdherence(), communicationWins
        );
    }

    private String buildModerateEffectivenessLesson(InterventionEffectiveness e, List<Reflection> reflections) {
        return String.format(
            "Mejora parcial: ICF %+.1f con adherencia del %.0f%%. " +
            "Oportunidad de mejora en frecuencia de reflexión (actual: %.0f%%). " +
            "Recomendación: reducir complejidad de misiones para aumentar completitud.",
            e.icfTrend(), e.avgAdherence(), e.reflectionRate() * 100
        );
    }

    private String buildLowEffectivenessLesson(InterventionEffectiveness e, List<LearningEntry> learnings) {
        boolean mentionsResistance = learnings.stream()
                .anyMatch(l -> l.getBehavioralChange() != null
                        && l.getBehavioralChange().toLowerCase().contains("resistencia"));
        return String.format(
            "Efectividad baja: adherencia del %.0f%% y variación ICF de %+.1f. %s" +
            "Acción correctiva: revisar longitud de tareas y aplicar skill 'micro_missions_high_stress'.",
            e.avgAdherence(), e.icfTrend(),
            mentionsResistance ? "Se detectó resistencia al cambio en bitácora. " : ""
        );
    }

    private String buildRegressionLesson(InterventionEffectiveness e) {
        return String.format(
            "ALERTA DE REGRESIÓN: ICF descendió %+.1f puntos. Adherencia crítica: %.0f%%. " +
            "El sistema debe pausar la intensidad del plan y activar protocolo de reenganche " +
            "(misiones de 1 semana + reflexión emocional guiada).",
            e.icfTrend(), e.avgAdherence()
        );
    }

    private void proposeSkillFromLesson(Long familyId, String lesson, InterventionEffectiveness e) {
        String skillName = "high_effectiveness_cycle_f" + familyId + "_"
                + LocalDateTime.now().getYear();
        if (skillRepository.existsBySkillName(skillName)) return;

        LearnedSkill skill = LearnedSkill.builder()
                .skillName(skillName)
                .description(lesson)
                .conditions(toJson(List.of("adherence_gt_60", "reflection_rate_gt_60")))
                .recommendedStrategy(toJson(List.of(
                        "maintain-current-plan-intensity",
                        "add-celebration-ritual",
                        "increase-reflection-frequency")))
                .dimension("GENERAL")
                .successRate(0.8)
                .reuseCount(e.evaluationCount())
                .successCount(e.evaluationCount())
                .confidence(0.75)
                .createdByAi(true)
                .build();

        skillRepository.save(skill);
        log.info("💡 [REFLECTION] Nueva skill propuesta desde lección: '{}'", skillName);
    }

    // ─── 4. Narrativa identitaria ─────────────────────────────────────────────

    /**
     * Actualiza la narrativa identitaria de la familia con el conocimiento acumulado.
     * Esta narrativa es la "memoria de largo plazo" que el copiloto IA consume.
     */
    @Transactional
    public String updateIdentityNarrative(Long familyId, InterventionEffectiveness effectiveness,
                                          AbandonmentRisk abandonmentRisk) {
        FamilyIdentityProfile profile = identityRepository.findByFamilyId(familyId)
                .orElseGet(() -> {
                    Family family = familyRepository.getReferenceById(familyId);
                    return identityRepository.save(FamilyIdentityProfile.builder().family(family).build());
                });

        List<Evaluation> evals = evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(familyId)
                .stream().filter(e -> e.getIcf() != null).toList();

        double currentIcf = evals.isEmpty() ? 50.0 : evals.get(evals.size() - 1).getIcf();
        String riskLevel = evals.isEmpty() ? "MODERADO"
                : Optional.ofNullable(evals.get(evals.size() - 1).getRiskLevel()).orElse("MODERADO");

        String narrative = buildNarrative(profile, effectiveness, abandonmentRisk, currentIcf, riskLevel);

        profile.setIdentityNarrative(narrative);
        profile.setUpdatedAt(LocalDateTime.now());
        identityRepository.save(profile);

        log.info("📖 [REFLECTION] Narrativa identitaria actualizada para familia ID: {}", familyId);
        return narrative;
    }

    private String buildNarrative(FamilyIdentityProfile profile, InterventionEffectiveness e,
                                   AbandonmentRisk risk, double icf, String riskLevel) {
        StringBuilder narrative = new StringBuilder();
        narrative.append(String.format(
            "Familia en etapa '%s' con %d ciclos completados. ",
            profile.getEvolutionStage(), profile.getCompletedCycles()
        ));

        narrative.append(String.format(
            "ICF actual: %.1f (%s). Estilo comunicación: %s. Expresión emocional: %s. ",
            icf, riskLevel, profile.getCommunicationStyle(), profile.getEmotionalExpression()
        ));

        narrative.append(String.format(
            "Efectividad histórica del sistema: %s (adherencia promedio %.0f%%). ",
            e.level(), e.avgAdherence()
        ));

        if (risk.level() == AbandonmentLevel.HIGH || risk.level() == AbandonmentLevel.CRITICAL) {
            narrative.append(String.format(
                "⚠️ RIESGO DE ABANDONO %s detectado — señales: %s. ",
                risk.level(), String.join(", ", risk.signals())
            ));
        }

        narrative.append(String.format(
            "Adaptabilidad al cambio: %.0f%%. ",
            profile.getAdaptabilityIndex() * 100
        ));

        return narrative.toString().trim();
    }

    // ─── 5. Persistencia de lección como memoria semántica ───────────────────

    private void persistLessonAsSemanticMemory(Long familyId, String lesson,
                                                InterventionEffectiveness effectiveness) {
        Family family = familyRepository.getReferenceById(familyId);

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("type", "lesson_learned");
        content.put("lesson", lesson);
        content.put("effectivenessLevel", effectiveness.level().name());
        content.put("icfTrend", effectiveness.icfTrend());
        content.put("avgAdherence", effectiveness.avgAdherence());
        content.put("generatedAt", LocalDateTime.now().toString());

        double importance = switch (effectiveness.level()) {
            case HIGH -> 0.90;
            case MODERATE -> 0.70;
            case LOW -> 0.60;
            case REGRESSING -> 0.95;
            default -> 0.50;
        };

        memoryRepository.save(FamilyMemory.builder()
                .family(family)
                .memoryType(MemoryType.SEMANTIC)
                .semanticKey("lesson-learned")
                .content(toJson(content))
                .importanceScore(importance)
                .sourceType("REFLECTION_ENGINE")
                .build());
    }

    // ─── Tipos de datos ──────────────────────────────────────────────────────

    public record ReflectionReport(
            Long familyId,
            LocalDateTime generatedAt,
            InterventionEffectiveness effectiveness,
            AbandonmentRisk abandonmentRisk,
            String lessonLearned,
            String updatedNarrative
    ) {
        public boolean requiresUrgentAttention() {
            return abandonmentRisk.level() == AbandonmentLevel.CRITICAL
                || effectiveness.level() == EffectivenessLevel.REGRESSING;
        }
    }

    public record InterventionEffectiveness(
            EffectivenessLevel level,
            int evaluationCount,
            double icfTrend,
            double avgAdherence,
            double reflectionRate,
            String summary
    ) {}

    public record AbandonmentRisk(
            AbandonmentLevel level,
            List<String> signals,
            int riskScore
    ) {}

    public enum EffectivenessLevel { HIGH, MODERATE, LOW, REGRESSING, INSUFFICIENT_DATA }
    public enum AbandonmentLevel   { LOW, MODERATE, HIGH, CRITICAL }

    // ─── Utilidades ──────────────────────────────────────────────────────────

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return obj.toString(); }
    }
}
