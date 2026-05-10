package com.integrityfamily.plan.service;

import com.integrityfamily.plan.service.PlanGenerationService.HybridPlanDto;
import com.integrityfamily.plan.service.PlanGenerationService.MilestoneDto;
import com.integrityfamily.plan.service.PlanGenerationService.TaskDto;
import com.integrityfamily.plan.service.PlanGenerationService.StepDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * SDD SPEC 6.7: PlanValidator.
 * Se encarga de validar que la salida JSON estructurada de la Inteligencia Artificial
 * cumpla estrictamente con las reglas de negocio de la plataforma Integrity Family v5.0.
 * Si detecta inconsistencias menores, las repara con valores por defecto válidos para
 * evitar la interrupción del flujo del sistema.
 */
@Component
@Slf4j
public class PlanValidator {

    private static final Set<String> VALID_MILESTONES = Set.of(
            "W1", "M1", "M2", "M3", "M4", "M5", "M6", "M9", "M12", "M15", "M18", "M21", "M24", "M36"
    );

    private static final Set<String> VALID_DIMENSIONS = Set.of(
            "EMOCIONES", "COMUNICACION", "HABITOS", "TIEMPOS"
    );

    private static final Set<String> VALID_PHASES = Set.of(
            "RECONOCIMIENTO", "AMOR", "ENTREGA"
    );

    /**
     * Valida y sanea el plan híbrido de la IA.
     * Retorna un HybridPlanDto saneado listo para persistir.
     */
    public HybridPlanDto validateAndSanitize(HybridPlanDto originalPlan) {
        log.info("🛡️ [PLAN-VALIDATOR] Iniciando validación de consistencia del plan híbrido...");

        if (originalPlan == null) {
            throw new IllegalArgumentException("El plan a validar no puede ser nulo.");
        }

        String vision = originalPlan.vision3y();
        if (vision == null || vision.isBlank()) {
            vision = "Acompañamiento longitudinal sistémico orientado a fortalecer el amor, el respeto y la comunicación en el hogar.";
            log.warn("⚠️ [PLAN-VALIDATOR] Visión vacía. Se aplicó visión general de respaldo.");
        }

        List<MilestoneDto> sanitizedMilestones = new ArrayList<>();

        if (originalPlan.milestones() != null) {
            for (MilestoneDto mDto : originalPlan.milestones()) {
                String code = mDto.code() != null ? mDto.code().toUpperCase().trim() : "M1";
                
                // Corregir códigos de hitos no válidos mapeándolos al hito más cercano
                if (!VALID_MILESTONES.contains(code)) {
                    log.warn("⚠️ [PLAN-VALIDATOR] Código de hito inválido detectado: '{}'. Forzando 'M1'.", code);
                    code = "M1";
                }

                String objective = mDto.objective();
                if (objective == null || objective.isBlank()) {
                    objective = "Objetivo terapéutico enfocado en el desarrollo e integración familiar.";
                }

                List<TaskDto> sanitizedTasks = new ArrayList<>();
                if (mDto.tasks() != null) {
                    for (TaskDto tDto : mDto.tasks()) {
                        String title = tDto.title();
                        if (title == null || title.isBlank()) {
                            continue; // Omitir tareas vacías
                        }

                        // Validar Dimensión
                        String dimension = tDto.dimension() != null ? tDto.dimension().toUpperCase().trim() : "EMOCIONES";
                        if (!VALID_DIMENSIONS.contains(dimension)) {
                            log.warn("⚠️ [PLAN-VALIDATOR] Dimensión inválida: '{}'. Reemplazando por 'EMOCIONES'.", dimension);
                            dimension = "EMOCIONES";
                        }

                        // Validar Fase
                        String fase = tDto.fase() != null ? tDto.fase().toUpperCase().trim() : "RECONOCIMIENTO";
                        if (!VALID_PHASES.contains(fase)) {
                            // Asignación automática inteligente basada en el hito
                            fase = resolvePhaseByMilestone(code);
                            log.warn("⚠️ [PLAN-VALIDATOR] Fase inválida para hito '{}'. Asignando phase: '{}'.", code, fase);
                        }

                        // Validar campos de transformación del ICF
                        String riesgo = tDto.riesgoAsociado() != null && !tDto.riesgoAsociado().isBlank() ? tDto.riesgoAsociado() : "Cohesión Familiar";
                        String obj = tDto.objetivo() != null && !tDto.objetivo().isBlank() ? tDto.objetivo() : "Desarrollar la inteligencia emocional cooperativa.";
                        String accion = tDto.accionConcreta() != null && !tDto.accionConcreta().isBlank() ? tDto.accionConcreta() : title;
                        String indicador = tDto.indicadorCumplimiento() != null && !tDto.indicadorCumplimiento().isBlank() ? tDto.indicadorCumplimiento() : "Registro diario de la actividad por la familia.";
                        String evidencia = tDto.evidenciaRequerida() != null && !tDto.evidenciaRequerida().isBlank() ? tDto.evidenciaRequerida() : "Fotografía o anotación en la Bitácora.";
                        Integer impacto = tDto.impactoIcf() != null ? tDto.impactoIcf() : 5;

                        // Validar y corregir pasos (Debe ser bucle cerrado: Planificar, Ejecutar, Evaluar)
                        List<StepDto> sanitizedSteps = sanitizeSteps(tDto.steps(), title);

                        sanitizedTasks.add(new TaskDto(
                                title,
                                dimension,
                                fase,
                                riesgo,
                                obj,
                                accion,
                                indicador,
                                evidencia,
                                impacto,
                                sanitizedSteps
                        ));
                    }
                }

                sanitizedMilestones.add(new MilestoneDto(code, objective, sanitizedTasks));
            }
        }

        log.info("🛡️ [PLAN-VALIDATOR] Validación completada con éxito. Plan completamente saneado.");
        return new HybridPlanDto(vision, sanitizedMilestones);
    }

    private String resolvePhaseByMilestone(String code) {
        return switch (code) {
            case "W1", "M1", "M2", "M3" -> "RECONOCIMIENTO";
            case "M4", "M5", "M6", "M9", "M12" -> "AMOR";
            default -> "ENTREGA";
        };
    }

    private List<StepDto> sanitizeSteps(List<StepDto> steps, String taskTitle) {
        if (steps == null || steps.size() < 3) {
            log.warn("⚠️ [PLAN-VALIDATOR] Pasos incompletos o nulos para la tarea '{}'. Generando ciclo cerrado estándar.", taskTitle);
            return List.of(
                    new StepDto("PLANIFICAR", "Definir el momento ideal del día en que la familia se reunirá para realizar la acción: " + taskTitle),
                    new StepDto("EJECUTAR", "Llevar a cabo la microacción familiar de forma unida, empática y sin pantallas."),
                    new StepDto("EVALUAR", "Conversar brevemente durante 2 minutos sobre cómo se sintieron compartiendo este momento.")
            );
        }

        // Si existen los pasos, asegurar que tengan tipos válidos
        List<StepDto> result = new ArrayList<>();
        String[] defaultTypes = {"PLANIFICAR", "EJECUTAR", "EVALUAR"};
        for (int i = 0; i < 3; i++) {
            StepDto s = steps.size() > i ? steps.get(i) : null;
            String type = defaultTypes[i];
            String detail = s != null && s.detail() != null ? s.detail() : "Realizar el paso correspondiente de " + type;
            result.add(new StepDto(type, detail));
        }
        return result;
    }
}
