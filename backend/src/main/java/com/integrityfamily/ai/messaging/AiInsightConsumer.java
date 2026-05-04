package com.integrityfamily.ai.messaging;

import com.integrityfamily.ai.service.AiService;
import com.integrityfamily.bitacora.dto.BitacoraRequest;
import com.integrityfamily.bitacora.service.BitacoraService;
import com.integrityfamily.common.config.RabbitConfig;
import com.integrityfamily.common.event.SystemEvent;
import com.integrityfamily.domain.repository.EvaluationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * SDD: Motor de Insights de IA (Consumer).
 * Escucha eventos de evaluación y genera bitácoras automáticas de aprendizaje.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiInsightConsumer {

    private final AiService aiService;
    private final BitacoraService bitacoraService;

    @RabbitListener(queues = RabbitConfig.AI_INSIGHTS_QUEUE)
    public void onEvaluationCompleted(SystemEvent event) {
        log.info("🤖 [AI-MOTOR] Analizando evento de evaluación para familia: {}", event.familyId());

        try {
            // 1. Recuperar contexto de la evaluación
            Double icf = Double.valueOf(event.payload().toString());
            
            // 2. Generar Recomendación Profunda via IA (Resolviendo ambigüedad de firma)
            String insight = aiService.generateExecutiveSynthesis((Long) event.familyId());

            // 3. Crear registro en Bitácora Automática
            BitacoraRequest bitacoraReq = new BitacoraRequest(
                event.familyId(),
                "EVALUATION",
                0L, 
                insight,
                "Hipótesis evolutiva basada en el ICF de " + icf,
                "Implementar las recomendaciones de la IA",
                "PENDIENTE"
            );

            bitacoraService.createEntry(bitacoraReq);
            log.info("✅ [AI-MOTOR] Insight generado y persistido en Bitácora para familia {}", event.familyId());

        } catch (Exception e) {
            log.error("❌ [AI-MOTOR] Error procesando insight: {}", e.getMessage());
        }
    }
}
