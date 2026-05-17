package com.integrityfamily.chat.controller;

import com.integrityfamily.domain.ChatMessage;
import com.integrityfamily.domain.repository.ChatMessageRepository;
import com.integrityfamily.ai.service.AiService;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.common.exception.NotFoundException;
import com.integrityfamily.common.security.SecurityValidator;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

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
    private final EvaluationRepository evaluationRepository; // FIX: InyecciÃƒÂ³n aÃƒÂ±adida
    private final AiService aiService;
    private final SecurityValidator securityValidator;

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

        return ApiResponse.ok(aiService.chat(request.getMessage(), family));
    }

    /**
     * Genera reportes de bienestar basados en una evaluaciÃƒÂ³n especÃƒÂ­fica.
     * ResoluciÃƒÂ³n del error de sÃƒÂ­mbolo para evaluationRepository.
     */
    @PostMapping("/report/{evaluationId}")
    public ApiResponse<String> generateAutoReport(@PathVariable Long evaluationId) {
        log.info("Ã°Å¸â€œÅ  [CHAT-CONTROLLER] Generating auto-report for evaluation: {}", evaluationId);

        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new NotFoundException("EvaluaciÃƒÂ³n no encontrada"));

        String advice = aiService.generateDashboardInsight(
                evaluation.getFamily(),
                Map.of("Reconocimiento", 3.0), // Contexto simulado (SDD-MOCK)
                "MEDIUM");

        return ApiResponse.ok(advice);
    }

    @Data
    public static class ChatRequest {
        private Long familyId;
        private String message;
    }
}


