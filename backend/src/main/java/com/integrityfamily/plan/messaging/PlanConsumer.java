package com.integrityfamily.plan.messaging;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.ai.service.AiService;
import com.integrityfamily.analytics.dto.DashboardSummaryResponse;
import com.integrityfamily.common.config.RabbitConfig;
import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.plan.dto.PlanDtos.AiMissionProposal;
import com.integrityfamily.plan.service.PlanTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SDD-PLAN-01: Consumidor Asíncrono de Planes de Acción.
 * Procesa la recomendación de la IA y la convierte en tareas estructuradas.
 *
 * Política de reintentos:
 *   - Errores recuperables (ej: BD caída) → RuntimeException → RabbitMQ reencola.
 *   - Errores irrecuperables (ej: JSON inválido de IA) → AmqpRejectAndDontRequeueException → descarta el mensaje.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PlanConsumer {

    private final PlanTaskService    planTaskService;
    private final AiService          aiService;
    private final FamilyRepository   familyRepository;
    private final ObjectMapper       objectMapper;

    @RabbitListener(queues = RabbitConfig.SUGGESTED_TASKS_QUEUE)
    @Transactional
    public void handlePlanGeneration(DashboardSummaryResponse summary) {
        log.info("🚀 [PLAN-CONSUMER] Iniciando orquestación de tareas para: {}", summary.familyName());

        try {
            // 1. Obtener la familia
            Family family = familyRepository.findById(summary.familyId())
                    .orElseThrow(() -> new BusinessException(
                            "Familia no encontrada: " + summary.familyId(),
                            "FAMILY_NOT_FOUND",
                            HttpStatus.NOT_FOUND));

            // 2. Generar misiones estructuradas usando la IA
            String jsonMissions = aiService.generateMissions(family);

            // 3. Parsear el JSON — limpiar posible markdown wrapper antes de parsear
            List<AiMissionProposal> proposals;
            try {
                String cleanJson = stripMarkdownCodeBlock(jsonMissions);
                proposals = objectMapper.readValue(
                        cleanJson,
                        new TypeReference<List<AiMissionProposal>>() {});
            } catch (Exception parseEx) {
                log.error("❌ [PLAN-CONSUMER] JSON de IA inválido — descartando mensaje sin reintentar: {}", parseEx.getMessage());
                throw new AmqpRejectAndDontRequeueException("Payload de IA no parseable", parseEx);
            }

            if (proposals.isEmpty()) {
                log.warn("⚠️ [PLAN-CONSUMER] La IA no generó misiones válidas.");
                return;
            }

            // 4. Persistir tareas en el Plan de Acción de la familia
            planTaskService.createTasksFromAi(summary.familyId(), proposals);

            log.info("✅ [PLAN-CONSUMER] Sincronización exitosa: {} nuevas misiones estructuradas para la familia.",
                    proposals.size());

        } catch (AmqpRejectAndDontRequeueException e) {
            // Re-lanzar sin envolver — ya es el tipo correcto
            throw e;
        } catch (Exception e) {
            // Error recuperable (BD, red, etc.) → RabbitMQ reencola automáticamente
            log.error("❌ [PLAN-CONSUMER] Fallo crítico en el procesamiento de mensajes: {}", e.getMessage());
            throw new RuntimeException("Fallo en PlanConsumer al procesar recomendaciones de la IA", e);
        }
    }

    /** Elimina bloques markdown ```json ... ``` que la IA puede añadir alrededor del JSON. */
    private String stripMarkdownCodeBlock(String raw) {
        if (raw == null) return "[]";
        String trimmed = raw.strip();
        // Quitar apertura: ```json o ``` seguido de posible salto de línea
        if (trimmed.startsWith("```")) {
            int newline = trimmed.indexOf('\n');
            if (newline != -1) trimmed = trimmed.substring(newline + 1).strip();
        }
        // Quitar cierre: ```
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3).strip();
        }
        return trimmed;
    }
}
