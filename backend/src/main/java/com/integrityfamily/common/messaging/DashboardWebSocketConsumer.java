package com.integrityfamily.common.messaging;

import com.integrityfamily.common.config.RabbitConfig;
import com.integrityfamily.common.service.NotificationWebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class DashboardWebSocketConsumer {

    private final NotificationWebSocketService webSocketService;

    @RabbitListener(queues = RabbitConfig.WEBSOCKET_DASHBOARD_QUEUE)
    public void handleEvaluationCompleted(Map<String, Object> event) {
        log.info("🌐 [WEBSOCKET-CONSUMER] Recibido evento para procesar por WebSocket: {}", event);
        
        try {
            String routingKey = event.containsKey("routingKey") ? event.get("routingKey").toString() : "UNKNOWN";
            
            if (event != null && event.containsKey("payload")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> payload = (Map<String, Object>) event.get("payload");
                
                if (payload.containsKey("familyId")) {
                    Long familyId = Long.valueOf(payload.get("familyId").toString());
                    
                    String message = "El dashboard ha sido actualizado.";
                    if ("evaluation.completed".equals(routingKey)) {
                        message = "La evaluación ha sido completada y el plan ha sido actualizado.";
                    } else if ("task.completed".equals(routingKey)) {
                        String title = payload.containsKey("title") ? payload.get("title").toString() : "una misión";
                        message = "La misión '" + title + "' ha sido completada.";
                    }
                    
                    // Enviar notificación de actualización al dashboard de la familia
                    webSocketService.sendToFamily(familyId, "/dashboard", Map.of(
                        "type", "DASHBOARD_UPDATE",
                        "message", message,
                        "timestamp", java.time.LocalDateTime.now().toString()
                    ));
                    
                    log.info("🌐 [WEBSOCKET-CONSUMER] Notificación enviada a la familia {} por WebSocket (Motivo: {})", familyId, routingKey);
                }
            }
        } catch (Exception e) {
            log.error("❌ [WEBSOCKET-CONSUMER] Error al procesar evento para WebSocket", e);
        }
    }
}
