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
import java.util.*;
import java.util.stream.Collectors;

/**
 * SDD Fase 2 — Motor de Habilidades Cognitivas Familiares.
 *
 * Responsabilidades:
 *  1. Detectar qué condiciones familiares activan habilidades existentes y aplicarlas.
 *  2. Extraer nuevas habilidades candidatas de ciclos exitosos (reflexión + adherencia alta).
 *  3. Actualizar métricas de confianza de skills según resultados reales.
 *  4. Registrar skills activas como memoria procedural en FamilyMemory.
 *
 * Flujo:
 *  Evaluación → Ciclo completado → FamilySkillEngine.analyze()
 *    → matchAndApplySkills()   (skills existentes que aplican a esta familia ahora)
 *    → extractNewSkillCandidate() (si hay patrón exitoso emergente)
 *    → updateSkillMetrics()    (retroalimentación sobre skills aplicadas antes)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FamilySkillEngine {

    private static final double MIN_CONFIDENCE_TO_APPLY = 0.55;
    private static final int MIN_EPISODES_FOR_NEW_SKILL = 3;

    private final LearnedSkillRepository skillRepository;
    private final FamilyMemoryRepository memoryRepository;
    private final FamilyIdentityProfileRepository identityRepository;
    private final ReflectionRepository reflectionRepository;
    private final LearningEntryRepository learningEntryRepository;
    private final FamilyMetricsSnapshotRepository snapshotRepository;
    private final FamilyRepository familyRepository;
    private final ObjectMapper objectMapper;

    // ─── Punto de entrada principal ──────────────────────────────────────────

    /**
     * Analiza el estado actual de la familia y ejecuta el ciclo completo del motor.
     * Llamado automáticamente al finalizar cada evaluación.
     */
    @Transactional
    public SkillEngineResult analyze(Long familyId, Evaluation evaluation) {
        log.info("⚙️ [SKILL-ENGINE] Iniciando análisis cognitivo para familia ID: {}", familyId);

        FamilyConditions conditions = buildConditions(familyId, evaluation);
        List<LearnedSkill> applied = matchAndApplySkills(familyId, conditions);
        Optional<LearnedSkill> newSkill = extractNewSkillCandidate(familyId, conditions);
        updateSkillMetrics(familyId, conditions);

        log.info("✅ [SKILL-ENGINE] Skills aplicadas: {} | Nueva skill candidata: {}",
                applied.size(), newSkill.map(LearnedSkill::getSkillName).orElse("ninguna"));

        return new SkillEngineResult(applied, newSkill.orElse(null), conditions);
    }

    // ─── 1. Construcción de condiciones familiares ────────────────────────────

    /**
     * Construye el perfil de condiciones actuales de la familia para matching con skills.
     */
    private FamilyConditions buildConditions(Long familyId, Evaluation evaluation) {
        List<FamilyMetricsSnapshot> snapshots = snapshotRepository
                .findByFamilyIdOrderBySnapshotDateAsc(familyId);

        double adherence = snapshots.isEmpty() ? 50.0
                : snapshots.get(snapshots.size() - 1).getAdherence() != null
                ? snapshots.get(snapshots.size() - 1).getAdherence() : 50.0;

        double icf = evaluation.getIcf() != null ? evaluation.getIcf() : 50.0;
        String criticalDim = evaluation.getCriticalDimension() != null
                ? evaluation.getCriticalDimension().toUpperCase() : "UNKNOWN";
        boolean hasCrisis = Boolean.TRUE.equals(evaluation.getHasCrisis());

        // Detectar tendencia ICF
        String trend = "STABLE";
        if (snapshots.size() >= 2) {
            Double prev = snapshots.get(snapshots.size() - 2).getConvivenceIndex();
            Double curr = snapshots.get(snapshots.size() - 1).getConvivenceIndex();
            if (prev != null && curr != null) {
                if (curr > prev + 5) trend = "IMPROVING";
                else if (curr < prev - 5) trend = "DECLINING";
            }
        }

        FamilyIdentityProfile identity = identityRepository.findByFamilyId(familyId).orElse(null);
        String evolutionStage = identity != null ? identity.getEvolutionStage() : "INITIAL";
        double adaptability = identity != null ? identity.getAdaptabilityIndex() : 0.5;

        // Score de comunicación desde la evaluación actual
        double communicationScore = evaluation.getDimensionScores() != null
                ? evaluation.getDimensionScores().stream()
                    .filter(ds -> "comunicacion".equalsIgnoreCase(ds.getDimensionName()))
                    .mapToDouble(EvaluationDimensionScore::getScore)
                    .findFirst().orElse(50.0)
                : 50.0;

        return new FamilyConditions(
                familyId, icf, adherence, communicationScore,
                criticalDim, hasCrisis, trend, evolutionStage, adaptability
        );
    }

    // ─── 2. Matching y aplicación de skills existentes ───────────────────────

    /**
     * Evalúa qué skills de alta confianza aplican a las condiciones actuales
     * y las registra como memoria procedural activa.
     */
    private List<LearnedSkill> matchAndApplySkills(Long familyId, FamilyConditions c) {
        List<LearnedSkill> highConfidence = skillRepository.findHighConfidenceSkills(MIN_CONFIDENCE_TO_APPLY);
        List<LearnedSkill> matched = new ArrayList<>();
        Family family = familyRepository.getReferenceById(familyId);

        for (LearnedSkill skill : highConfidence) {
            if (skillMatchesConditions(skill, c)) {
                matched.add(skill);
                registerSkillAsProceduralMemory(family, skill, c);
                log.info("🎯 [SKILL-ENGINE] Skill activada: '{}' (confianza: {})",
                        skill.getSkillName(), skill.getConfidence());
            }
        }

        return matched;
    }

    /**
     * Evalúa si una skill aplica dado el estado actual de la familia.
     * Las condiciones son JSON array — se parsean y evalúan semánticamente.
     */
    private boolean skillMatchesConditions(LearnedSkill skill, FamilyConditions c) {
        try {
            @SuppressWarnings("unchecked")
            List<String> conditions = objectMapper.readValue(skill.getConditions(), List.class);

            int matched = 0;
            for (String condition : conditions) {
                if (evaluateCondition(condition, c)) matched++;
            }
            // La skill aplica si cumple al menos la mitad de sus condiciones
            return matched >= Math.ceil(conditions.size() / 2.0);
        } catch (Exception e) {
            log.warn("⚠️ [SKILL-ENGINE] Error evaluando condiciones de skill '{}': {}",
                    skill.getSkillName(), e.getMessage());
            return false;
        }
    }

    private boolean evaluateCondition(String condition, FamilyConditions c) {
        return switch (condition) {
            case "low_adherence"             -> c.adherence() < 40;
            case "high_fatigue"              -> c.adherence() < 50 && c.icf() < 60;
            case "tasks_too_long"            -> c.adaptability() < 0.4;
            case "communication_score_lt_40" -> c.communicationScore() < 40;
            case "conflict_style_avoidant"   -> "AVOIDANT".equals(c.conflictStyle());
            case "high_stress_triggers"      -> c.hasCrisis() || c.icf() < 45;
            case "evolution_stage_consolidation" -> "CONSOLIDATION".equals(c.evolutionStage());
            case "adherence_gt_60"           -> c.adherence() > 60;
            case "completed_cycles_gte_3"    -> !"INITIAL".equals(c.evolutionStage())
                                                && !"RECOGNITION".equals(c.evolutionStage());
            default                          -> false;
        };
    }

    private void registerSkillAsProceduralMemory(Family family, LearnedSkill skill, FamilyConditions c) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("skillName", skill.getSkillName());
        content.put("description", skill.getDescription());
        content.put("recommendedStrategy", parseJson(skill.getRecommendedStrategy()));
        content.put("confidence", skill.getConfidence());
        content.put("activatedAt", LocalDateTime.now().toString());
        content.put("activatingConditions", Map.of(
                "icf", c.icf(),
                "adherence", c.adherence(),
                "criticalDimension", c.criticalDimension()
        ));

        // Expira en 30 días — si la familia supera las condiciones, la skill deja de ser relevante
        memoryRepository.save(FamilyMemory.builder()
                .family(family)
                .memoryType(MemoryType.PROCEDURAL)
                .semanticKey("active-skill:" + skill.getSkillName())
                .content(toJson(content))
                .importanceScore(skill.getConfidence())
                .sourceType("SKILL_ENGINE")
                .sourceId(skill.getId())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build());
    }

    // ─── 3. Extracción de nuevas habilidades candidatas ──────────────────────

    /**
     * Analiza el historial reciente de la familia y propone una nueva skill
     * si detecta un patrón de éxito repetido que aún no está capturado.
     */
    private Optional<LearnedSkill> extractNewSkillCandidate(Long familyId, FamilyConditions c) {
        List<Reflection> reflections = reflectionRepository.findByFamilyId(familyId);
        List<LearningEntry> learnings = learningEntryRepository.findByFamilyId(familyId);

        if (reflections.size() < MIN_EPISODES_FOR_NEW_SKILL) return Optional.empty();

        // Analizar reflexiones exitosas recientes (impacto alto + intent de repetir)
        List<Reflection> successfulReflections = reflections.stream()
                .filter(r -> r.getStatus() == ReflectionStatus.COMPLETED)
                .filter(r -> Boolean.TRUE.equals(r.getRepeatIntent()))
                .filter(r -> r.getEmotionalImpact() != null && r.getEmotionalImpact() >= 4)
                .sorted(Comparator.comparing(Reflection::getCreatedAt).reversed())
                .limit(10)
                .toList();

        if (successfulReflections.size() < MIN_EPISODES_FOR_NEW_SKILL) return Optional.empty();

        // Detectar patrón: ¿hay una dimensión con comunicación mejorada consistentemente?
        long communicationSuccesses = successfulReflections.stream()
                .filter(r -> Boolean.TRUE.equals(r.getCommunicationImproved()))
                .count();

        double successRate = (double) communicationSuccesses / successfulReflections.size();

        // Sólo proponer si el patrón es sólido (>60% de éxito)
        if (successRate < 0.6) return Optional.empty();

        // Generar nombre semántico de la skill basado en las condiciones detectadas
        String skillName = buildSkillName(c, successfulReflections);

        // No duplicar si ya existe
        if (skillRepository.existsBySkillName(skillName)) return Optional.empty();

        // Construir la nueva skill
        List<String> conditions = buildConditionsList(c);
        List<String> strategy = buildStrategyFromLearnings(learnings, successfulReflections);

        LearnedSkill newSkill = LearnedSkill.builder()
                .skillName(skillName)
                .description(buildSkillDescription(c, successRate))
                .conditions(toJson(conditions))
                .recommendedStrategy(toJson(strategy))
                .dimension(c.criticalDimension())
                .successRate(successRate)
                .reuseCount(successfulReflections.size())
                .successCount((int) communicationSuccesses)
                .confidence(Math.min(0.75, 0.5 + successRate * 0.3))
                .createdByAi(true)
                .build();

        LearnedSkill saved = skillRepository.save(newSkill);
        log.info("🌱 [SKILL-ENGINE] Nueva habilidad extraída: '{}' (tasa de éxito: {}%)",
                skillName, Math.round(successRate * 100));
        return Optional.of(saved);
    }

    private String buildSkillName(FamilyConditions c, List<Reflection> successes) {
        String dimPart = c.criticalDimension().toLowerCase().replace(" ", "_");
        String stagePart = c.evolutionStage().toLowerCase();
        boolean lowAdherence = c.adherence() < 50;
        return (lowAdherence ? "recovery_" : "reinforce_") + dimPart + "_" + stagePart;
    }

    private List<String> buildConditionsList(FamilyConditions c) {
        List<String> conds = new ArrayList<>();
        if (c.adherence() < 50) conds.add("low_adherence");
        if (c.communicationScore() < 40) conds.add("communication_score_lt_40");
        if (c.hasCrisis()) conds.add("high_stress_triggers");
        if ("CONSOLIDATION".equals(c.evolutionStage()) || "ADJUSTMENT".equals(c.evolutionStage()))
            conds.add("evolution_stage_" + c.evolutionStage().toLowerCase());
        if (conds.isEmpty()) conds.add("general_improvement_cycle");
        return conds;
    }

    private List<String> buildStrategyFromLearnings(List<LearningEntry> learnings,
                                                     List<Reflection> successes) {
        List<String> strategy = new ArrayList<>();

        // Si los aprendizajes mencionan patrones clave, incluirlos
        boolean mentionsMicro = learnings.stream()
                .anyMatch(l -> l.getBehavioralChange() != null &&
                        l.getBehavioralChange().toLowerCase().contains("pequeño"));
        boolean mentionsDialogue = successes.stream()
                .anyMatch(r -> r.getLearning() != null &&
                        r.getLearning().toLowerCase().contains("diálogo"));

        if (mentionsMicro) strategy.add("micro-task-fragmentation");
        if (mentionsDialogue) strategy.add("structured-guided-dialogue");
        strategy.add("positive-reinforcement-after-completion");
        strategy.add("short-weekly-reflection");
        return strategy;
    }

    private String buildSkillDescription(FamilyConditions c, double successRate) {
        return String.format(
            "Patrón detectado en familia con dimensión crítica '%s' (ICF: %.1f, adherencia: %.1f%%). " +
            "Reflecciones exitosas con tasa de éxito del %.0f%%. Estrategia inferida del historial real.",
            c.criticalDimension(), c.icf(), c.adherence(), successRate * 100
        );
    }

    // ─── 4. Retroalimentación de métricas ────────────────────────────────────

    /**
     * Actualiza las métricas de las skills que fueron aplicadas en ciclos anteriores,
     * evaluando si produjeron mejora medible en esta evaluación.
     */
    private void updateSkillMetrics(Long familyId, FamilyConditions current) {
        // Buscar skills procedurales activas de ciclos anteriores
        List<FamilyMemory> previouslyApplied = memoryRepository
                .findProceduralMemories(familyId)
                .stream()
                .filter(m -> m.getSourceType() != null && m.getSourceType().equals("SKILL_ENGINE"))
                .filter(m -> m.getCreatedAt().isBefore(LocalDateTime.now().minusDays(1)))
                .toList();

        for (FamilyMemory mem : previouslyApplied) {
            try {
                String skillNameKey = mem.getSemanticKey();
                if (skillNameKey == null || !skillNameKey.startsWith("active-skill:")) continue;
                String skillName = skillNameKey.substring("active-skill:".length());

                skillRepository.findBySkillName(skillName).ifPresent(skill -> {
                    // Si el ICF mejoró o la adherencia subió → éxito
                    boolean improved = current.icf() > 55 && current.adherence() > 50
                                    && current.trend().equals("IMPROVING");
                    if (improved) {
                        skill.recordSuccess();
                    } else {
                        skill.recordFailure();
                    }
                    skillRepository.save(skill);
                    log.debug("📊 [SKILL-ENGINE] Métricas actualizadas para '{}': éxito={}, confianza={}",
                            skillName, improved, skill.getConfidence());
                });
            } catch (Exception e) {
                log.warn("⚠️ [SKILL-ENGINE] Error actualizando métrica de skill: {}", e.getMessage());
            }
        }
    }

    // ─── Tipos de datos ──────────────────────────────────────────────────────

    public record FamilyConditions(
            Long familyId,
            double icf,
            double adherence,
            double communicationScore,
            String criticalDimension,
            boolean hasCrisis,
            String trend,
            String evolutionStage,
            double adaptability
    ) {
        public String conflictStyle() { return null; } // se poblará en Fase 3 desde FamilyIdentityProfile
    }

    public record SkillEngineResult(
            List<LearnedSkill> appliedSkills,
            LearnedSkill newSkillExtracted,
            FamilyConditions conditions
    ) {
        public boolean hasNewSkill() { return newSkillExtracted != null; }
        public boolean hasAppliedSkills() { return !appliedSkills.isEmpty(); }
    }

    // ─── Utilidades ──────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Object parseJson(String json) {
        try { return objectMapper.readValue(json, List.class); }
        catch (Exception e) { return json; }
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return obj.toString(); }
    }
}
