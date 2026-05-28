package com.integrityfamily.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.ai.dto.AiContext;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.cognitive.service.MemberIdentityProfileService;
import com.integrityfamily.domain.repository.ChatMessageRepository;
import com.integrityfamily.domain.repository.ConversationSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Fase D: Analiza patrones conversacionales al alcanzar umbrales de turnos
 * y actualiza el MemberIdentityProfile con los estilos detectados.
 *
 * Se dispara cada ANALYSIS_INTERVAL turnos para amortizar el coste de inferencia.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PostSessionAnalyzer {

    private final ChatMessageRepository chatMessageRepository;
    private final ConversationSessionRepository sessionRepository;
    private final MemberIdentityProfileService identityProfileService;
    private final AiProvider aiProvider;
    private final PromptGenerator promptGenerator;
    private final ObjectMapper objectMapper;

    private static final int ANALYSIS_INTERVAL   = 5;
    private static final int MIN_MESSAGES_TO_ANALYZE = 3;

    /**
     * Verifica si el turno actual alcanza el umbral de análisis y, si es así,
     * llama a Claude para detectar patrones conversacionales y actualiza el perfil.
     *
     * @param sessionId  ID de la sesión activa
     * @param memberId   ID del miembro
     * @param turnCount  Número de turno recién completado
     * @param context    AiContext del turno actual (para obtener nombre/rol sin re-query)
     */
    @Transactional
    public void analyzeIfThresholdReached(Long sessionId, Long memberId, int turnCount, AiContext context) {
        if (sessionId == null || memberId == null) return;
        if (turnCount < MIN_MESSAGES_TO_ANALYZE) return;
        if (turnCount % ANALYSIS_INTERVAL != 0) return;

        try {
            List<String> userMessages = chatMessageRepository.findUserMessageContentsBySessionId(sessionId);
            if (userMessages.size() < MIN_MESSAGES_TO_ANALYZE) return;

            String memberName = context.activeMember() != null ? context.activeMember().fullName() : "el miembro";
            String memberRole = context.activeMember() != null ? context.activeMember().role() : "FAMILIA";

            String prompt = promptGenerator.buildIdentityAnalysisPrompt(userMessages, memberRole, memberName);
            String rawResponse = aiProvider.generateRawResponse(prompt);

            parseAndUpdateProfile(memberId, rawResponse);
            log.info("[IDENTITY_EVOLUTION] Perfil actualizado — miembro={} sesión={} turno={}", memberId, sessionId, turnCount);
        } catch (Exception e) {
            log.warn("[IDENTITY_EVOLUTION] Error en análisis — sesión={}: {}", sessionId, e.getMessage());
        }
    }

    private void parseAndUpdateProfile(Long memberId, String rawResponse) throws Exception {
        String json = extractJson(rawResponse);
        JsonNode node = objectMapper.readTree(json);

        String communicationStyle  = textOrNull(node, "communicationStyle");
        Integer reflexivityLevel   = intOrNull(node, "reflexivityLevel");
        Integer emotionalSensitivity = intOrNull(node, "emotionalSensitivity");
        String changeResistance    = textOrNull(node, "changeResistance");

        String evasionPatterns = null;
        if (node.has("evasionPatterns") && !node.get("evasionPatterns").isNull() && node.get("evasionPatterns").isArray()) {
            evasionPatterns = objectMapper.writeValueAsString(node.get("evasionPatterns"));
        }

        String motivators = null;
        if (node.has("motivators") && !node.get("motivators").isNull() && node.get("motivators").isArray()) {
            motivators = objectMapper.writeValueAsString(node.get("motivators"));
        }

        identityProfileService.update(memberId, communicationStyle, reflexivityLevel,
                emotionalSensitivity, changeResistance, evasionPatterns, motivators);
    }

    private String extractJson(String text) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Respuesta vacía del modelo");
        int jsonStart = text.indexOf("```json");
        if (jsonStart >= 0) {
            int jsonEnd = text.indexOf("```", jsonStart + 7);
            if (jsonEnd > jsonStart) return text.substring(jsonStart + 7, jsonEnd).trim();
        }
        int curly = text.indexOf('{');
        int lastCurly = text.lastIndexOf('}');
        if (curly >= 0 && lastCurly > curly) return text.substring(curly, lastCurly + 1);
        throw new IllegalArgumentException("No se encontró JSON válido en la respuesta");
    }

    private String textOrNull(JsonNode node, String field) {
        return (node.has(field) && !node.get(field).isNull()) ? node.get(field).asText() : null;
    }

    private Integer intOrNull(JsonNode node, String field) {
        return (node.has(field) && !node.get(field).isNull()) ? node.get(field).asInt() : null;
    }
}
