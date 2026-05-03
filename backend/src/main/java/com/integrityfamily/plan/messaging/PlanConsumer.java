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
 * SDD-PLAN-01: Consumidor AsÃƒÂ­ncrono de Planes de AcciÃƒÂ³n.
 * Procesa la recomendaciÃƒÂ³n de la IA y la convierte en tareas ejecutables.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PlanConsumer {

    private final PlanTaskService planTaskService;

    @RabbitListener(queues = RabbitConfig.PLAN_QUEUE)
    @Transactional
    public void handlePlanGeneration(DashboardSummaryResponse summary) {
        log.info("Ã°Å¸Å¡â‚¬ [PLAN-CONSUMER] Iniciando orquestaciÃƒÂ³n de tareas para: {}", summary.familyName());

        try {
            // 1. Extraer potenciales tareas del texto de la IA
            List<String> suggestedTasks = parseAiRecommendation(summary.aiRecommendation());

            if (suggestedTasks.isEmpty()) {
                log.warn("Ã¢Å¡Â Ã¯Â¸Â [PLAN-CONSUMER] El reporte de IA no contenÃƒÂ­a acciones claras para procesar.");
                return;
            }

            // 2. Persistir tareas en el Plan de AcciÃƒÂ³n de la familia
            planTaskService.createTasksFromAi(summary.familyId(), suggestedTasks);

            log.info("Ã¢Å“â€¦ [PLAN-CONSUMER] SincronizaciÃƒÂ³n exitosa: {} nuevas tareas para la familia.",
                    suggestedTasks.size());

        } catch (Exception e) {
            log.error("Ã¢ÂÅ’ [PLAN-CONSUMER] Fallo crÃƒÂ­tico en el procesamiento de mensajes: {}", e.getMessage());
        }
    }

    /**
     * Extrae lÃƒÂ­neas que parecen acciones (empiezan con -, * o nÃƒÂºmero)
     * para transformarlas en tareas de base de datos.
     */
    private List<String> parseAiRecommendation(String text) {
        if (text == null || text.isBlank())
            return List.of();

        return Arrays.stream(text.split("\n"))
                .map(String::trim)
                .filter(line -> line.startsWith("-") || line.startsWith("*") || line.matches("^\\d+\\..*"))
                .map(line -> line.replaceAll("^[-*\\d.]+\\s*", "")) // Limpiar viÃƒÂ±etas
                .filter(line -> line.length() > 5) // Filtrar ruido
                .toList();
    }
}


