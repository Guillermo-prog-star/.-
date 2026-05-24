package com.integrityfamily.plan.messaging;

import com.integrityfamily.analytics.dto.DashboardSummaryResponse;
import com.integrityfamily.common.config.RabbitConfig;
import com.integrityfamily.plan.service.PlanTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * SDD-PLAN-01: Consumidor Asíncrono de Planes de Acción.
 * Procesa la recomendación de la IA y la convierte en tareas estructuradas.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PlanConsumer {

    private final PlanTaskService planTaskService;
    private final com.integrityfamily.ai.service.AiService aiService;
    private final com.integrityfamily.domain.repository.FamilyRepository familyRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitConfig.SUGGESTED_TASKS_QUEUE)
    @Transactional
    public void handlePlanGeneration(DashboardSummaryResponse summary) {
        log.info("🚀 [PLAN-CONSUMER] Iniciando orquestación de tareas para: {}", summary.familyName());

        try {
            // 1. Obtener la familia
            com.integrityfamily.domain.Family family = familyRepository.findById(summary.familyId())
                    .orElseThrow(() -> new RuntimeException("Familia no encontrada: " + summary.familyId()));

            // 2. Generar misiones estructuradas usando la IA
            String jsonMissions = aiService.generateMissions(family);
            
            // 3. Parsear el JSON
            List<com.integrityfamily.plan.dto.PlanDtos.AiMissionProposal> proposals = objectMapper.readValue(
                    jsonMissions, 
                    new com.fasterxml.jackson.core.type.TypeReference<List<com.integrityfamily.plan.dto.PlanDtos.AiMissionProposal>>() {}
            );

            if (proposals.isEmpty()) {
                log.warn("⚠️ [PLAN-CONSUMER] La IA no generó misiones válidas.");
                return;
            }

            // 4. Persistir tareas en el Plan de Acción de la familia
            planTaskService.createTasksFromAi(summary.familyId(), proposals);

            log.info("✅ [PLAN-CONSUMER] Sincronización exitosa: {} nuevas misiones estructuradas para la familia.",
                    proposals.size());

        } catch (Exception e) {
            log.error("❌ [PLAN-CONSUMER] Fallo crítico en el procesamiento de mensajes: {}", e.getMessage());
            throw new RuntimeException("Fallo en PlanConsumer al procesar recomendaciones de la IA", e);
        }
    }

    /**
     * Extrae líneas que parecen acciones (viñetas markdown o párrafos con contenido significativo).
     * Soporta tanto el formato estructurado de Claude como el texto libre del modo simulación.
     */
    private List<String> parseAiRecommendation(String text) {
        if (text == null || text.isBlank())
            return List.of();

        List<String> bulleted = Arrays.stream(text.split("\n"))
                .map(String::trim)
                .filter(line -> line.startsWith("-") || line.startsWith("*") || line.matches("^\\d+\\..*"))
                .map(line -> line.replaceAll("^[-*\\d.]+\\s*", ""))
                .filter(line -> line.length() > 5)
                .toList();

        // Si Claude encontró viñetas, usarlas directamente
        if (!bulleted.isEmpty()) {
            return bulleted;
        }

        // Fallback: parsear párrafos (modo simulación sin Claude)
        return Arrays.stream(text.split("[.!?]"))
                .map(String::trim)
                .filter(sentence -> sentence.length() > 20) // Oración suficientemente larga
                .limit(3)                                    // Máximo 3 tareas por ciclo
                .toList();
    }
}
