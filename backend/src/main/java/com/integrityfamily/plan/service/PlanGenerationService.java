package com.integrityfamily.plan.service;

import com.integrityfamily.domain.ImprovementPlan;
import com.integrityfamily.domain.PlanTask;
import com.integrityfamily.domain.PlanTaskStep;
import com.integrityfamily.domain.StepType;
import com.integrityfamily.domain.Milestone;
import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.EvaluationDimensionScore;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.repository.MilestoneRepository;
import com.integrityfamily.ai.service.AiService;
import com.integrityfamily.common.service.WhatsAppService;
import com.integrityfamily.checklist.service.ChecklistService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SDD SPEC 6.5: Motor de Generación de Planes Híbridos.
 */
@Slf4j
@Service
public class PlanGenerationService {

    private final PlanService planService;
    private final EvaluationRepository evaluationRepository;
    private final AiService aiService;
    private final WhatsAppService whatsappService;
    private final ObjectMapper objectMapper;
    private final ChecklistService checklistService;
    private final MilestoneRepository milestoneRepository;

    public PlanGenerationService(PlanService planService,
                                EvaluationRepository evaluationRepository,
                                AiService aiService,
                                WhatsAppService whatsappService,
                                ObjectMapper objectMapper,
                                ChecklistService checklistService,
                                MilestoneRepository milestoneRepository) {
        this.planService = planService;
        this.evaluationRepository = evaluationRepository;
        this.aiService = aiService;
        this.whatsappService = whatsappService;
        this.objectMapper = objectMapper;
        this.checklistService = checklistService;
        this.milestoneRepository = milestoneRepository;
    }

    @RabbitListener(queues = "${app.messaging.queues.plan:if.plan.queue}")
    @Transactional
    public void generatePlanFromEvaluation(Map<String, Object> event) {
        log.info("🚀 [AI_PLAN_ENGINE] Iniciando síntesis híbrida para evento: {}", event);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) event.get("payload");
            
            if (payload == null || payload.get("evaluationId") == null) {
                log.warn("⚠️ [AI_PLAN_ENGINE] Evento recibido sin payload de evaluación válido.");
                return;
            }
            
            Long evaluationId = Long.valueOf(payload.get("evaluationId").toString());
            Evaluation evaluation = evaluationRepository.findById(evaluationId)
                    .orElseThrow(() -> new RuntimeException("Evaluación no encontrada: " + evaluationId));

            // 1. Generar Síntesis Espiritual Asíncrona (Desbloqueo de UI)
            try {
                log.info("🧠 [AI_PLAN_ENGINE] Generando síntesis espiritual en background...");
                String synthesis = aiService.generateExecutiveSynthesis(evaluation);
                evaluation.setSpiritualSynthesis(synthesis);
                evaluationRepository.save(evaluation);
            } catch (Exception e) {
                log.error("⚠️ [AI_PLAN_ENGINE] Fallo en síntesis: {}", e.getMessage());
            }

            // 2. Procesar Plan Híbrido
            processHybridPlan(evaluation, payload);

        } catch (Exception e) {
            log.error("❌ [PLAN-ENGINE-ERROR] Fallo crítico al procesar evento: {}", e.getMessage());
        }
    }

    private void processHybridPlan(Evaluation evaluation, Map<String, Object> event) {
        Map<String, Double> dimensions = evaluation.getDimensionScores().stream()
                .collect(Collectors.toMap(EvaluationDimensionScore::getDimensionName, EvaluationDimensionScore::getScore));
        
        String riskLevel = event.getOrDefault("riskLevel", "MEDIUM").toString();

        String jsonResponse = aiService.generateHybridPlan(evaluation.getFamily(), dimensions, riskLevel);
        
        try {
            HybridPlanDto planDto = objectMapper.readValue(jsonResponse, HybridPlanDto.class);

            ImprovementPlan p = new ImprovementPlan();
            p.setFamily(evaluation.getFamily());
            p.setEvaluation(evaluation);
            p.setTitle("PLAN DE TRANSFORMACIÓN: " + evaluation.getFamily().getName());
            p.setVision3y(planDto.vision3y());
            p.setAiReport(jsonResponse);
            p.setAiGeneratedAt(LocalDateTime.now());

            for (MilestoneDto mDto : planDto.milestones()) {
                Milestone milestone = milestoneRepository.findByCode(mDto.code())
                        .orElseGet(() -> milestoneRepository.findAll().get(0));

                for (TaskDto tDto : mDto.tasks()) {
                    PlanTask task = new PlanTask();
                    task.setPlan(p);
                    task.setTitle(tDto.title());
                    task.setDimension(tDto.dimension());
                    task.setMilestone(milestone);
                    
                    for (StepDto sDto : tDto.steps()) {
                        try {
                            PlanTaskStep step = new PlanTaskStep();
                            step.setTask(task);
                            // Robustez ante variaciones de la IA (case-insensitive)
                            String typeStr = sDto.type() != null ? sDto.type().toUpperCase().trim() : "PLANIFICAR";
                            step.setType(StepType.valueOf(typeStr));
                            step.setDetail(sDto.detail());
                            task.getSteps().add(step);
                        } catch (Exception e) {
                            log.warn("⚠️ [AI-PARSER] Tipo de paso inválido: {}. Ignorando.", sDto.type());
                        }
                    }
                    p.getTasks().add(task);
                }
            }

            planService.createPlan(p);
            log.info("✅ [PLAN-ENGINE] Plan Híbrido persistido con éxito.");

        } catch (Exception e) {
            log.error("⚠️ [AI-PARSER] Error en formato JSON Híbrido: {}", e.getMessage());
        }
    }

    public record HybridPlanDto(
        @JsonProperty("vision_3y") String vision3y,
        List<MilestoneDto> milestones
    ) {}

    public record MilestoneDto(
        String code,
        String objective,
        List<TaskDto> tasks
    ) {}

    public record TaskDto(
        String title,
        String dimension,
        List<StepDto> steps
    ) {}

    public record StepDto(
        String type,
        String detail
    ) {}
}
