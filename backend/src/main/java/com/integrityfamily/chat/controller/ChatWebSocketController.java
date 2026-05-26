package com.integrityfamily.chat.controller;

import com.integrityfamily.ai.service.AiService;
import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.common.service.NotificationWebSocketService;
import com.integrityfamily.domain.ChatMessage;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@Slf4j
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final AiService aiService;
    private final FamilyRepository familyRepository;
    private final NotificationWebSocketService webSocketService;

    @MessageMapping("/chat.send")
    public void handleChatMessage(@Payload ChatWebSocketRequest request) {
        log.info("💬 [CHAT-WS] Mensaje recibido de la familia {}: {}", request.getFamilyId(), request.getMessage());
        
        try {
            Family family = familyRepository.findById(request.getFamilyId())
                    .orElseThrow(() -> new BusinessException("Familia no encontrada", "FAMILY_NOT_FOUND", HttpStatus.NOT_FOUND));

            // Procesar el chat (guarda mensaje y genera respuesta)
            ChatMessage aiResponse = aiService.processInteractiveChat(request.getMessage(), family);

            // Enviar la respuesta de la IA de vuelta por WebSocket
            webSocketService.sendToFamily(request.getFamilyId(), "/chat", Map.of(
                "type", "AI_RESPONSE",
                "message", aiResponse.getContent(),
                "timestamp", java.time.LocalDateTime.now().toString()
            ));
            
            log.info("💬 [CHAT-WS] Respuesta de IA enviada a la familia {} por WebSocket", request.getFamilyId());
        } catch (Exception e) {
            log.error("❌ [CHAT-WS] Error al procesar mensaje de chat", e);
        }
    }

    @Data
    public static class ChatWebSocketRequest {
        private Long familyId;
        private String message;
    }
}
