package com.integrityfamily.plan.service;

import com.integrityfamily.evaluation.domain.Evaluation;
import com.integrityfamily.evaluation.repository.EvaluationRepository;
import com.integrityfamily.plan.domain.Plan;
import com.integrityfamily.plan.domain.PlanTask;
import com.integrityfamily.common.service.AiService;
import com.integrityfamily.common.service.WhatsAppService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@Service
public class PlanGenerationService {

    private static final Logger log = LoggerFactory.getLogger(PlanGenerationService.class);

    private final PlanService planService;
    private final EvaluationRepository evaluationRepository;
    private final AiService aiService;
    private final WhatsAppService whatsappService;
    private final ObjectMapper objectMapper;

    public PlanGenerationService(PlanService planService, 
                                 EvaluationRepository evaluationRepository, 
                                 AiService aiService, 
                                 WhatsAppService whatsappService, 
                                 ObjectMapper objectMapper) {
        this.planService = planService;
        this.evaluationRepository = evaluationRepository;
        this.aiService = aiService;
        this.whatsappService = whatsappService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "${app.messaging.queues.plan:if.plan.queue}")
    public void generatePlanFromEvaluation(Map<String, Object> event) {
        log.info("🔥 [AI-PLAN] Iniciando generación generativa para evento: {}", event);

        try {
            if (event.get("evaluationId") == null) return;

            Long evaluationId = Long.valueOf(event.get("evaluationId").toString());
            Evaluation evaluation = evaluationRepository.findById(evaluationId).orElse(null);
            
            if (evaluation == null) return;

            String milestone = evaluation.getMilestoneKey() != null ? evaluation.getMilestoneKey() : "inicio";
            String riskLevel = (String) event.getOrDefault("riskLevel", "MEDIUM");
            boolean hasCrisis = (boolean) event.getOrDefault("requiresImmediatePlan", false);

            Plan newPlan = new Plan();
            newPlan.setFamily(evaluation.getFamily());
            newPlan.setEvaluation(evaluation);
            newPlan.setTitle("Ruta de Transformación: " + milestone);
            newPlan.setDescription("Este plan ha sido diseñado por nuestro Consultor de IA basado en tu diagnóstico.");

            // --- LLAMADA A IA PARA MISIONES REALES ---
            String jsonMissions = aiService.generateMissions(event);
            try {
                List<Map<String, String>> tasks = objectMapper.readValue(jsonMissions, new com.fasterxml.jackson.core.type.TypeReference<>() {});
                for (Map<String, String> t : tasks) {
                    PlanTask task = new PlanTask();
                    task.setPlan(newPlan);
                    task.setTitle(t.getOrDefault("title", "Misión de Bienestar"));
                    task.setDescription(t.getOrDefault("description", "Seguir las recomendaciones del mentor."));
                    newPlan.getTasks().add(task);
                }
            } catch (Exception e) {
                log.warn("⚠️ Fallo al parsear JSON de IA, usando tareas de contingencia.");
                PlanTask fallbackTask = new PlanTask();
                fallbackTask.setPlan(newPlan);
                fallbackTask.setTitle("Diálogo Familiar");
                fallbackTask.setDescription("Validación de resultados.");
                newPlan.getTasks().add(fallbackTask);
            }

            // Persistencia del plan
            planService.createPlan(newPlan);
            log.info("✅ [AI-PLAN] Plan estratégico generado guardado correctamente.");

            // --- NOTIFICACIÓN PROACTIVA WHATSAPP ---
            String whatsappNum = evaluation.getFamily().getWhatsapp();
            if (whatsappNum != null && !whatsappNum.isBlank()) {
                String topic = hasCrisis ? "medidas de apoyo inmediato" : "un nuevo plan de bienestar familiar";
                String warmMessage = aiService.generateNotificationCopy(evaluation.getFamily().getName(), topic);
                whatsappService.sendMessage(whatsappNum, warmMessage);
            }

        } catch (Exception e) {
            log.error("❌ ERROR CRÍTICO en motor de IA", e);
        }
    }
}