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

// Mapeo de códigos de hito a días reales (calendaria terapéutica del SDD)
// W1 = 1 semana, M1 = 1 mes, M3 = 3 meses, M6 = 6 meses, M12 = 1 año, M24 = 2 años, M36 = 3 años

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
    private final ContinuityEngine continuityEngine;
    private final PlanValidator planValidator;

    public PlanGenerationService(PlanService planService,
                                EvaluationRepository evaluationRepository,
                                AiService aiService,
                                WhatsAppService whatsappService,
                                ObjectMapper objectMapper,
                                ChecklistService checklistService,
                                MilestoneRepository milestoneRepository,
                                ContinuityEngine continuityEngine,
                                PlanValidator planValidator) {
        this.planService = planService;
        this.evaluationRepository = evaluationRepository;
        this.aiService = aiService;
        this.whatsappService = whatsappService;
        this.objectMapper = objectMapper;
        this.checklistService = checklistService;
        this.milestoneRepository = milestoneRepository;
        this.continuityEngine = continuityEngine;
        this.planValidator = planValidator;
    }

    @RabbitListener(queues = com.integrityfamily.common.config.RabbitConfig.PLAN_QUEUE)
    @Transactional
    public void generatePlanFromEvaluation(Map<String, Object> event) {
        log.info("🚀 [AI_PLAN_ENGINE] Iniciando síntesis híbrida para evento: {}", event);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = null;
            if (event != null && event.containsKey("payload")) {
                Object pObj = event.get("payload");
                if (pObj instanceof Map) {
                    payload = (Map<String, Object>) pObj;
                }
            }
            if (payload == null) {
                payload = event;
            }
            
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
            throw new RuntimeException("Fallo en PlanGenerationService al sintetizar plan híbrido", e);
        }
    }

    private void processHybridPlan(Evaluation evaluation, Map<String, Object> event) {
        log.info("🧬 [PLAN-ENGINE] Iniciando análisis longitudinal y validación del plan...");
        Map<String, Double> dimensions = evaluation.getDimensionScores().stream()
                .collect(Collectors.toMap(EvaluationDimensionScore::getDimensionName, EvaluationDimensionScore::getScore));
        
        String riskLevel = event.getOrDefault("riskLevel", "MEDIUM").toString();

        // 1. Ejecutar ContinuityEngine para obtener el diagnóstico longitudinal
        com.integrityfamily.plan.service.ContinuityEngine.ContinuityAnalysis continuityAnalysis = 
                continuityEngine.analyzeFamilyContinuity(evaluation.getFamily().getId(), evaluation);

        // 2. Generar el plan con el contexto analítico inyectado
        String jsonResponse = aiService.generateHybridPlan(evaluation.getFamily(), dimensions, riskLevel, continuityAnalysis);
        
        try {
            // Parsear respuesta inicial
            HybridPlanDto rawPlanDto = objectMapper.readValue(jsonResponse, HybridPlanDto.class);

            // 3. Pasar por PlanValidator para validar y sanear
            HybridPlanDto planDto = planValidator.validateAndSanitize(rawPlanDto);

            ImprovementPlan p = ImprovementPlan.builder()
                    .family(evaluation.getFamily())
                    .evaluation(evaluation)
                    .title("PLAN DE TRANSFORMACIÓN: " + evaluation.getFamily().getName())
                    .vision3y(planDto.vision3y())
                    .aiReport(jsonResponse)
                    .aiGeneratedAt(LocalDateTime.now())
                    .tasks(new java.util.ArrayList<>())
                    .build();

            List<Milestone> allMilestones = milestoneRepository.findAll();
            for (MilestoneDto mDto : planDto.milestones()) {
                Milestone milestone = milestoneRepository.findByCode(mDto.code())
                        .orElseGet(() -> allMilestones.isEmpty() ? null : allMilestones.get(0));

                for (TaskDto tDto : mDto.tasks()) {
                    int daysForMilestone = resolveMilestoneDays(mDto.code());
                    int periodicityMonths = resolveMilestonePeriodicityMonths(mDto.code());

                    PlanTask task = PlanTask.builder()
                            .plan(p)
                            .title(tDto.title())
                            .dimension(tDto.dimension())
                            .milestone(milestone)
                            .fase(tDto.fase())
                            .riesgoAsociado(tDto.riesgoAsociado())
                            .objetivo(tDto.objetivo())
                            .accionConcreta(tDto.accionConcreta())
                            .indicadorCumplimiento(tDto.indicadorCumplimiento())
                            .evidenciaRequerida(tDto.evidenciaRequerida())
                            .impactoIcf(tDto.impactoIcf())
                            .dueDate(LocalDateTime.now().plusDays(daysForMilestone))
                            .periodicityMonths(periodicityMonths)
                            .completed(false)
                            .steps(new java.util.ArrayList<>())
                            .build();

                    log.info("📅 [PLAN-ENGINE] Microacción '{}' → Hito {} → Vence: {} ({} meses)",
                            tDto.title(), mDto.code(), task.getDueDate().toLocalDate(), periodicityMonths);

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

            // 4. Despachar notificación cálida de bienvenida al nuevo hito por WhatsApp
            try {
                String currentMilestone = evaluation.getFamily().getCurrentMilestone();
                String welcomeMessage = String.format(
                    "🌟 *¡Felicidades Familia %s!* 🌟\n\n" +
                    "Hemos completado con éxito su diagnóstico de convivencia. Su nuevo hito activo es *%s*.\n\n" +
                    "Nuestro motor de IA ha calibrado a la perfección su *Plan de Transformación de 36 Meses* basándose en sus resultados de sintonía. " +
                    "Ya tienen listas nuevas microacciones cotidianas en su línea de tiempo para caminar juntos esta semana.\n\n" +
                    "¡Sigamos sembrando sintonía, amor e integridad en el hogar! 🚀🏡",
                    evaluation.getFamily().getName(),
                    currentMilestone != null ? currentMilestone : "Inicial"
                );
                whatsappService.sendToFamily(evaluation.getFamily(), welcomeMessage);
                log.info("📧 [AI_PLAN_ENGINE] Notificación de bienvenida al hito {} despachada vía WhatsApp.", currentMilestone);
            } catch (Exception we) {
                log.warn("⚠️ [AI_PLAN_ENGINE] No se pudo enviar notificación de bienvenida por WhatsApp: {}", we.getMessage());
            }

        } catch (Exception e) {
            log.error("⚠️ [AI-PARSER] Error en formato JSON Híbrido o procesamiento: {}", e.getMessage(), e);
            try {
                log.info("🛡️ [PLAN-FALLBACK] Activando motor determinístico de contingencia para Evaluación ID: {}", evaluation.getId());
                planService.generateDeterministicPlan(evaluation.getId());
            } catch (Exception ex) {
                log.error("❌ [PLAN-FALLBACK-ERROR] Fallo en motor determinístico: {}", ex.getMessage());
            }
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
        String fase,
        @JsonProperty("riesgo_asociado") String riesgoAsociado,
        String objetivo,
        @JsonProperty("accion_concreta") String accionConcreta,
        @JsonProperty("indicador_cumplimiento") String indicadorCumplimiento,
        @JsonProperty("evidencia_requerida") String evidenciaRequerida,
        @JsonProperty("impacto_icf") Integer impactoIcf,
        List<StepDto> steps
    ) {}

    public record StepDto(
        String type,
        String detail
    ) {}

    /**
     * SDD Calendaria Terapéutica: Resuelve los días exactos de vencimiento
     * para cada código de hito de evolución de la ruta de transformación de 36 meses.
     * Soporta los 14 hitos longitudinales de la transformación familiar.
     */
    private int resolveMilestoneDays(String code) {
        if (code == null) return 30;
        return switch (code.toUpperCase().trim()) {
            case "W1"  -> 7;     // 1 semana
            case "M1"  -> 30;    // 1 mes
            case "M2"  -> 60;    // 2 meses
            case "M3"  -> 90;    // 3 meses
            case "M4"  -> 120;   // 4 meses
            case "M5"  -> 150;   // 5 meses
            case "M6"  -> 180;   // 6 meses
            case "M9"  -> 270;   // 9 meses
            case "M12" -> 365;   // 12 meses (1 año)
            case "M15" -> 455;   // 15 meses
            case "M18" -> 545;   // 18 meses (1.5 años)
            case "M21" -> 635;   // 21 meses
            case "M24" -> 730;   // 24 meses (2 años)
            case "M36" -> 1095;  // 36 meses (3 años)
            default    -> 30;    // Fallback seguro: 1 mes
        };
    }

    /**
     * SDD Calendaria Terapéutica: Resuelve el campo periodicityMonths
     * (usado para la UI de la línea de tiempo evolutiva).
     * Soporta los 14 hitos longitudinales de la transformación familiar.
     */
    private int resolveMilestonePeriodicityMonths(String code) {
        if (code == null) return 1;
        return switch (code.toUpperCase().trim()) {
            case "W1"  -> 0;   // Sub-mensual: semana 1
            case "M1"  -> 1;
            case "M2"  -> 2;
            case "M3"  -> 3;
            case "M4"  -> 4;
            case "M5"  -> 5;
            case "M6"  -> 6;
            case "M9"  -> 9;
            case "M12" -> 12;
            case "M15" -> 15;
            case "M18" -> 18;
            case "M21" -> 21;
            case "M24" -> 24;
            case "M36" -> 36;
            default    -> 1;
        };
    }
}
