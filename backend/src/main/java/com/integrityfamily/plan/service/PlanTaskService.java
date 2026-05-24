package com.integrityfamily.plan.service;

import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.ImprovementPlan;
import com.integrityfamily.domain.PlanTask;
import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.repository.ImprovementPlanRepository;
import com.integrityfamily.ai.service.AiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * SDD SPEC: Motor de Ejecución de Planes Harmonizado.
 * Refactorizado para usar ImprovementPlan y la estructura de dominio centralizada.
 * Adaptado para el Diagnóstico Familiar Consciente.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanTaskService {

    private final ImprovementPlanRepository planRepository;
    private final FamilyRepository familyRepository;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    public record DiagnosticMissionProposal(
        String title,
        String description,
        String dimension,
        String objective,
        String successMetric,
        Integer estimatedDuration
    ) {}

    public void createTasksFromAi(Long familyId, List<com.integrityfamily.plan.dto.PlanDtos.AiMissionProposal> tasks) {
        log.info("📌 [PLAN-TASK] Iniciando persistencia de {} tareas para familia ID: {}", tasks.size(), familyId);

        List<ImprovementPlan> existingPlans = planRepository.findByFamilyId(familyId);
        ImprovementPlan plan = (existingPlans != null && !existingPlans.isEmpty()) ?
                existingPlans.get(existingPlans.size() - 1) :
                createDefaultPlan(familyId);

        com.integrityfamily.domain.Family family = plan.getFamily();
        String currentMilestone = family != null ? family.getCurrentMilestone() : "W1";
        String activeFase = resolveFaseFromMilestone(currentMilestone);

        tasks.forEach(proposal -> {
            PlanTask task = PlanTask.builder()
                    .title(proposal.title() != null ? proposal.title() : "Misión Sugerida por IA")
                    .description(proposal.description())
                    .completed(false)
                    .plan(plan)
                    .dimension(proposal.dimension())
                    .riesgoAsociado(proposal.problemDetected())
                    .objetivo(proposal.objective())
                    .indicadorCumplimiento(proposal.successMetric())
                    .accionConcreta(proposal.missionType() + " - " + proposal.frequency())
                    .impactoIcf(proposal.riskLevel() != null && proposal.riskLevel().equalsIgnoreCase("high") ? 20 : 10)
                    .fase(activeFase)
                    .build();
            plan.getTasks().add(task);
        });

        planRepository.save(plan);
        log.info("✅ [PLAN-TASK] Sincronización exitosa.");
    }

    /**
     * Genera misiones automáticas personalizadas de acuerdo con el Diagnóstico Familiar Consciente.
     */
    @Transactional
    public void generateTasksFromDiagnosis(Evaluation evaluation) {
        if (evaluation == null || evaluation.getMember() == null) {
            log.warn("⚠️ [PLAN-TASK] No se pueden generar tareas: Evaluación o Miembro nulo.");
            return;
        }
        
        Long familyId = evaluation.getFamily().getId();
        String role = evaluation.getMember().getRole();
        
        log.info("🎯 [PLAN-TASK] Iniciando generación de misiones automáticas de diagnóstico para rol: {} en familia ID: {}", role, familyId);

        List<ImprovementPlan> existingPlans = planRepository.findByFamilyId(familyId);
        ImprovementPlan plan = (existingPlans != null && !existingPlans.isEmpty()) ?
                existingPlans.get(existingPlans.size() - 1) :
                createDefaultPlan(familyId);

        com.integrityfamily.domain.Family family = evaluation.getFamily();
        String currentMilestone = family != null ? family.getCurrentMilestone() : "W1";
        String activeFase = resolveFaseFromMilestone(currentMilestone);

        List<PlanTask> newTasks = new ArrayList<>();

        boolean aiSuccess = false;
        try {
            log.info("[PLAN-TASK] Solicitando propuesta de misiones de diagnóstico al motor de IA...");
            String aiJson = aiService.generateDiagnosticMissions(evaluation);
            if (aiJson != null && !aiJson.isBlank()) {
                // Limpiar envoltura markdown si existiera
                if (aiJson.contains("```json")) {
                    aiJson = aiJson.substring(aiJson.indexOf("```json") + 7);
                    if (aiJson.contains("```")) {
                        aiJson = aiJson.substring(0, aiJson.indexOf("```"));
                      }
                }
                aiJson = aiJson.trim();

                List<DiagnosticMissionProposal> proposals = objectMapper.readValue(
                        aiJson, 
                        new TypeReference<List<DiagnosticMissionProposal>>() {}
                );

                for (DiagnosticMissionProposal prop : proposals) {
                    PlanTask task = PlanTask.builder()
                            .title(prop.title() != null ? prop.title() : "Misión de Conciencia")
                            .description(prop.description())
                            .completed(false)
                            .plan(plan)
                            .dimension(prop.dimension() != null ? prop.dimension().toUpperCase().trim() : "COMUNICACION")
                            .riesgoAsociado("Brecha en diagnóstico adaptativo")
                            .objetivo(prop.objective() != null ? prop.objective() : "Mejorar cohesión familiar")
                            .indicadorCumplimiento(prop.successMetric() != null ? prop.successMetric() : "Completar la microacción")
                            .responsible(evaluation.getMember())
                            .fase(activeFase)
                            .build();
                    newTasks.add(task);
                }
                aiSuccess = !newTasks.isEmpty();
            }
        } catch (Exception e) {
            log.warn("⚠️ [PLAN-TASK] Fallo en la generación de misiones de IA diagnóstica para evaluación ID {}: {}. Activando fallback de resiliencia estática...", 
                    evaluation.getId(), e.getMessage());
        }

        // Fallback resiliente: Si la IA falla, aplicamos las tareas estáticas para evitar bloqueos
        if (!aiSuccess) {
            log.info("[PLAN-TASK] Ejecutando misiones estáticas de contingencia para rol: {}", role);
            newTasks.clear();
            if (role != null) {
                switch (role.toUpperCase()) {
                    case "PADRE":
                        newTasks.add(PlanTask.builder()
                                .title("Escucha Activa Consciente")
                                .description("Dedica 15 minutos a escuchar a un miembro de la familia sin dar consejos ni juzgar.")
                                .completed(false)
                                .plan(plan)
                                .dimension("COMUNICACION")
                                .responsible(evaluation.getMember())
                                .fase(activeFase)
                                .build());
                        newTasks.add(PlanTask.builder()
                                .title("Liderazgo sin Pantallas")
                                .description("Propón un momento de convivencia (como la cena) donde todos dejen los dispositivos.")
                                .completed(false)
                                .plan(plan)
                                .dimension("TIEMPOS")
                                .responsible(evaluation.getMember())
                                .fase(activeFase)
                                .build());
                        break;
                    case "MADRE":
                        newTasks.add(PlanTask.builder()
                                .title("Distribución de Carga")
                                .description("Delega una tarea visible del hogar a otro miembro de la familia.")
                                .completed(false)
                                .plan(plan)
                                .dimension("HABITOS")
                                .responsible(evaluation.getMember())
                                .fase(activeFase)
                                .build());
                        newTasks.add(PlanTask.builder()
                                .title("Espacio de Autocuidado")
                                .description("Tómate 30 minutos exclusivos para ti para recargar energía.")
                                .completed(false)
                                .plan(plan)
                                .dimension("EMOCIONES")
                                .responsible(evaluation.getMember())
                                .fase(activeFase)
                                .build());
                        break;
                    case "ADOLESCENTE":
                        newTasks.add(PlanTask.builder()
                                .title("Expresión Segura")
                                .description("Escribe una entrada privada en tu bitácora sobre cómo te sientes hoy.")
                                .completed(false)
                                .plan(plan)
                                .dimension("EMOCIONES")
                                .responsible(evaluation.getMember())
                                .fase(activeFase)
                                .build());
                        newTasks.add(PlanTask.builder()
                                .title("Propuesta de Conexión")
                                .description("Sugiere una actividad que te guste para hacer juntos en familia.")
                                .completed(false)
                                .plan(plan)
                                .dimension("COMUNICACION")
                                .responsible(evaluation.getMember())
                                .fase(activeFase)
                                .build());
                        break;
                    case "NINO":
                    case "NIÑO":
                        newTasks.add(PlanTask.builder()
                                .title("Rutina Divertida")
                                .description("Completa tus hábitos de la mañana cantando o jugando.")
                                .completed(false)
                                .plan(plan)
                                .dimension("HABITOS")
                                .responsible(evaluation.getMember())
                                .fase(activeFase)
                                .build());
                        newTasks.add(PlanTask.builder()
                                .title("Juego Consciente")
                                .description("Pide a tus papás jugar a algo juntos por 20 minutos.")
                                .completed(false)
                                .plan(plan)
                                .dimension("TIEMPOS")
                                .responsible(evaluation.getMember())
                                .fase(activeFase)
                                .build());
                        break;
                    default:
                        newTasks.add(PlanTask.builder()
                                .title("Misión de Conexión")
                                .description("Realizar una actividad compartida de baja fricción.")
                                .completed(false)
                                .plan(plan)
                                .dimension("COMUNICACION")
                                .responsible(evaluation.getMember())
                                .fase(activeFase)
                                .build());
                        break;
                }
            }
        }

        plan.getTasks().addAll(newTasks);
        planRepository.save(plan);
        log.info("✅ [PLAN-TASK] Sincronización finalizada. {} misiones persistidas para la evaluación ID {}.", newTasks.size(), evaluation.getId());
    }

    private String resolveFaseFromMilestone(String code) {
        if (code == null) return "RECONOCIMIENTO";
        return switch (code.toUpperCase().trim()) {
            case "W1", "M1", "M2", "M3" -> "RECONOCIMIENTO";
            case "M4", "M5", "M6", "M9", "M12" -> "AMOR";
            case "M15", "M18", "M21", "M24", "M36" -> "ENTREGA";
            default -> "RECONOCIMIENTO";
        };
    }

    private ImprovementPlan createDefaultPlan(Long familyId) {
        log.info("🆕 [PLAN-TASK] Generando ImprovementPlan base.");
        return ImprovementPlan.builder()
                .family(familyRepository.getReferenceById(familyId))
                .title("Estrategia de Integridad Familiar")
                .description("Generado automáticamente por el motor Sentinel.")
                .build();
    }
}
