package com.integrityfamily.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.ai.dto.AiContext;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.cognitive.service.FamilyMemoryService;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMemory;
import com.integrityfamily.domain.repository.ChatMessageRepository;
import com.integrityfamily.domain.repository.ConversationSessionRepository;
import com.integrityfamily.domain.repository.FamilyMemoryRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fase E: Cierra el bucle cognitivo persistiendo cada sesión conversacional
 * como una memoria episódica en FamilyMemory (semanticKey = "conversation-session").
 *
 * Flujo: sesión → síntesis Claude → FamilyMemory.EPISODIC → buildCognitiveContext()
 * Las conversaciones ya forman parte del contexto de los próximos chats.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SessionMemoryConsolidator {

    private final ConversationSessionRepository sessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final FamilyRepository familyRepository;
    private final FamilyMemoryRepository memoryRepository;
    private final FamilyMemoryService familyMemoryService;
    private final AiProvider aiProvider;
    private final PromptGenerator promptGenerator;
    private final ObjectMapper objectMapper;

    static final String SEMANTIC_KEY = "conversation-session";

    /**
     * Sintetiza la sesión y crea/actualiza una FamilyMemory episódica.
     * Falla silenciosamente para no bloquear el flujo del chat.
     */
    @Transactional
    public void consolidate(Long sessionId, Long familyId, AiContext context) {
        if (sessionId == null || familyId == null) return;
        try {
            var session = sessionRepository.findById(sessionId).orElse(null);
            if (session == null) return;

            List<String> userMessages = chatMessageRepository.findUserMessageContentsBySessionId(sessionId);
            if (userMessages.isEmpty()) return;

            String memberName = context.activeMember() != null ? context.activeMember().fullName() : "el miembro";
            String memberRole = context.activeMember() != null ? context.activeMember().role() : "FAMILIA";

            String prompt = promptGenerator.buildSessionSynthesisPrompt(
                    userMessages, memberRole, memberName,
                    session.getGoal(), session.getEmotionalState());

            String rawResponse = aiProvider.generateRawResponse(prompt);
            upsertSessionMemory(sessionId, familyId, session.getGoal(),
                    session.getEmotionalState(), session.getTurnCount(),
                    session.getMemberId(), memberRole, memberName, rawResponse);

            // Trigger consolidación semántica si hay suficiente historial
            familyMemoryService.consolidateSemanticPattern(familyId, SEMANTIC_KEY);

            log.info("[SESSION_MEMORY] Episodio conversacional consolidado — sesión={} familia={}", sessionId, familyId);
        } catch (Exception e) {
            log.warn("[SESSION_MEMORY] Error consolidando sesión {}: {}", sessionId, e.getMessage());
        }
    }

    private void upsertSessionMemory(
            Long sessionId, Long familyId,
            String goal, String emotionalArc, int turnCount,
            Long memberId, String memberRole, String memberName,
            String rawResponse) throws Exception {

        String synthesisJson = extractJson(rawResponse);
        JsonNode synthesis = objectMapper.readTree(synthesisJson);

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("sessionId", sessionId);
        if (memberId != null) content.put("memberId", memberId);
        content.put("memberRole", memberRole);
        content.put("memberName", memberName);
        content.put("goal", goal);
        content.put("turnCount", turnCount);
        content.put("emotionalArc", emotionalArc);

        addIfPresent(content, synthesis, "themes");
        addTextIfPresent(content, synthesis, "emotionalSummary");
        addTextIfPresent(content, synthesis, "memberState");
        addIfPresent(content, synthesis, "progressSignals");
        if (synthesis.has("recommendedFollowUp") && !synthesis.get("recommendedFollowUp").isNull()) {
            content.put("recommendedFollowUp", synthesis.get("recommendedFollowUp").asText());
        }

        double importance = synthesis.has("importanceScore")
                ? Math.min(1.0, Math.max(0.1, synthesis.get("importanceScore").asDouble(0.5)))
                : 0.5;

        String contentJson = objectMapper.writeValueAsString(content);

        // Upsert: actualiza la memoria de la sesión si ya existe, crea si no
        List<FamilyMemory> existing = memoryRepository
                .findByFamilyIdAndSemanticKeyOrderByCreatedAtDesc(familyId, SEMANTIC_KEY);

        FamilyMemory existingForSession = existing.stream()
                .filter(m -> sessionId.equals(m.getSourceId()))
                .findFirst()
                .orElse(null);

        if (existingForSession != null) {
            existingForSession.setContent(contentJson);
            existingForSession.setImportanceScore(importance);
            existingForSession.setUpdatedAt(LocalDateTime.now());
            memoryRepository.save(existingForSession);
        } else {
            Family family = familyRepository.getReferenceById(familyId);
            memoryRepository.save(FamilyMemory.builder()
                    .family(family)
                    .memoryType(FamilyMemory.MemoryType.EPISODIC)
                    .semanticKey(SEMANTIC_KEY)
                    .content(contentJson)
                    .importanceScore(importance)
                    .sourceType("AI_INFERENCE")
                    .sourceId(sessionId)
                    .build());
        }
    }

    @SuppressWarnings("unchecked")
    private void addIfPresent(Map<String, Object> content, JsonNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            try {
                content.put(field, objectMapper.convertValue(node.get(field), List.class));
            } catch (Exception ignored) {
                content.put(field, node.get(field).asText());
            }
        }
    }

    private void addTextIfPresent(Map<String, Object> content, JsonNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            content.put(field, node.get(field).asText());
        }
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
}
