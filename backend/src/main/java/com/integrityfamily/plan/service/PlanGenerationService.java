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
import com.integrityfamily.domain.repository.ImprovementPlanRepository;
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
    private final ImprovementPlanRepository planRepository;

    public PlanGenerationService(PlanService planService,
                                EvaluationRepository evaluationRepository,
                                AiService aiService,
                                WhatsAppService whatsappService,
                                ObjectMapper objectMapper,
                                ChecklistService checklistService,
                                MilestoneRepository milestoneRepository,
                                ContinuityEngine continuityEngine,
                                PlanValidator planValidator,
                                ImprovementPlanRepository planRepository) {
        this.planService = planService;
        this.evaluationRepository = evaluationRepository;
        this.aiService = aiService;
        this.whatsappService = whatsappService;
        this.objectMapper = objectMapper;
        this.checklistService = checklistService;
        this.milestoneRepository = milestoneRepository;
        this.continuityEngine = continuityEngine;
        this.planValidator = planValidator;
        this.planRepository = planRepository;
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
            // Parsear respuesta inicial (Extrayendo el bloque JSON si hay texto libre)
            String jsonContent = extractJson(jsonResponse);
            HybridPlanDto planDto = objectMapper.readValue(jsonContent, HybridPlanDto.class);

            // De-duplicación estricta (SDD SPEC): Solo 1 plan activo por familia
            List<ImprovementPlan> existingPlans = planRepository.findByFamilyId(evaluation.getFamily().getId());
            if (existingPlans != null && !existingPlans.isEmpty()) {
                log.info("🧹 [PLAN-ENGINE] Eliminando {} planes duplicados existentes para la familia ID: {}", 
                        existingPlans.size(), evaluation.getFamily().getId());
                planRepository.deleteAll(existingPlans);
                planRepository.flush();
            }

            ImprovementPlan p = ImprovementPlan.builder()
                    .family(evaluation.getFamily())
                    .evaluation(evaluation)
                    .title("PLAN DE TRANSFORMACIÓN: " + evaluation.getFamily().getName())
                    .vision3y(planDto.vision() != null ? planDto.vision().vision3y() : "Construir una convivencia más tranquila y conectada.")
                    .aiReport(jsonResponse)
                    .aiGeneratedAt(LocalDateTime.now())
                    .tasks(new java.util.ArrayList<>())
                    .build();

            List<Milestone> allMilestones = milestoneRepository.findAll();
            
            if (planDto.milestones() != null) {
                for (MilestoneDto mDto : planDto.milestones()) {
                    Milestone milestone = milestoneRepository.findByCode(mDto.code())
                            .orElseGet(() -> allMilestones.isEmpty() ? null : allMilestones.get(0));

                    if (mDto.microActions() != null) {
                        for (MicroActionDto tDto : mDto.microActions()) {
                            int daysForMilestone = resolveMilestoneDays(mDto.code());
                            int periodicityMonths = resolveMilestonePeriodicityMonths(mDto.code());

                            String resolvedFase = tDto.fase() != null ? tDto.fase().toUpperCase().trim() : resolveFase(mDto.code());
                            String resolvedDimension = tDto.dimension() != null ? tDto.dimension().toUpperCase().trim() : resolveDimension(mDto.code());

                            PlanTask task = PlanTask.builder()
                                    .plan(p)
                                    .title(tDto.title())
                                    .dimension(resolvedDimension)
                                    .milestone(milestone)
                                    .fase(resolvedFase)
                                    .objetivo(mDto.goal()) // Usamos la meta del hito como objetivo
                                    .accionConcreta(tDto.description())
                                    .indicadorCumplimiento("Completar la acción")
                                    .evidenciaRequerida(tDto.evidenceType())
                                    .impactoIcf(5) // Default impact
                                    .dueDate(LocalDateTime.now().plusDays(daysForMilestone))
                                    .periodicityMonths(periodicityMonths)
                                    .completed(false)
                                    .steps(new java.util.ArrayList<>())
                                    .build();

                            log.info("📅 [PLAN-ENGINE] Microacción '{}' (fase: {}, dim: {}) → Hito {} → Vence: {} ({} meses)",
                                    tDto.title(), resolvedFase, resolvedDimension, mDto.code(), task.getDueDate().toLocalDate(), periodicityMonths);

                            p.getTasks().add(task);
                        }
                    }
                }
            }

            if (p.getTasks().isEmpty()) {
                throw new RuntimeException("AI generó un plan vacío — activando motor determinístico de contingencia.");
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
        @JsonProperty("family_state") FamilyStateDto familyState,
        VisionDto vision,
        List<MilestoneDto> milestones
    ) {}

    public record FamilyStateDto(
        String risk,
        Integer icf,
        @JsonProperty("main_problem") String mainProblem
    ) {}

    public record VisionDto(
        @JsonProperty("3y") String vision3y
    ) {}

    public record MilestoneDto(
        String code,
        String goal,
        @JsonProperty("micro_actions") List<MicroActionDto> microActions
    ) {}

    public record MicroActionDto(
        String title,
        String description,
        @JsonProperty("duration_minutes") Integer durationMinutes,
        List<String> participants,
        @JsonProperty("evidence_type") String evidenceType,
        String fase,
        String dimension
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

    private String resolveFase(String code) {
        if (code == null) return "RECONOCIMIENTO";
        return switch (code.toUpperCase().trim()) {
            case "W1", "M1", "M2", "M3" -> "RECONOCIMIENTO";
            case "M4", "M5", "M6", "M9", "M12" -> "AMOR";
            case "M15", "M18", "M21", "M24", "M36" -> "ENTREGA";
            default -> "RECONOCIMIENTO";
        };
    }

    private String resolveDimension(String code) {
        if (code == null) return "EMOCIONES";
        return switch (code.toUpperCase().trim()) {
            case "W1", "M1" -> "EMOCIONES";
            case "M2", "M3", "M4" -> "COMUNICACION";
            case "M5", "M6", "M9" -> "HABITOS";
            case "M12", "M15", "M18", "M21", "M24", "M36" -> "TIEMPOS";
            default -> "EMOCIONES";
        };
    }

    private String extractJson(String response) {
        if (response == null) return "{}";
        
        // Intentar buscar el bloque delimitado por ```json y ```
        int startIndex = response.indexOf("```json");
        if (startIndex != -1) {
            int endIndex = response.indexOf("```", startIndex + 7);
            if (endIndex != -1) {
                return response.substring(startIndex + 7, endIndex).trim();
            }
        }
        
        // Fallback: buscar el primer '{' y el último '}'
        int firstBrace = response.indexOf("{");
        int lastBrace = response.lastIndexOf("}");
        if (firstBrace != -1 && lastBrace != -1 && firstBrace < lastBrace) {
            return response.substring(firstBrace, lastBrace + 1).trim();
        }
        
        return response.trim();
    }
}
