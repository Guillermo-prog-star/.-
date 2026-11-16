package com.integrityfamily.evaluation.service.post;

import com.integrityfamily.common.config.RabbitConfig;
import com.integrityfamily.common.event.AfterCommit;
import com.integrityfamily.common.event.SystemEvent;
import com.integrityfamily.domain.Evaluation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Publica "evaluation.completed" DESPUÉS del commit (ADR-020, corte A).
 *
 * <p>Antes se enviaba en el acto: un consumidor podía adelantarse a la transacción
 * (EVALUATION_NOT_FOUND) o recibir el evento de una evaluación revertida.
 * Un fallo de RabbitMQ se registra y no se propaga (resiliencia).
 */
@Slf4j
@Component
@Order(110)
@RequiredArgsConstructor
public class CompletedEventStep implements PostFinalizationStep {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public String name() { return "EVALUATION_COMPLETED_EVENT"; }

    @Override
    public void execute(PostFinalizationContext ctx) {
        Evaluation saved = ctx.evaluation();
        final Long familyId = saved.getFamily().getId();
        final String riskLevel = ctx.riskLevel();

        Map<String, Object> payload = new HashMap<>();
        payload.put("evaluationId", saved.getId());
        payload.put("icf", saved.getIcf());
        payload.put("familyId", familyId);
        payload.put("riskLevel", riskLevel);

        SystemEvent event = SystemEvent.of("evaluation.completed", familyId, payload, "SYSTEM");

        AfterCommit.run(() -> {
            try {
                rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, "evaluation.completed", event);
                log.info("📧 [EVALUATION] Evento 'evaluation.completed' enviado para familia: {} con riesgo: {}",
                        familyId, riskLevel);
            } catch (Exception e) {
                log.error("❌ [EVALUATION] Error al publicar evento 'evaluation.completed' a RabbitMQ (resiliencia activada): {}",
                        e.getMessage());
            }
        });
    }
}
