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
 * Punto de entrada sincronizado para mensajería y diagnósticos rápidos.
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
        // FIX: Uso de proyecciones para evitar serializaci?n circular
        return ApiResponse.ok(chatMessageRepository.findProjectedByFamilyIdOrderByCreatedAtAsc(familyId));
    }

    /** @deprecated Usar /send (V2 con transformationContext). Mantenido por compatibilidad. */
    @PostMapping("/send/legacy")
    public ApiResponse<ChatMessage> sendMessageLegacy(@RequestBody ChatRequest request, Principal principal) {
        securityValidator.validateFamilyOwnership(request.getFamilyId(), principal);
        Family family = familyRepository.findById(request.getFamilyId())
                .orElseThrow(() -> new NotFoundException("Familia no encontrada"));
        return ApiResponse.ok(aiService.chat(request.getMessage(), family, request.getMemberId()));
    }

    /**
     * Genera reportes de bienestar basados en una evaluación específica.
     * Resolución del error de símbolo para evaluationRepository.
     */
    @PostMapping("/report/{evaluationId}")
    public ApiResponse<String> generateAutoReport(@PathVariable Long evaluationId) {
        log.info("📊 [CHAT-CONTROLLER] Generating auto-report for evaluation: {}", evaluationId);

        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new NotFoundException("Evaluaci?n no encontrada"));

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

    @PostMapping("/send")
    public ApiResponse<ChatMessage> sendMessageWithContext(
            @RequestBody ChatRequestV2 request, Principal principal) {
        securityValidator.validateFamilyOwnership(request.getFamilyId(), principal);
        Family family = familyRepository.findById(request.getFamilyId())
                .orElseThrow(() -> new NotFoundException("Familia no encontrada"));
        return ApiResponse.ok(
            aiService.chatWithTransformation(request.getMessage(), family,
                request.getMemberId(), request.getTransformationContext())
        );
    }

    @Data
    public static class ChatRequest {
        private Long familyId;
        private String message;
        private Long memberId;
    }

    /** ChatRequest v2: incluye contexto de transformaci?n del frontend */
    @Data
    public static class ChatRequestV2 {
        private Long familyId;
        private String message;
        private Long memberId;
        private TransformationContextDto transformationContext;

        @Data
        public static class TransformationContextDto {
            private String currentPillar;
            private Integer currentMonth;
            private String milestoneLabel;
            private String currentPhase;
            private Integer sprintNumber;
            private String activeMissionId;
            private Integer progressPercent;
            private Boolean onboardingCompleted;
        }
    }
}


