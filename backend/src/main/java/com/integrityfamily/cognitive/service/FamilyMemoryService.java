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

/**
 * SDD Fase 1 — Motor de Memoria Cognitiva Familiar.
 *
 * Responsabilidades:
 *  1. Capturar memoria episódica desde evaluaciones, reflexiones y evidencias.
 *  2. Consolidar patrones en memoria semántica (abstracción de episodios repetidos).
 *  3. Registrar y actualizar el perfil de identidad familiar.
 *  4. Proveer contexto cognitivo compacto para el copiloto IA.
 *
 * Este servicio NO llama a la IA — es la capa de datos inteligente que la IA consume.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FamilyMemoryService {

    private final FamilyMemoryRepository memoryRepository;
    private final FamilyIdentityProfileRepository identityRepository;
    private final FamilyRepository familyRepository;
    private final EvaluationRepository evaluationRepository;
    private final ReflectionRepository reflectionRepository;
    private final LearningEntryRepository learningEntryRepository;
    private final ObjectMapper objectMapper;

    // ─── Captura Episódica ────────────────────────────────────────────────────

    /**
     * Captura el resultado de una evaluación como memoria episódica.
     * Invocado automáticamente al finalizar una evaluación.
     */
    @Transactional
    public FamilyMemory captureEvaluationMemory(Evaluation evaluation) {
        log.info("🧠 [MEMORY] Capturando memoria episódica para evaluación ID: {}", evaluation.getId());

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("evaluationId", evaluation.getId());
        content.put("icf", evaluation.getIcf());
        content.put("riskLevel", evaluation.getRiskLevel());
        content.put("criticalDimension", evaluation.getCriticalDimension());
        content.put("hasCrisis", evaluation.getHasCrisis());
        content.put("date", evaluation.getFinalizedAt() != null
                ? evaluation.getFinalizedAt().toString() : LocalDateTime.now().toString());

        if (evaluation.getDimensionScores() != null) {
            Map<String, Double> dims = new LinkedHashMap<>();
            evaluation.getDimensionScores().forEach(ds -> dims.put(ds.getDimensionName(), ds.getScore()));
            content.put("dimensionScores", dims);
        }

        double importance = calculateEpisodeImportance(evaluation);

        FamilyMemory memory = FamilyMemory.builder()
                .family(evaluation.getFamily())
                .memoryType(MemoryType.EPISODIC)
                .semanticKey("evaluation-result")
                .content(toJson(content))
                .importanceScore(importance)
                .sourceType("EVALUATION")
                .sourceId(evaluation.getId())
                .build();

        FamilyMemory saved = memoryRepository.save(memory);
        log.info("✅ [MEMORY] Memoria episódica guardada. ICF: {} | Riesgo: {} | Importancia: {}",
                evaluation.getIcf(), evaluation.getRiskLevel(), importance);

        // Después de capturar el episodio, intentar consolidar patrón semántico
        consolidateSemanticPattern(evaluation.getFamily().getId(), "evaluation-result");

        return saved;
    }

    /**
     * Captura una reflexión como memoria episódica emocional.
     */
    @Transactional
    public FamilyMemory captureReflectionMemory(Reflection reflection) {
        log.info("💭 [MEMORY] Capturando reflexión familia ID: {}", reflection.getFamily().getId());

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("reflectionId", reflection.getId());
        content.put("emotionalImpact", reflection.getEmotionalImpact());
        content.put("communicationImproved", reflection.getCommunicationImproved());
        content.put("learning", reflection.getLearning());
        content.put("repeatIntent", reflection.getRepeatIntent());
        content.put("difficulty", reflection.getDifficulty());
        content.put("date", reflection.getCreatedAt().toString());

        double importance = 0.4;
        if (Boolean.TRUE.equals(reflection.getCommunicationImproved())) importance += 0.2;
        if (reflection.getEmotionalImpact() != null && reflection.getEmotionalImpact() >= 4) importance += 0.2;
        if (Boolean.FALSE.equals(reflection.getRepeatIntent())) importance += 0.1; // abandono = señal importante

        FamilyMemory memory = FamilyMemory.builder()
                .family(reflection.getFamily())
                .memoryType(MemoryType.EPISODIC)
                .semanticKey("reflection-emotional")
                .content(toJson(content))
                .importanceScore(Math.min(1.0, importance))
                .sourceType("REFLECTION")
                .sourceId(reflection.getId())
                .build();

        return memoryRepository.save(memory);
    }

    /**
     * Captura un aprendizaje conductual como memoria episódica.
     */
    @Transactional
    public FamilyMemory captureLearningMemory(LearningEntry entry) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("entryId", entry.getId());
        content.put("behavioralChange", entry.getBehavioralChange());
        content.put("awarenessShift", entry.getAwarenessShift());
        content.put("date", entry.getCreatedAt().toString());

        FamilyMemory memory = FamilyMemory.builder()
                .family(entry.getFamily())
                .memoryType(MemoryType.EPISODIC)
                .semanticKey("learning-behavioral")
                .content(toJson(content))
                .importanceScore(0.6) // los cambios conductuales son siempre relevantes
                .sourceType("LEARNING_ENTRY")
                .sourceId(entry.getId())
                .build();

        return memoryRepository.save(memory);
    }

    /**
     * Captura una nueva entrada de bitácora como memoria episódica de situación abierta.
     * Importancia media: el sistema conoce la dificultad pero aún no hay resolución.
     */
    @Transactional
    public FamilyMemory captureLogbookOpenMemory(FamilyLogbookEntry entry) {
        log.info("📓 [MEMORY] Capturando situación abierta de bitácora ID: {} para familia ID: {}",
                entry.getId(), entry.getFamily().getId());

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("entryId",            entry.getId());
        content.put("situation",          entry.getSituation());
        content.put("difficultyDetected", entry.getDifficultyDetected());
        content.put("emotionIdentified",  entry.getEmotionIdentified());
        content.put("familyAgreement",    entry.getFamilyAgreement());
        content.put("status",             "OPEN");
        content.put("createdBy",          entry.getCreatedBy());
        content.put("date",               entry.getCreatedAt().toString());

        FamilyMemory memory = FamilyMemory.builder()
                .family(entry.getFamily())
                .memoryType(MemoryType.EPISODIC)
                .semanticKey("logbook-open")
                .content(toJson(content))
                .importanceScore(0.45) // situación detectada, aún sin resolver
                .sourceType("LOGBOOK")
                .sourceId(entry.getId())
                .build();

        return memoryRepository.save(memory);
    }

    /**
     * Captura la resolución de una bitácora como memoria episódica de alta importancia.
     * La evidencia de avance + el acuerdo familiar son los datos más valiosos del sistema.
     * Si hay ≥3 resoluciones previas, también consolida un patrón semántico de "acuerdos familiares".
     */
    @Transactional
    public FamilyMemory captureLogbookResolutionMemory(FamilyLogbookEntry entry) {
        log.info("✅ [MEMORY] Capturando resolución de bitácora ID: {} para familia ID: {}",
                entry.getId(), entry.getFamily().getId());

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("entryId",          entry.getId());
        content.put("situation",        entry.getSituation());
        content.put("familyAgreement",  entry.getFamilyAgreement());
        content.put("correctionAction", entry.getCorrectionAction());
        content.put("understanding",    entry.getUnderstanding());
        content.put("progressEvidence", entry.getProgressEvidence());
        content.put("resolvedBy",       entry.getResolvedBy());
        content.put("status",           "RESOLVED");
        content.put("date",             entry.getResolvedAt() != null
                                            ? entry.getResolvedAt().toString()
                                            : LocalDateTime.now().toString());

        // Resoluciones = crecimiento demostrado → alta importancia
        FamilyMemory memory = FamilyMemory.builder()
                .family(entry.getFamily())
                .memoryType(MemoryType.EPISODIC)
                .semanticKey("logbook-resolution")
                .content(toJson(content))
                .importanceScore(0.85)
                .sourceType("LOGBOOK")
                .sourceId(entry.getId())
                .build();

        FamilyMemory saved = memoryRepository.save(memory);

        // Intentar consolidar patrón semántico si hay historial de acuerdos
        consolidateSemanticPattern(entry.getFamily().getId(), "logbook-resolution");

        log.info("📌 [MEMORY] Acuerdo familiar capturado: \"{}\"",
                entry.getFamilyAgreement() != null
                    ? entry.getFamilyAgreement().substring(0, Math.min(60, entry.getFamilyAgreement().length()))
                    : "—");

        return saved;
    }

    // ─── Consolidación Semántica ──────────────────────────────────────────────

    /**
     * Analiza episodios repetidos y consolida un patrón semántico si hay suficiente evidencia.
     * Un patrón se consolida cuando hay ≥3 episodios con el mismo semanticKey y tendencia similar.
     */
    @Transactional
    public void consolidateSemanticPattern(Long familyId, String semanticKey) {
        List<FamilyMemory> episodes = memoryRepository
                .findByFamilyIdAndSemanticKeyOrderByCreatedAtDesc(familyId, semanticKey);

        if (episodes.size() < 3) return; // no hay suficiente evidencia todavía

        // Tomar los últimos 5 episodios para el análisis
        List<FamilyMemory> recent = episodes.subList(0, Math.min(5, episodes.size()));

        // Detectar tendencia en evaluaciones
        if ("evaluation-result".equals(semanticKey)) {
            detectAndSaveEvaluationPattern(familyId, recent);
        }

        log.debug("🔄 [MEMORY] Consolidación semántica ejecutada para familia {} / {}", familyId, semanticKey);
    }

    private void detectAndSaveEvaluationPattern(Long familyId, List<FamilyMemory> recentEpisodes) {
        try {
            List<Double> icfValues = new ArrayList<>();
            List<String> criticalDims = new ArrayList<>();

            for (FamilyMemory mem : recentEpisodes) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = objectMapper.readValue(mem.getContent(), Map.class);
                if (data.get("icf") != null) icfValues.add(((Number) data.get("icf")).doubleValue());
                if (data.get("criticalDimension") != null) criticalDims.add((String) data.get("criticalDimension"));
            }

            if (icfValues.isEmpty()) return;

            double avgIcf = icfValues.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            boolean improving = icfValues.size() >= 2 && icfValues.get(0) > icfValues.get(icfValues.size() - 1);

            // Dimensión crítica más recurrente
            String dominantDimension = criticalDims.stream()
                    .filter(Objects::nonNull)
                    .reduce((a, b) -> a.equals(b) ? a : b)
                    .orElse("unknown");

            Family family = familyRepository.getReferenceById(familyId);
            Map<String, Object> patternContent = new LinkedHashMap<>();
            patternContent.put("pattern", "longitudinal-evaluation-trend");
            patternContent.put("averageIcf", Math.round(avgIcf * 10.0) / 10.0);
            patternContent.put("trend", improving ? "IMPROVING" : "STABLE_OR_DECLINING");
            patternContent.put("dominantCriticalDimension", dominantDimension);
            patternContent.put("episodeCount", recentEpisodes.size());
            patternContent.put("consolidatedAt", LocalDateTime.now().toString());

            // Actualizar o crear la memoria semántica de tendencia
            List<FamilyMemory> existing = memoryRepository
                    .findByFamilyIdAndSemanticKeyOrderByCreatedAtDesc(familyId, "evaluation-trend-pattern");

            if (!existing.isEmpty()) {
                FamilyMemory semantic = existing.get(0);
                semantic.setContent(toJson(patternContent));
                semantic.setUpdatedAt(LocalDateTime.now());
                semantic.setImportanceScore(0.85);
                memoryRepository.save(semantic);
            } else {
                memoryRepository.save(FamilyMemory.builder()
                        .family(family)
                        .memoryType(MemoryType.SEMANTIC)
                        .semanticKey("evaluation-trend-pattern")
                        .content(toJson(patternContent))
                        .importanceScore(0.85)
                        .sourceType("AI_CONSOLIDATION")
                        .build());
            }

            log.info("📊 [MEMORY] Patrón semántico consolidado: ICF promedio={}, tendencia={}, dim crítica={}",
                    avgIcf, improving ? "MEJORA" : "ESTABLE/DECLIVE", dominantDimension);

        } catch (Exception e) {
            log.warn("⚠️ [MEMORY] Error consolidando patrón semántico: {}", e.getMessage());
        }
    }

    // ─── Perfil de Identidad ─────────────────────────────────────────────────

    /**
     * Obtiene o crea el perfil de identidad de la familia.
     */
    @Transactional
    public FamilyIdentityProfile getOrCreateIdentityProfile(Long familyId) {
        return identityRepository.findByFamilyId(familyId).orElseGet(() -> {
            log.info("🆕 [IDENTITY] Creando perfil de identidad para familia ID: {}", familyId);
            Family family = familyRepository.getReferenceById(familyId);
            return identityRepository.save(FamilyIdentityProfile.builder()
                    .family(family)
                    .build());
        });
    }

    /**
     * Actualiza el perfil identitario basado en los datos más recientes de la familia.
     * Invocado tras cada ciclo completo (evaluación + plan + evidencias + reflexión).
     */
    @Transactional
    public FamilyIdentityProfile updateIdentityProfile(Long familyId) {
        FamilyIdentityProfile profile = getOrCreateIdentityProfile(familyId);

        List<Reflection> reflections = reflectionRepository.findByFamilyId(familyId);
        List<LearningEntry> learnings = learningEntryRepository.findByFamilyId(familyId);

        // Calcular adaptabilidad basada en adherencia histórica a reflexiones
        if (!reflections.isEmpty()) {
            long completed = reflections.stream()
                    .filter(r -> r.getStatus() == ReflectionStatus.COMPLETED)
                    .count();
            double adaptability = (double) completed / reflections.size();
            profile.setAdaptabilityIndex(Math.round(adaptability * 100.0) / 100.0);

            // Inferir expresión emocional desde impacto emocional promedio
            double avgImpact = reflections.stream()
                    .filter(r -> r.getEmotionalImpact() != null)
                    .mapToInt(Reflection::getEmotionalImpact)
                    .average().orElse(3.0);

            if (avgImpact >= 4.0) profile.setEmotionalExpression("HIGH");
            else if (avgImpact >= 2.5) profile.setEmotionalExpression("MEDIUM");
            else profile.setEmotionalExpression("LOW");
        }

        // Registrar ciclo si hay suficiente actividad (reflexión + aprendizaje)
        if (!reflections.isEmpty() && !learnings.isEmpty()) {
            profile.registerCompletedCycle();
        }

        profile.setUpdatedAt(LocalDateTime.now());
        FamilyIdentityProfile saved = identityRepository.save(profile);
        log.info("✅ [IDENTITY] Perfil actualizado. Ciclos: {} | Etapa: {} | Adaptabilidad: {}",
                saved.getCompletedCycles(), saved.getEvolutionStage(), saved.getAdaptabilityIndex());
        return saved;
    }

    // ─── Contexto Cognitivo para IA ──────────────────────────────────────────

    /**
     * Construye el contexto cognitivo compacto que el copiloto IA consume.
     * Prioriza memorias de alta importancia y evita ruido.
     */
    @Transactional(readOnly = true)
    public CognitiveContext buildCognitiveContext(Long familyId) {
        List<FamilyMemory> activeMemories = memoryRepository
                .findActiveMemoriesByFamilyId(familyId, LocalDateTime.now());

        List<FamilyMemory> semanticPatterns = activeMemories.stream()
                .filter(m -> m.getMemoryType() == MemoryType.SEMANTIC)
                .toList();

        List<FamilyMemory> recentEpisodes = activeMemories.stream()
                .filter(m -> m.getMemoryType() == MemoryType.EPISODIC)
                .limit(5)
                .toList();

        FamilyIdentityProfile identity = identityRepository.findByFamilyId(familyId).orElse(null);

        return new CognitiveContext(familyId, recentEpisodes, semanticPatterns, identity);
    }

    public record CognitiveContext(
            Long familyId,
            List<FamilyMemory> recentEpisodes,
            List<FamilyMemory> semanticPatterns,
            FamilyIdentityProfile identityProfile
    ) {
        public boolean hasIdentity() { return identityProfile != null; }
        public boolean hasPatterns() { return !semanticPatterns.isEmpty(); }
        public String evolutionStage() {
            return identityProfile != null ? identityProfile.getEvolutionStage() : "INITIAL";
        }
    }

    // ─── Utilidades ──────────────────────────────────────────────────────────

    private double calculateEpisodeImportance(Evaluation evaluation) {
        double importance = 0.5;
        if (evaluation.getIcf() != null) {
            if (evaluation.getIcf() < 40) importance = 0.95; // crisis
            else if (evaluation.getIcf() < 60) importance = 0.80;
            else if (evaluation.getIcf() >= 80) importance = 0.70;
        }
        if (Boolean.TRUE.equals(evaluation.getHasCrisis())) importance = 1.0;
        return importance;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }
}
