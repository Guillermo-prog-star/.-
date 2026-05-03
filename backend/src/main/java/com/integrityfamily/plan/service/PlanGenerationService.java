package com.integrityfamily.plan.service;

import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.ai.service.AiService;
import com.integrityfamily.common.service.WhatsAppService;
import com.integrityfamily.checklist.service.ChecklistService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PlanGenerationService {

    private final PlanService planService;
    private final EvaluationRepository evaluationRepository;
    private final AiService aiService;
    private final WhatsAppService whatsappService;
    private final ObjectMapper objectMapper;
    private final ChecklistService checklistService;

    public PlanGenerationService(PlanService planService,
                               EvaluationRepository evaluationRepository,
                               AiService aiService,
                               WhatsAppService whatsappService,
                               ObjectMapper objectMapper,
                               ChecklistService checklistService) {
        this.planService = planService;
        this.evaluationRepository = evaluationRepository;
        this.aiService = aiService;
        this.whatsappService = whatsappService;
        this.objectMapper = objectMapper;
        this.checklistService = checklistService;
    }

    @RabbitListener(queues = "${app.messaging.queues.plan:if.plan.queue}")
    public void generatePlanFromEvaluation(Map<String, Object> event) {
        log.info("🚀 [AI_PLAN_ENGINE] Iniciando síntesis generativa para evento: {}", event);

        try {
            if (event.get("evaluationId") == null) return;
            
            Long evaluationId = Long.valueOf(event.get("evaluationId").toString());
            Evaluation evaluation = evaluationRepository.findById(evaluationId)
                    .orElseThrow(() -> new RuntimeException("Evaluación no encontrada: " + evaluationId));

            boolean isCrisis = Boolean.TRUE.equals(evaluation.getHasCrisis()) || 
                               (evaluation.getIcf() != null && evaluation.getIcf() < 40.0);

            if (isCrisis) {
                processCrisisPlan(evaluation);
            } else {
                processStandardRoadmap(evaluation, event);
            }

        } catch (Exception e) {
            log.error("❌ [PLAN-ENGINE-ERROR] Fallo crítico: {}", e.getMessage());
        }
    }

    private void processStandardRoadmap(Evaluation evaluation, Map<String, Object> event) {
        String milestone = evaluation.getFamily().getCurrentMilestone();
        boolean isFirst = planService.findByFamilyId(evaluation.getFamily().getId()).isEmpty();

        Plan p = new Plan();
        p.setFamily(evaluation.getFamily());
        p.setEvaluation(evaluation);
        p.setTitle(isFirst ? "HOJA DE RUTA: " + milestone : "AJUSTE TÁCTICO: " + milestone);
        p.setDescription(isFirst ? "Activación de transformación 36 meses." : "Evolución basada en progreso actual.");

        injectEvolutionaryMissions(p, evaluation, event);
        planService.createPlan(p);

        log.info("✅ [PLAN-ENGINE] Plan '{}' creado satisfactoriamente.", p.getTitle());
    }

    private void injectEvolutionaryMissions(Plan p, Evaluation eval, Map<String, Object> event) {
        // 1. Clasificación de Dimensiones (Hechos)
        Map<String, Double> dimensions = eval.getDimensionScores().stream()
                .collect(Collectors.toMap(EvaluationDimensionScore::getDimensionName, EvaluationDimensionScore::getScore));
        
        String riskLevel = event.getOrDefault("riskLevel", "MEDIUM").toString();

        // 2. Invocación IA
        String jsonResponse = aiService.generateEvolutionaryMissions(eval.getFamily(), dimensions, riskLevel);
        
        p.setAiReport(eval.getSpiritualSynthesis());
        p.setAiGeneratedAt(LocalDateTime.now());

        try {
            // 3. Parseo Resiliente
            List<AiMissionDto> missions = objectMapper.readValue(jsonResponse, new TypeReference<List<AiMissionDto>>() {});
            
            for (AiMissionDto m : missions) {
                PlanTask task = new PlanTask();
                task.setPlan(p);
                task.setTitle(m.title());
                task.setDescription(m.description());
                task.setDimension(m.dimension() != null ? m.dimension() : "GENERAL");
                
                int months = m.periodicityMonths() > 0 ? m.periodicityMonths() : 1;
                task.setPeriodicityMonths(months);
                task.setDueDate(LocalDateTime.now().plusMonths(months));
                
                p.getTasks().add(task);
            }
        } catch (Exception e) {
            log.warn("⚠️ [AI-PARSER] Error en formato JSON. Activando Fallback Estructural.");
            applyStructuralFallback(p, dimensions);
        }

        // Sincronización con Checklist (Retrocompatibilidad)
        checklistService.extractAndAdd(jsonResponse, "EVO_" + eval.getId(), eval.getFamily().getId());
    }

    private void applyStructuralFallback(Plan p, Map<String, Double> dimensions) {
        String weakDimension = dimensions.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("INTEGRIDAD");

        PlanTask task = new PlanTask();
        task.setPlan(p);
        task.setTitle("Consolidación en " + weakDimension);
        task.setDescription("Foco prioritario en fortalecer la base de " + weakDimension + " tras inconsistencia en el reporte de IA.");
        task.setDimension(weakDimension);
        task.setDueDate(LocalDateTime.now().plusWeeks(2));
        p.getTasks().add(task);
    }

    private void processCrisisPlan(Evaluation evaluation) {
        Plan p = new Plan();
        p.setFamily(evaluation.getFamily());
        p.setEvaluation(evaluation);
        p.setTitle("⚠️ PROTOCOLO DE CONTENCIÓN SENTINEL");
        p.setDescription("Intervención inmediata por riesgo crítico detectado.");
        
        String jsonMissions = aiService.generateMissions(evaluation.getFamily());
        checklistService.extractAndAdd(jsonMissions, "CRISIS_" + evaluation.getId(), evaluation.getFamily().getId());
        
        planService.createPlan(p);
        log.error("🚨 [SENTINEL] Plan de Crisis generado para Familia: {}", evaluation.getFamily().getName());
    }

    private record AiMissionDto(String title, String description, String dimension, int periodicityMonths) {}
}


