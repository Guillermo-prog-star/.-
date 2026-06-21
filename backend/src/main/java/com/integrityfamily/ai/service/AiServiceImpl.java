package com.integrityfamily.ai.service;

import com.integrityfamily.ai.dto.AiContext;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.ai.provider.TaskType;
import com.integrityfamily.domain.ChatMessage;
import com.integrityfamily.domain.repository.ChatMessageRepository;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.EvaluationStatus;
import com.integrityfamily.domain.ParticipationEventType;
import com.integrityfamily.participation.service.ParticipationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.integrityfamily.chat.controller.ChatController;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final AiProviderSelector aiProviderSelector;
    private final FamilyRepository familyRepository;
    private final EvaluationRepository evaluationRepository;
    private final ContextSynthesizer contextSynthesizer;
    private final ChatMessageRepository chatMessageRepository;
    private final PromptGenerator promptGenerator;
    private final SentimentAnalysisService sentimentAnalysisService;
    private final ParticipationService participationService;
    private final ConversationSessionService conversationSessionService;
    private final ConversationGoalManager conversationGoalManager;
    private final EmotionalStateTracker emotionalStateTracker;
    private final PostSessionAnalyzer postSessionAnalyzer;

    /**
     * Chat enriquecido con contexto de transformación del frontend.
     * Construye un bloque adicional en el prompt con el pilar, mes, sprint y misión activa,
     * para que el Mentor IA responda de forma contextualizada al momento del viaje familiar.
     */
    @Override
    @Transactional
    public ChatMessage chatWithTransformation(String message, Family family, Long memberId,
                                              Object rawContext) {
        // Si hay contexto de transformación, prefijarlo al mensaje antes de procesarlo
        if (rawContext instanceof ChatController.ChatRequestV2.TransformationContextDto tc) {
            String enrichedMessage = buildTransformationPrefix(tc) + message;
            return processInteractiveChat(enrichedMessage, family, memberId);
        }
        return processInteractiveChat(message, family, memberId);
    }

    private String buildTransformationPrefix(ChatController.ChatRequestV2.TransformationContextDto tc) {
        if (tc == null) return "";
        return String.format("""
            [CONTEXTO_TRANSFORMACIÓN: pilar=%s, mes=%s, hito=%s, fase=%s, sprint=%s, misión_activa=%s, progreso=%s%%]
            """,
            tc.getCurrentPillar() != null ? tc.getCurrentPillar() : "reconocimiento",
            tc.getCurrentMonth()  != null ? tc.getCurrentMonth()  : 1,
            tc.getMilestoneLabel() != null ? tc.getMilestoneLabel() : "M1",
            tc.getCurrentPhase()  != null ? tc.getCurrentPhase()  : "Estabilización",
            tc.getSprintNumber()  != null ? tc.getSprintNumber()  : 1,
            tc.getActiveMissionId() != null ? tc.getActiveMissionId() : "ninguna",
            tc.getProgressPercent() != null ? tc.getProgressPercent() : 0
        );
    }

    @Override
    @Transactional
    public ChatMessage processInteractiveChat(String message, Family family, Long memberId) {
        // 1. Análisis de sentimiento (antes de guardar para enriquecer el mensaje)
        var sentiment = sentimentAnalysisService.analyze(message);
        log.info("[AI_SENTIMENT] Detectado: {} (score: {})", sentiment.getLabel(), sentiment.getScore());

        // 2. Inferir objetivo y obtener/crear sesión conversacional
        final String inferredGoal = inferGoalSilent(family.getId(), memberId);
        final Long sessionId = findOrCreateSessionSilent(family.getId(), memberId, inferredGoal);

        // 3. Guardar mensaje del usuario con sesión y snapshot emocional
        String emotionalSnapshot = toEmotionalSnapshot(sentiment.getLabel());
        chatMessageRepository.save(ChatMessage.builder()
                .content(message)
                .family(family)
                .ai(false)
                .memberId(memberId)
                .sessionId(sessionId)
                .emotionalSnapshot(emotionalSnapshot)
                .build());

        // 4. Registrar evento de participación
        participationService.record(family.getId(), memberId, ParticipationEventType.CHAT_MESSAGE);

        // 5. Actualizar arco emocional de la sesión (incluye el mensaje recién guardado)
        emotionalStateTracker.computeAndUpdateArc(sessionId);

        // 6. Sintetizar contexto relacional unificado (lee arco + goal desde la sesión)
        AiContext context = contextSynthesizer.synthesize(family, memberId, sessionId, sentiment.getLabel());

        // 7. Routing explícito al prompt diferenciado por rol
        String fullPrompt;
        if (context.activeMember() != null && context.activeMember().isGuardian()) {
            log.info("[AI_CHAT] Modo GUARDIAN — familia {} / miembro {}", family.getId(), memberId);
            fullPrompt = promptGenerator.buildGuardianMentorPrompt(message, context);
        } else if (context.activeMember() != null) {
            log.info("[AI_CHAT] Modo MEMBER ({}) — familia {} / miembro {}",
                    context.activeMember().role(), family.getId(), memberId);
            fullPrompt = promptGenerator.buildMemberMentorPrompt(message, context);
        } else {
            log.info("[AI_CHAT] Modo FAMILY — familia {}", family.getId());
            fullPrompt = promptGenerator.buildFamilyMentorPrompt(message, context);
        }

        // 8. Chat conversacional → STANDARD (si el proveedor falla, no persistir error en BD)
        String response;
        try {
            response = aiProviderSelector.selectProvider(TaskType.STANDARD).generateWithFullPrompt(fullPrompt);
        } catch (Exception e) {
            log.error("[AI_CHAT] Proveedor IA no disponible para familia {}: {}", family.getId(), e.getMessage());
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                "El servicio de IA no está disponible en este momento. Intenta en unos minutos.");
        }

        // 9. Guardar respuesta de la IA vinculada a la misma sesión
        ChatMessage aiMessage = chatMessageRepository.save(ChatMessage.builder()
                .content(response)
                .family(family)
                .ai(true)
                .sessionId(sessionId)
                .build());

        // 10. Actualizar métricas de sesión y analizar identidad si corresponde
        if (sessionId != null) {
            try {
                conversationSessionService.updateEmotionalState(sessionId, emotionalSnapshot);
                int turnCount = conversationSessionService.incrementTurnCount(sessionId);
                postSessionAnalyzer.analyzeIfThresholdReached(sessionId, family.getId(), memberId, turnCount, context);
            } catch (Exception e) {
                log.warn("[AI_CHAT] No se pudo actualizar sesión {}: {}", sessionId, e.getMessage());
            }
        }

        return aiMessage;
    }

    private Long findOrCreateSessionSilent(Long familyId, Long memberId, String goal) {
        if (memberId == null) return null;
        try {
            return conversationSessionService.findOrCreateSession(familyId, memberId, goal).getId();
        } catch (Exception e) {
            log.warn("[AI_CHAT] No se pudo obtener sesión conversacional: {}", e.getMessage());
            return null;
        }
    }

    private String inferGoalSilent(Long familyId, Long memberId) {
        if (memberId == null) return "GENERAL";
        try {
            return conversationGoalManager.inferGoal(familyId, memberId);
        } catch (Exception e) {
            log.warn("[AI_CHAT] No se pudo inferir objetivo conversacional: {}", e.getMessage());
            return "GENERAL";
        }
    }

    private String toEmotionalSnapshot(String sentimentLabel) {
        if (sentimentLabel == null) return "CALM";
        return switch (sentimentLabel) {
            case "CRISIS"   -> "ANXIOUS";
            case "NEGATIVE" -> "FRUSTRATED";
            case "POSITIVE" -> "ENGAGED";
            default         -> "CALM";
        };
    }

    @Override
    public String processAnalyticInference(String prompt, Long familyId) {
        AiContext context = (familyId != null)
                ? familyRepository.findById(familyId).map(f -> contextSynthesizer.synthesize(f, "NEUTRAL")).orElse(null)
                : null;
        return aiProviderSelector.selectProvider(TaskType.STANDARD).generateResponse(prompt, context);
    }

    @Override
    public String generateDashboardInsight(Family family, Map<String, Double> dimensions, String riskLevel) {
        log.info("[AI_ANALYTICS] Generando insight de dashboard para familia: {}", family.getName());
        String prompt = promptGenerator.buildDashboardInsightPrompt(family, dimensions, riskLevel);
        return aiProviderSelector.selectProvider(TaskType.STANDARD).generateRawResponse(prompt);
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
        return aiProviderSelector.selectProvider(TaskType.HIGH_CAPACITY).generateRawResponse(prompt);
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
        return aiProviderSelector.selectProvider(TaskType.HIGH_CAPACITY).generateRawResponse(prompt);
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
        return aiProviderSelector.selectProvider(TaskType.HIGH_CAPACITY).generateResponse(prompt, aiContext);
    }

    @Override
    public String generateEvolutionaryMissions(Family family, Map<String, Double> dimensions, String riskLevel) {
        log.info("[AI_MISSIONS] Generando misiones evolutivas (1m-2y) para familia: {}", family.getName());
        String prompt = promptGenerator.buildMissionGenerationPrompt(family, dimensions, riskLevel);
        return aiProviderSelector.selectProvider(TaskType.HIGH_CAPACITY).generateRawResponse(prompt);
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
        return aiProviderSelector.selectProvider(TaskType.HIGH_CAPACITY).generateRawResponse(prompt);
    }
}
