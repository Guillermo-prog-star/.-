package com.integrityfamily.ai.service;

import com.integrityfamily.ai.dto.AiContext;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.domain.ChatMessage;
import com.integrityfamily.domain.repository.ChatMessageRepository;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.EvaluationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final AiProvider aiProvider;
    private final FamilyRepository familyRepository;
    private final EvaluationRepository evaluationRepository;
    private final ContextSynthesizer contextSynthesizer;
    private final ChatMessageRepository chatMessageRepository;
    private final PromptGenerator promptGenerator;
    private final SentimentAnalysisService sentimentAnalysisService;

    @Override
    @Transactional
    public ChatMessage processInteractiveChat(String message, Family family) {
        // 1. Guardar mensaje del usuario
        chatMessageRepository.save(ChatMessage.builder()
                .content(message)
                .family(family)
                .ai(false)
                .build());

        // 2. Análisis de Sentimiento para adaptar la respuesta
        var sentiment = sentimentAnalysisService.analyze(message);
        log.info("[AI_SENTIMENT] Detectado: {} (score: {})", sentiment.getLabel(), sentiment.getScore());

        // 3. Sintetizar contexto
        AiContext context = contextSynthesizer.synthesize(family, sentiment.getLabel());
        
        // 4. Generar respuesta (Aquí podríamos pasar el sentimiento al promptGenerator en el futuro)
        String response = aiProvider.generateResponse(message, context);

        // 5. Guardar respuesta de la IA
        return chatMessageRepository.save(ChatMessage.builder()
                .content(response)
                .family(family)
                .ai(true)
                .build());
    }

    @Override
    public String processAnalyticInference(String prompt, Long familyId) {
        AiContext context = (familyId != null)
                ? familyRepository.findById(familyId).map(f -> contextSynthesizer.synthesize(f, "NEUTRAL")).orElse(null)
                : null;
        return aiProvider.generateResponse(prompt, context);
    }

    @Override
    public String generateDashboardInsight(Family family, Map<String, Double> dimensions, String riskLevel) {
        log.info("[AI_ANALYTICS] Generando insight de dashboard para familia: {}", family.getName());
        String prompt = promptGenerator.buildDashboardInsightPrompt(family, dimensions, riskLevel);
        return aiProvider.generateRawResponse(prompt);
    }

    @Override
    public String generateExecutiveSynthesis(Long familyId) {
        log.info("[AI_ANALYTICS] Generando síntesis ejecutiva por ID para ID: {}", familyId);
        Evaluation lastEval = evaluationRepository.findTopByFamilyIdAndStatusOrderByFinalizedAtDesc(familyId, EvaluationStatus.FINALIZED)
                .orElse(null);
        if (lastEval == null) return "Sin evaluaciones finalizadas.";
        return generateExecutiveSynthesis(lastEval);
    }

    @Override
    public String generateExecutiveSynthesis(Evaluation evaluation) {
        log.info("[AI_ANALYTICS] Generando síntesis ejecutiva (UIE) enriquecida para Evaluación ID: {}", evaluation.getId());
        
        Map<String, Double> dimensions = evaluation.getDimensionScores().stream()
                .collect(Collectors.toMap(ds -> ds.getDimensionName(), ds -> ds.getScore()));

        String answersJson = evaluation.getAnswers().stream()
                .map(a -> String.format("{\"questionKey\":\"%s\", \"dimension\":\"%s\", \"score\":%d, \"consciousnessLevel\":\"%s\"}", 
                        a.getQuestionKey(), 
                        a.getDiagnosticDimension() != null ? a.getDiagnosticDimension() : "comunicacion", 
                        a.getScore(), 
                        a.getConsciousnessLevel() != null ? a.getConsciousnessLevel() : "Consciente"))
                .collect(Collectors.joining(",\n  ", "[\n  ", "\n]"));

        String prompt = promptGenerator.buildSpiritualSynthesisPrompt(evaluation.getFamily(), dimensions, answersJson);
        return aiProvider.generateRawResponse(prompt);
    }

    @Override
    public String generateDiagnosticMissions(Evaluation evaluation) {
        log.info("[AI_MISSIONS] Generando misiones diagnósticas adaptativas para Evaluación ID: {}", evaluation.getId());
        
        String answersJson = evaluation.getAnswers().stream()
                .map(a -> String.format("{\"questionKey\":\"%s\", \"dimension\":\"%s\", \"score\":%d, \"consciousnessLevel\":\"%s\"}", 
                        a.getQuestionKey(), 
                        a.getDiagnosticDimension() != null ? a.getDiagnosticDimension() : "comunicacion", 
                        a.getScore(), 
                        a.getConsciousnessLevel() != null ? a.getConsciousnessLevel() : "Consciente"))
                .collect(Collectors.joining(",\n  ", "[\n  ", "\n]"));

        String prompt = promptGenerator.buildDiagnosticMissionsPrompt(
                evaluation.getFamily(), 
                evaluation.getMember(), 
                answersJson, 
                evaluation.getIcf(), 
                evaluation.getRiskLevel()
        );
        return aiProvider.generateRawResponse(prompt);
    }

    @Override
    public String generateSynthesis(Map<String, Object> context) {
        log.info("[AI_ANALYTICS] Generando síntesis técnica de evaluación.");
        // SDD: Mantenemos el legacy por compatibilidad de firma, pero delegamos al motor unificado si es posible
        return "SÍNTESIS_TÉCNICA_DEPRECATED: Use generateExecutiveSynthesis.";
    }

    @Override
    public String generateMissions(Family family) {
        log.info("[AI_MISSIONS] Generando misiones pedagógicas básicas para familia: {}", family.getName());
        AiContext aiContext = contextSynthesizer.synthesize(family, "NEUTRAL");
        String prompt = "Como Mentor de Integridad, genera una lista de 3 misiones pedagógicas inmediatas para esta familia. " +
                "IMPORTANTE: No hables como un terapeuta ni uses lenguaje corporativo o clínico. Usa un tono sumamente cálido, sencillo, humano y directo, como un consejo de un amigo sabio. " +
                "Las misiones deben ser microacciones cotidianas de fricción casi nula, fáciles de cumplir y recordar. " +
                "Responde ÚNICAMENTE con un arreglo JSON válido siguiendo estrictamente este esquema:\n" +
                "[\n" +
                "  {\n" +
                "    \"dimension\": \"EMOCIONES | COMUNICACION | HABITOS | TIEMPOS\",\n" +
                "    \"riskLevel\": \"LOW | MEDIUM | HIGH | CRISIS\",\n" +
                "    \"problemDetected\": \"Descripción sencilla del problema detectado (ej: Comparten poco tiempo)\",\n" +
                "    \"objective\": \"Objetivo real (ej: Mejorar sintonía)\",\n" +
                "    \"missionType\": \"Tipo de misión (ej: Cena, Paseo)\",\n" +
                "    \"targetMembers\": [\"Todos\" o nombres específicos],\n" +
                "    \"frequency\": \"Frecuencia (ej: Hoy, 1 vez)\",\n" +
                "    \"estimatedDuration\": 15,\n" +
                "    \"successMetric\": \"Evidencia simple (ej: Una foto o frase)\",\n" +
                "    \"adaptiveReason\": \"Razón de la adaptación\",\n" +
                "    \"title\": \"Título corto y cálido (ej: 🍽 Cena sin celulares)\",\n" +
                "    \"description\": \"Instrucción muy corta, humana y motivadora (ej: Intenten comer juntos hoy sin pantallas. Lo importante es compartir el momento.)\"\n" +
                "  }\n" +
                "]";
        return aiProvider.generateResponse(prompt, aiContext);
    }

    @Override
    public String generateEvolutionaryMissions(Family family, Map<String, Double> dimensions, String riskLevel) {
        log.info("[AI_MISSIONS] Generando misiones evolutivas (1m-2y) para familia: {}", family.getName());
        String prompt = promptGenerator.buildMissionGenerationPrompt(family, dimensions, riskLevel);
        return aiProvider.generateRawResponse(prompt);
    }

    @Override
    public String generateHybridPlan(Family family, Map<String, Double> dimensions, String riskLevel) {
        return generateHybridPlan(family, dimensions, riskLevel, null);
    }

    @Override
    public String generateHybridPlan(Family family, Map<String, Double> dimensions, String riskLevel, com.integrityfamily.plan.service.ContinuityEngine.ContinuityAnalysis continuityAnalysis) {
        log.info("[AI_PLAN] Generando Plan Híbrido Longitudinal Estructurado (SDD v5.0) para familia: {}", family.getName());
        
        com.integrityfamily.ai.dto.LogbookCorrelationResult correlation = null;
        try {
            correlation = sentimentAnalysisService.correlateFamilySentiment(family.getId());
            log.info("[AI_PLAN] Correlación de sentimiento en vivo consultada con éxito. Etiqueta: {}, Promedio: {}", 
                     correlation.getGeneralLabel(), correlation.getAverageEmotionalScore());
        } catch (Exception e) {
            log.warn("⚠️ [AI_PLAN] No se pudo obtener la correlación de sentimiento de bitácora para la familia ID {}: {}", 
                     family.getId(), e.getMessage());
        }

        String prompt = promptGenerator.buildHybridPlanPrompt(family, dimensions, riskLevel, correlation, continuityAnalysis);
        return aiProvider.generateRawResponse(prompt);
    }
}
