package com.integrityfamily.chat.controller;

import com.integrityfamily.domain.ChatMessage;
import com.integrityfamily.domain.ConversationSession;
import com.integrityfamily.domain.repository.ChatMessageRepository;
import com.integrityfamily.ai.service.AiService;
import com.integrityfamily.ai.service.ConversationSessionService;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.common.exception.NotFoundException;
import com.integrityfamily.common.security.SecurityValidator;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SDD: ChatController (Protocolo Sentinel)
 * Punto de entrada sincronizado para mensajerÃƒÂ­a y diagnÃƒÂ³sticos rÃƒÂ¡pidos.
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatMessageRepository chatMessageRepository;
    private final FamilyRepository familyRepository;
    private final EvaluationRepository evaluationRepository;
    private final AiService aiService;
    private final SecurityValidator securityValidator;
    private final ConversationSessionService conversationSessionService;

    @GetMapping("/family/{familyId}")
    public ApiResponse<List<com.integrityfamily.domain.repository.ChatMessageSummary>> getHistory(@PathVariable Long familyId, Principal principal) {
        securityValidator.validateFamilyOwnership(familyId, principal);
        // FIX: Uso de proyecciones para evitar serialización circular
        return ApiResponse.ok(chatMessageRepository.findProjectedByFamilyIdOrderByCreatedAtAsc(familyId));
    }

    @PostMapping("/send")
    public ApiResponse<ChatMessage> sendMessage(@RequestBody ChatRequest request, Principal principal) {
        securityValidator.validateFamilyOwnership(request.getFamilyId(), principal);

        Family family = familyRepository.findById(request.getFamilyId())
                .orElseThrow(() -> new NotFoundException("Familia no encontrada"));

        return ApiResponse.ok(aiService.chat(request.getMessage(), family, request.getMemberId()));
    }

    /**
     * Genera reportes de bienestar basados en una evaluaciÃƒÂ³n especÃƒÂ­fica.
     * ResoluciÃƒÂ³n del error de sÃƒÂ­mbolo para evaluationRepository.
     */
    @PostMapping("/report/{evaluationId}")
    public ApiResponse<String> generateAutoReport(@PathVariable Long evaluationId) {
        log.info("Ã°Å¸â€œÅ  [CHAT-CONTROLLER] Generating auto-report for evaluation: {}", evaluationId);

        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new NotFoundException("Evaluación no encontrada"));

        Map<String, Double> dimensions = evaluation.getDimensionScores().stream()
                .collect(Collectors.toMap(
                    ds -> ds.getDimensionName(),
                    ds -> ds.getScore(),
                    (v1, v2) -> v1));

        String riskLevel = evaluation.getRiskLevel() != null ? evaluation.getRiskLevel() : "MEDIUM";

        String advice = aiService.generateDashboardInsight(evaluation.getFamily(), dimensions, riskLevel);

        return ApiResponse.ok(advice);
    }

    @GetMapping("/session/active")
    public ApiResponse<SessionContextResponse> getActiveSession(
            @RequestParam Long familyId,
            @RequestParam(required = false) Long memberId,
            Principal principal) {
        securityValidator.validateFamilyOwnership(familyId, principal);
        ConversationSession session = conversationSessionService.getActiveSession(familyId, memberId);
        if (session == null) return ApiResponse.ok(null);
        return ApiResponse.ok(SessionContextResponse.builder()
                .sessionId(session.getId())
                .goal(session.getGoal())
                .emotionalArc(session.getEmotionalState())
                .turnCount(session.getTurnCount())
                .startedAt(session.getStartedAt() != null ? session.getStartedAt().toString() : null)
                .build());
    }

    @Data
    @Builder
    public static class SessionContextResponse {
        private Long sessionId;
        private String goal;
        private String emotionalArc;
        private int turnCount;
        private String startedAt;
    }

    @Data
    public static class ChatRequest {
        private Long familyId;
        private String message;
        private Long memberId;
    }
}


