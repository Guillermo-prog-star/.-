package com.integrityfamily.ai.service;

import com.integrityfamily.ai.dto.DimensionCorrelation;
import com.integrityfamily.ai.dto.LogbookCorrelationResult;
import com.integrityfamily.ai.dto.SentimentResult;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.ai.service.ContextSynthesizer;
import com.integrityfamily.ai.dto.AiContext;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SDD: Sentiment Analysis and Clinical Correlation Engine
 * Objetivo: Procesar logs de bitácora y correlacionarlos con el estado de diagnóstico psicométrico.
 */
@Slf4j
@Service
public class SentimentAnalysisService {

    private final FamilyRepository familyRepository;
    private final FamilyLogbookEntryRepository logbookRepository;
    private final EvaluationRepository evaluationRepository;
    private final AiProvider aiProvider;
    private final ContextSynthesizer contextSynthesizer;

    public SentimentAnalysisService(
            FamilyRepository familyRepository,
            FamilyLogbookEntryRepository logbookRepository,
            EvaluationRepository evaluationRepository,
            AiProvider aiProvider,
            ContextSynthesizer contextSynthesizer
    ) {
        this.familyRepository = familyRepository;
        this.logbookRepository = logbookRepository;
        this.evaluationRepository = evaluationRepository;
        this.aiProvider = aiProvider;
        this.contextSynthesizer = contextSynthesizer;
    }

    /**
     * Analiza un bloque de texto básico (Método legado mantenido para compatibilidad de firma)
     */
    public SentimentResult analyze(String text) {
        double score = calculateSpanishVaderScore(text);
        return SentimentResult.builder()
                .text(text)
                .score(score)
                .label(determineLabel(score))
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Motor Central de Correlación Multidimensional (v5.0 Premium)
     * Cruza el estado emocional de la bitácora con los puntajes diagnósticos de la última evaluación.
     */
    public LogbookCorrelationResult correlateFamilySentiment(Long familyId) {
        log.info("🧠 [SENTIMENT-CORRELATION] Procesando correlación de bitácora para Familia ID: {}", familyId);

        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new IllegalArgumentException("Familia no encontrada para ID: " + familyId));

        // 1. Recuperar todas las entradas de bitácora de la familia
        List<FamilyLogbookEntry> entries = logbookRepository.findByFamilyIdOrderByCreatedAtDesc(familyId);

        // 2. Recuperar la última evaluación finalizada
        Optional<Evaluation> lastEvalOpt = evaluationRepository.findFirstByFamilyIdAndStatusOrderByFinalizedAtDesc(familyId, EvaluationStatus.FINALIZED);

        if (entries.isEmpty()) {
            return buildEmptyCorrelationResult(family, lastEvalOpt);
        }

        // 3. Agrupar y clasificar las entradas por dimensión y calcular su promedio emocional
        Map<String, List<FamilyLogbookEntry>> entriesByDimension = new HashMap<>();
        double globalScoreSum = 0.0;

        for (FamilyLogbookEntry entry : entries) {
            String dimension = classifyEntryDimension(entry);
            entriesByDimension.computeIfAbsent(dimension, k -> new ArrayList<>()).add(entry);
            
            // Analizar la valencia sumando todos los campos de texto narrativo de la bitácora
            double entrySentiment = calculateSpanishVaderScore(
                    entry.getSituation(),
                    entry.getDifficultyDetected(),
                    entry.getEmotionIdentified(),
                    entry.getUnderstanding(),
                    entry.getCorrectionAction(),
                    entry.getFamilyAgreement()
            );
            globalScoreSum += entrySentiment;
        }

        double averageGlobalSentiment = globalScoreSum / entries.size();

        // Map de puntajes de diagnóstico por dimensión de la última evaluación (por defecto 50.0 si no hay evaluación)
        Map<String, Double> diagnosticScores = new HashMap<>();
        if (lastEvalOpt.isPresent()) {
            Evaluation eval = lastEvalOpt.get();
            for (EvaluationDimensionScore ds : eval.getDimensionScores()) {
                diagnosticScores.put(ds.getDimensionName().toLowerCase(), ds.getScore());
            }
        }

        // 4. Calcular correlaciones dimensionales cruzadas para las 4 dimensiones principales
        String[] coreDimensions = {"comunicacion", "emociones", "habitos", "tiempos"};
        List<DimensionCorrelation> correlations = new ArrayList<>();

        for (String dim : coreDimensions) {
            List<FamilyLogbookEntry> dimEntries = entriesByDimension.getOrDefault(dim, new ArrayList<>());
            double dimSentimentAvg = 0.0;

            if (!dimEntries.isEmpty()) {
                double dimSum = 0.0;
                for (FamilyLogbookEntry de : dimEntries) {
                    dimSum += calculateSpanishVaderScore(
                            de.getSituation(), de.getDifficultyDetected(), de.getEmotionIdentified(),
                            de.getUnderstanding(), de.getCorrectionAction(), de.getFamilyAgreement()
                    );
                }
                dimSentimentAvg = dimSum / dimEntries.size();
            }

            double diagScore = diagnosticScores.getOrDefault(dim, 50.0);

            // Calcular Delta de Discrepancia: Normalizamos el sentimiento de [-1.0, 1.0] a [0.0, 100.0] para alinearlo con el score
            double normalizedSentiment = ((dimSentimentAvg + 1.0) / 2.0) * 100.0;
            double delta = diagScore - normalizedSentiment;

            // Alerta si el diagnóstico es alto pero el registro de vivencias reales es crítico (Sesgo o Deterioro)
            boolean priorityShift = diagScore > 70.0 && dimSentimentAvg < -0.25;

            correlations.add(DimensionCorrelation.builder()
                    .dimensionName(dim)
                    .dimensionFriendlyName(getDimensionFriendlyName(dim))
                    .diagnosticScore(diagScore)
                    .logbookSentimentScore(dimSentimentAvg)
                    .correlationDelta(delta)
                    .requiresPriorityShift(priorityShift)
                    .build());
        }

        // 5. Formular Recomendación de Calibración de Plan (Dinámica con Claude o Fallback Estático)
        String recommendation = generateDynamicClaudeRecommendation(family, entries, averageGlobalSentiment, correlations);

        return LogbookCorrelationResult.builder()
                .familyId(familyId)
                .familyName(family.getName())
                .totalEntriesAnalyzed(entries.size())
                .averageEmotionalScore(averageGlobalSentiment)
                .generalLabel(determineLabel(averageGlobalSentiment))
                .dimensionCorrelations(correlations)
                .adaptationRecommendation(recommendation)
                .calculatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Motor Cualitativo Inteligente: Envía los registros narrativos de bitácora y las correlaciones a Claude
     * para generar un informe de análisis psicoclínico empático y una calibración dinámica del plan de 36 meses.
     */
    private String generateDynamicClaudeRecommendation(
            Family family, 
            List<FamilyLogbookEntry> entries, 
            double globalSentiment, 
            List<DimensionCorrelation> correlations
    ) {
        try {
            log.info("🧠 [SENTIMENT-CLAUDE] Iniciando análisis clínico cualitativo de bitácoras con Claude para Familia: {}", family.getId());
            
            // 1. Sintetizar contexto del hogar
            AiContext context = contextSynthesizer.synthesize(family, "ANALYSIS");

            // 2. Construir histórico cualitativo de las últimas 5 entradas
            StringBuilder logbookText = new StringBuilder();
            int limit = Math.min(5, entries.size());
            for (int i = 0; i < limit; i++) {
                FamilyLogbookEntry entry = entries.get(i);
                logbookText.append(String.format("- Entrada %d (%s):\n", i + 1, entry.getCreatedAt() != null ? entry.getCreatedAt().toLocalDate() : "Fecha N/A"));
                logbookText.append(String.format("  * Situación: \"%s\"\n", entry.getSituation() != null ? entry.getSituation() : "N/A"));
                logbookText.append(String.format("  * Dificultad: \"%s\"\n", entry.getDifficultyDetected() != null ? entry.getDifficultyDetected() : "N/A"));
                logbookText.append(String.format("  * Emoción: \"%s\"\n", entry.getEmotionIdentified() != null ? entry.getEmotionIdentified() : "N/A"));
                logbookText.append(String.format("  * Comprensión/Aprendizaje: \"%s\"\n", entry.getUnderstanding() != null ? entry.getUnderstanding() : "N/A"));
                logbookText.append(String.format("  * Acción correctora: \"%s\"\n", entry.getCorrectionAction() != null ? entry.getCorrectionAction() : "N/A"));
                logbookText.append(String.format("  * Acuerdo: \"%s\"\n", entry.getFamilyAgreement() != null ? entry.getFamilyAgreement() : "N/A"));
            }

            // 3. Construir métricas de correlación para informar al LLM
            StringBuilder correlationsText = new StringBuilder();
            for (DimensionCorrelation dc : correlations) {
                correlationsText.append(String.format("- %s: Puntaje Diagnóstico = %.1f%%, Sentimiento Bitácora = %.2f (Delta = %.1f%%, Reajuste Prioridad = %s)\n",
                        dc.getDimensionFriendlyName(), dc.getDiagnosticScore(), dc.getLogbookSentimentScore(), dc.getCorrelationDelta(), dc.isRequiresPriorityShift() ? "SÍ" : "NO"));
            }

            // 4. Prompt clínico estructurado para Claude
            String prompt = String.format(
                "ALINEACIÓN ADAPTATIVA Y ANÁLISIS CUALITATIVO DE BITÁCORAS:\n\n" +
                "Hola Claude. Como Mentor de Integridad Familiar, realiza un análisis clínico cualitativo profundo " +
                "de las vivencias reales registradas por la familia en su bitácora semanal y contrástalas contra su última evaluación formal.\n\n" +
                "=== DATOS GENERALES DE LA FAMILIA ===\n" +
                "Nombre: %s\n" +
                "Hito Actual: %s\n" +
                "Sentimiento General Estimado (VADER): %.2f\n\n" +
                "=== CORRELACIONES DIMENSIONALES (DIAGNÓSTICO VS BITÁCORA) ===\n" +
                "%s\n" +
                "=== REGISTROS RECIENTES DE LA BITÁCORA ===\n" +
                "%s\n" +
                "=== INSTRUCCIÓN ===\n" +
                "Genera un reporte premium estructurado de dos secciones usando markdown:\n\n" +
                "1. **Análisis Cualitativo Clínico 🧠**\n" +
                "   (Analiza el tono del lenguaje, emociones subyacentes, fortalezas o patrones pasivo-agresivos/regresivos que se deduzcan de lo que la familia escribe).\n\n" +
                "2. **Calibración Adaptativa del Plan de 36 Meses 🎯**\n" +
                "   (Ofrece una sugerencia asertiva de qué misiones del hito actual priorizar, reforzar o adaptar basándote en las brechas y discrepancias detectadas).\n\n" +
                "Sé empático, clínicamente asertivo, asombrosamente perspicaz y directo. Estructura el markdown de forma elegante.",
                family.getName(), family.getCurrentMilestone() != null ? family.getCurrentMilestone() : "Inicial",
                globalSentiment, correlationsText.toString(), logbookText.toString()
            );

            log.info("🧠 [SENTIMENT-CLAUDE] Consultando a Claude...");
            return aiProvider.generateResponse(prompt, context);
        } catch (Exception e) {
            log.error("⚠️ [SENTIMENT-CLAUDE] Error al generar recomendación adaptativa con Claude, usando fallback: {}", e.getMessage());
            return generateAdaptationRecommendation(globalSentiment, correlations);
        }
    }

    /**
     * Clasificador Heurístico Multidimensional
     * Mapea semánticamente la bitácora hacia una de las 4 dimensiones psicométricas.
     */
    private String classifyEntryDimension(FamilyLogbookEntry entry) {
        String fullText = String.format("%s %s %s %s",
                entry.getSituation(),
                entry.getDifficultyDetected(),
                entry.getEmotionIdentified(),
                entry.getFamilyAgreement()
        ).toLowerCase();

        // 1. Dimensión Hábitos
        if (fullText.contains("orden") || fullText.contains("limpieza") || fullText.contains("celular") ||
            fullText.contains("pantalla") || fullText.contains("tarea") || fullText.contains("hábito") ||
            fullText.contains("rutina") || fullText.contains("norma") || fullText.contains("regla")) {
            return "habitos";
        }

        // 2. Dimensión Gestión Emocional
        if (fullText.contains("triste") || fullText.contains("rabia") || fullText.contains("miedo") ||
            fullText.contains("enojo") || fullText.contains("llanto") || fullText.contains("frustra") ||
            fullText.contains("afecto") || fullText.contains("abrazo") || fullText.contains("cariño")) {
            return "emociones";
        }

        // 3. Dimensión Tiempos de Calidad
        if (fullText.contains("tiempo") || fullText.contains("salida") || fullText.contains("ausen") ||
            fullText.contains("viaje") || fullText.contains("trabajo") || fullText.contains("compartir") ||
            fullText.contains("juntos") || fullText.contains("comer") || fullText.contains("cenar")) {
            return "tiempos";
        }

        // 4. Default / Comunicación (Dimension base de tensiones)
        return "comunicacion";
    }

    /**
     * Motor de Valencia Léxica para Español Clínico Familiar (VADER Simplificado)
     */
    private double calculateSpanishVaderScore(String... texts) {
        if (texts == null || texts.length == 0) return 0.0;
        
        double score = 0.0;
        int wordCount = 0;

        // Lexicón de valencias del sistema
        Map<String, Double> lexicon = new HashMap<>();
        // Negativos / Crisis
        lexicon.put("crisis", -1.0);
        lexicon.put("pelea", -0.8);
        lexicon.put("grito", -0.7);
        lexicon.put("insulto", -0.9);
        lexicon.put("grosería", -0.8);
        lexicon.put("distancia", -0.4);
        lexicon.put("ausente", -0.5);
        lexicon.put("rabia", -0.6);
        lexicon.put("triste", -0.5);
        lexicon.put("llanto", -0.5);
        lexicon.put("frustración", -0.6);
        lexicon.put("enojado", -0.6);
        lexicon.put("celular", -0.3); // Disrupción por pantallas
        lexicon.put("pantalla", -0.3);
        lexicon.put("desorden", -0.4);
        lexicon.put("mentira", -0.8);

        // Positivos / Reparación
        lexicon.put("amor", 0.8);
        lexicon.put("abrazo", 0.7);
        lexicon.put("perdon", 0.9);
        lexicon.put("perdón", 0.9);
        lexicon.put("acuerdo", 0.6);
        lexicon.put("dialogo", 0.6);
        lexicon.put("diálogo", 0.6);
        lexicon.put("hablar", 0.4);
        lexicon.put("juntos", 0.5);
        lexicon.put("unidos", 0.6);
        lexicon.put("paz", 0.8);
        lexicon.put("feliz", 0.7);
        lexicon.put("felicidad", 0.7);
        lexicon.put("escuchar", 0.5);
        lexicon.put("entender", 0.5);
        lexicon.put("reparar", 0.7);

        for (String text : texts) {
            if (text == null || text.trim().isEmpty()) continue;
            String[] words = text.toLowerCase().split("\\s+");
            for (String word : words) {
                wordCount++;
                // Limpiar caracteres de puntuación
                String cleanWord = word.replaceAll("[^a-zA-ZáéíóúñÁÉÍÓÚ]", "");
                if (lexicon.containsKey(cleanWord)) {
                    score += lexicon.get(cleanWord);
                }
            }
        }

        if (wordCount == 0) return 0.0;
        
        // Normalización matemática acotada entre -1.0 y 1.0
        return Math.max(-1.0, Math.min(1.0, score));
    }

    private String determineLabel(double score) {
        if (score < -0.4) return "CRISIS";
        if (score < 0) return "NEGATIVO";
        if (score < 0.4) return "CONSCIENTE";
        return "POSITIVO";
    }

    private String generateAdaptationRecommendation(double globalSentiment, List<DimensionCorrelation> correlations) {
        if (globalSentiment < -0.4) {
            return "🚨 PROTOCOLO SENTINEL RECOMENDADO: La bitácora familiar registra una tensión emocional severa y sostenida en el hogar. " +
                    "Se recomienda pausar las misiones avanzadas de la hoja de ruta y enfocar el plan de 36 meses exclusivamente en 'Protocolos de Contención Emocional' " +
                    "y mediación guiada por el mentor.";
        }

        List<String> prioritizedDims = correlations.stream()
                .filter(DimensionCorrelation::isRequiresPriorityShift)
                .map(DimensionCorrelation::getDimensionFriendlyName)
                .collect(Collectors.toList());

        if (!prioritizedDims.isEmpty()) {
            return "⚡ CALIBRACIÓN ADAPTATIVA MILIMÉTRICA: Se ha detectado una discrepancia crítica entre el diagnóstico formal de la familia y las vivencias registradas en la bitácora semanal en la(s) dimensión(es): " +
                    String.join(", ", prioritizedDims) + ". El motor de selección adaptativo ha modificado automáticamente las prioridades, " +
                    "inyectando misiones de reestructuración preventiva de hábitos y forzando reactivos adaptativos enfocados en mitigar regresiones conductuales.";
        }

        if (globalSentiment > 0.4) {
            return "🌟 PROGRESIÓN DE EXCELENCIA: Consonancia asertiva plena. El registro cualitativo de la bitácora ratifica la maduración estructural " +
                    "de la familia. El plan de misiones continuará su progresión lineal estándar, habilitando actividades avanzadas de autonomía y liderazgo familiar.";
        }

        return "✓ EQUILIBRIO CONDUCTUAL: Coincidencia asertiva entre el diagnóstico base y la bitácora. Mantener las misiones asignadas del hito actual y continuar registrando acuerdos en el checklist de hábitos semanales.";
    }

    private LogbookCorrelationResult buildEmptyCorrelationResult(Family family, Optional<Evaluation> lastEvalOpt) {
        List<DimensionCorrelation> emptyCorrelations = new ArrayList<>();
        String[] coreDimensions = {"comunicacion", "emociones", "habitos", "tiempos"};
        
        Map<String, Double> diagnosticScores = new HashMap<>();
        if (lastEvalOpt.isPresent()) {
            Evaluation eval = lastEvalOpt.get();
            for (EvaluationDimensionScore ds : eval.getDimensionScores()) {
                diagnosticScores.put(ds.getDimensionName().toLowerCase(), ds.getScore());
            }
        }

        for (String dim : coreDimensions) {
            emptyCorrelations.add(DimensionCorrelation.builder()
                    .dimensionName(dim)
                    .dimensionFriendlyName(getDimensionFriendlyName(dim))
                    .diagnosticScore(diagnosticScores.getOrDefault(dim, 50.0))
                    .logbookSentimentScore(0.0)
                    .correlationDelta(0.0)
                    .requiresPriorityShift(false)
                    .build());
        }

        return LogbookCorrelationResult.builder()
                .familyId(family.getId())
                .familyName(family.getName())
                .totalEntriesAnalyzed(0)
                .averageEmotionalScore(0.0)
                .generalLabel("CONSCIENTE")
                .dimensionCorrelations(emptyCorrelations)
                .adaptationRecommendation("✓ SIN DATOS DE BITÁCORA: Aún no se registran entradas semanales en la bitácora para evaluar. " +
                        "Se recomienda registrar su primera vivencia de convivencia semanal para calibrar el algoritmo adaptativo del plan.")
                .calculatedAt(LocalDateTime.now())
                .build();
    }

    private String getDimensionFriendlyName(String dimension) {
        switch (dimension.toLowerCase()) {
            case "comunicacion": return "Comunicación Asertiva";
            case "emociones": return "Regulación & Clima Emocional";
            case "habitos": return "Hábitos & Convivencia Colectiva";
            case "tiempos": return "Tiempos de Conexión Activa";
            default: return dimension.toUpperCase();
        }
    }
}
