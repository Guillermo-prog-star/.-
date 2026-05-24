package com.integrityfamily.plan.service;

import com.integrityfamily.domain.*;
import com.integrityfamily.plan.dto.PlanDtos.*;
import com.integrityfamily.domain.repository.*;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SDD: Servicio de Planificación Harmonizado.
 * Gestiona el ciclo de vida de los planes de transformación familiar.
 * Integra un Motor Determinístico de Generación de Planes (Sin IA) y el motor Sentinel Auto-Evidence.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlanService {

    private final ImprovementPlanRepository planRepository;
    private final PlanTaskRepository planTaskRepository;
    private final FamilyLogbookEntryRepository logbookEntryRepository;
    private final EvaluationRepository evaluationRepository;
    private final PlanTemplateRepository planTemplateRepository;
    private final PlanTemplateActivityRepository planTemplateActivityRepository;
    private final MilestoneRepository milestoneRepository;
    private final QuestionRepository questionRepository;
    private final org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    @Transactional(readOnly = true)
    public List<PlanResponse> findAllPlans() {
        return planRepository.findAll().stream().map(this::toPlanResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> findByFamilyId(Long familyId) {
        return planRepository.findByFamilyId(familyId).stream().map(this::toPlanResponse).toList();
    }

    @Transactional(readOnly = true)
    public PlanResponse findPlanById(Long id) {
        ImprovementPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan no encontrado: " + id));
        return toPlanResponse(plan);
    }

    @Transactional
    public PlanResponse createPlan(ImprovementPlan plan) {
        return toPlanResponse(planRepository.save(plan));
    }

    @Transactional
    public PlanResponse updatePlan(Long id, ImprovementPlan request) {
        ImprovementPlan existing = planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan no encontrado: " + id));
        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        return toPlanResponse(planRepository.save(existing));
    }

    @Transactional
    public void deletePlan(Long id) {
        planRepository.deleteById(id);
    }

    // --- Motor Determinístico (Sprint 3) ---

    @Transactional
    public PlanResponse generateDeterministicPlan(Long evaluationId) {
        log.info("🎯 [PLAN DETERMINÍSTICO] Iniciando ensamblado dinámico para Evaluation ID: {}", evaluationId);
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new RuntimeException("Evaluación no encontrada: " + evaluationId));

        List<Question> evaluationQuestions = new ArrayList<>();
        if (evaluation.getAnswers() != null) {
            for (EvaluationAnswer answer : evaluation.getAnswers()) {
                questionRepository.findByQuestionKey(answer.getQuestionKey())
                        .ifPresent(evaluationQuestions::add);
            }
        }

        Family family = evaluation.getFamily();
        if (family == null) {
            throw new RuntimeException("La evaluación no tiene una familia asociada.");
        }

        String criticalDimension = evaluation.getCriticalDimension();
        if (criticalDimension == null || criticalDimension.isBlank()) {
            criticalDimension = "COMUNICACION";
        }

        String riskLevel = evaluation.getRiskLevel();
        if (riskLevel == null || riskLevel.isBlank()) {
            riskLevel = "MODERATE";
        }

        log.info("📊 Diagnóstico detectado -> Dimensión Crítica: {}, Nivel de Riesgo: {}", criticalDimension, riskLevel);

        // Buscar plantilla maestra
        List<PlanTemplate> templates = planTemplateRepository.findByDimensionAndRiskLevel(criticalDimension, riskLevel);
        if (templates.isEmpty()) {
            templates = planTemplateRepository.findByDimension(criticalDimension);
        }

        PlanTemplate template;
        if (templates.isEmpty()) {
            // Generar plantilla de respaldo de forma determinística
            template = PlanTemplate.builder()
                    .code("TMPL-" + criticalDimension.toUpperCase() + "-" + riskLevel)
                    .name("Plan de Transformación en " + criticalDimension)
                    .dimension(criticalDimension)
                    .riskLevel(riskLevel)
                    .build();
            template = planTemplateRepository.save(template);
            log.info("📦 Plantilla de respaldo generada automáticamente: {}", template.getCode());
        } else {
            template = templates.get(0);
            log.info("📦 Plantilla maestra seleccionada: {} - {}", template.getCode(), template.getName());
        }

        // De-duplication: ensure only one active plan exists per family
        List<ImprovementPlan> existingPlans = planRepository.findByFamilyId(family.getId());
        if (existingPlans != null && !existingPlans.isEmpty()) {
            log.info("🧹 [PLAN-SERVICE] Eliminando {} planes existentes para la familia ID: {}", existingPlans.size(), family.getId());
            planRepository.deleteAll(existingPlans);
            planRepository.flush();
        }

        // Crear el ImprovementPlan
        ImprovementPlan plan = ImprovementPlan.builder()
                .family(family)
                .evaluation(evaluation)
                .title("Plan de Transformación: " + template.getName())
                .description(String.format("Intervención clínica ensamblada determinísticamente para el nivel de riesgo %s en la dimensión %s.", riskLevel, criticalDimension))
                .vision3y("Consolidar un hogar íntegro, consciente y con comunicación plena en un horizonte longitudinal de 36 meses.")
                .aiReport("Generado por Motor de Reglas Determinístico v3.0 (Sin IA).")
                .aiGeneratedAt(LocalDateTime.now())
                .tasks(new ArrayList<>())
                .build();
        plan = planRepository.save(plan);

        // Buscar actividades de la plantilla
        List<PlanTemplateActivity> activities = planTemplateActivityRepository.findByTemplateCode(template.getCode());
        if (activities.isEmpty()) {
            // Sembrar actividades clínicas estándar por fase para asegurar suficiencia e incrementalidad
            activities = List.of(
                PlanTemplateActivity.builder()
                    .templateCode(template.getCode())
                    .title("Mesa de diálogo sin dispositivos electrónicos")
                    .frequency("DAILY")
                    .durationDays(7)
                    .phase("1 semana")
                    .build(),
                PlanTemplateActivity.builder()
                    .templateCode(template.getCode())
                    .title("Asamblea Familiar de Reconocimiento y Gratitud")
                    .frequency("3_PER_WEEK")
                    .durationDays(30)
                    .phase("1 mes")
                    .build(),
                PlanTemplateActivity.builder()
                    .templateCode(template.getCode())
                    .title("Revisión de Acuerdos y Convivencia")
                    .frequency("WEEKLY")
                    .durationDays(90)
                    .phase("3 meses")
                    .build(),
                PlanTemplateActivity.builder()
                    .templateCode(template.getCode())
                    .title("Hito Longitudinal de Celebración de Integridad")
                    .frequency("MONTHLY")
                    .durationDays(180)
                    .phase("6 meses")
                    .build()
            );
            activities = planTemplateActivityRepository.saveAll(activities);
            log.info("🌱 Actividades clínicas de plantilla sembradas automáticamente.");
        }

        // Ensamblar tareas (Misiones)
        for (PlanTemplateActivity activity : activities) {
            int impacto = switch(activity.getPhase()) {
                case "1 semana" -> 15;
                case "1 mes" -> 25;
                case "3 meses" -> 30;
                default -> 50;
            };

            String objetivo = switch(activity.getPhase()) {
                case "1 semana" -> "Establecer presencia plena y escucha activa durante la convivencia diaria.";
                case "1 mes" -> "Fomentar el aprecio mutuo y la validación emocional entre los miembros.";
                case "3 meses" -> "Ajustar rutinas familiares y resolver tensiones acumuladas de forma constructiva.";
                default -> "Consolidar el sentido de pertenencia y amor incondicional en el hogar.";
            };

            String accion = switch(activity.getPhase()) {
                case "1 semana" -> "Apagar televisores y dejar celulares en una canasta antes de compartir momentos clave.";
                case "1 mes" -> "Cada miembro expresa una gratitud o reconocimiento genuino hacia otro miembro de la familia.";
                case "3 meses" -> "Dedicación de 45 minutos semanales para revisar el tablero de tareas y acuerdos.";
                default -> "Salida familiar especial o actividad conmemorativa de los logros alcanzados.";
            };

            String evidencia = switch(activity.getPhase()) {
                case "1 semana" -> "Fotografía de la canasta con celulares o nota de compromiso en bitácora.";
                case "1 mes" -> "Registro en el muro de gratitud o entrada reflexiva en la bitácora familiar.";
                case "3 meses" -> "Acta breve o check de confirmación en la bitácora de convivencia.";
                default -> "Reflexión compartida de la evolución familiar en el sistema.";
            };

            String pilarFase = switch (activity.getPhase()) {
                 case "1 semana", "1 mes", "3 meses" -> "RECONOCIMIENTO";
                 case "6 meses" -> "AMOR";
                 default -> "ENTREGA";
            };

            PlanTask task = PlanTask.builder()
                    .plan(plan)
                    .title(activity.getTitle())
                    .description("Misión clínica asignada para la fase: " + pilarFase)
                    .dimension(template.getDimension())
                    .dueDate(LocalDateTime.now().plusDays(activity.getDurationDays()))
                    .fase(pilarFase)
                    .riesgoAsociado(riskLevel)
                    .objetivo(objetivo)
                    .accionConcreta(accion)
                    .indicadorCumplimiento("Cumplimiento constante en el periodo asignado según frecuencia: " + activity.getFrequency())
                    .evidenciaRequerida(evidencia)
                    .impactoIcf(impacto)
                    .completed(false)
                    .steps(new ArrayList<>())
                    .build();
            populateTaxonomyV2(task, activity.getPhase(), null, evaluation, evaluationQuestions, null, null, null, null, null);
            task = planTaskRepository.save(task);
            plan.getTasks().add(task);
        }

        log.info("✅ Plan ensamblado con éxito con {} misiones/tareas clínicas.", plan.getTasks().size());
        return toPlanResponse(plan);
    }

    /**
     * [PLAN ADAPTATIVO] Creando plan desde respuesta IA (Rediseño 6.4)
     */
    @Transactional
    public PlanResponse createPlanFromAiResponse(Long evaluationId, IaPlanResponse aiResponse) {
        log.info("🎯 [PLAN ADAPTATIVO] Creando plan desde respuesta IA para Evaluation ID: {}", evaluationId);
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new RuntimeException("Evaluación no encontrada: " + evaluationId));

        List<Question> evaluationQuestions = new ArrayList<>();
        if (evaluation.getAnswers() != null) {
            for (EvaluationAnswer answer : evaluation.getAnswers()) {
                questionRepository.findByQuestionKey(answer.getQuestionKey())
                        .ifPresent(evaluationQuestions::add);
            }
        }

        Family family = evaluation.getFamily();
        if (family == null) {
            throw new RuntimeException("La evaluación no tiene una familia asociada.");
        }

        // De-duplication: ensure only one active plan exists per family
        List<ImprovementPlan> existingPlans = planRepository.findByFamilyId(family.getId());
        if (existingPlans != null && !existingPlans.isEmpty()) {
            log.info("🧹 [PLAN-SERVICE] Eliminando {} planes existentes para la familia ID: {}", existingPlans.size(), family.getId());
            planRepository.deleteAll(existingPlans);
            planRepository.flush();
        }

        ImprovementPlan plan = ImprovementPlan.builder()
                .family(family)
                .evaluation(evaluation)
                .title("Plan de Transformación Adaptativo")
                .description("Intervención clínica generada por IA y validada por el sistema.")
                .vision3y(aiResponse.vision_3y())
                .aiReport("Generado por IA (Contrato Estructurado).")
                .aiGeneratedAt(LocalDateTime.now())
                .tasks(new ArrayList<>())
                .build();
        plan = planRepository.save(plan);

        for (IaMilestone iaMilestone : aiResponse.milestones()) {
            Milestone milestone = milestoneRepository.findByCode(iaMilestone.code())
                    .orElseThrow(() -> new RuntimeException("Hito no encontrado: " + iaMilestone.code()));

            for (IaTask iaTask : iaMilestone.tasks()) {
                PlanTask task = PlanTask.builder()
                        .plan(plan)
                        .title(iaTask.title())
                        .description(iaMilestone.objective())
                        .dimension(iaTask.dimension())
                        .dueDate(LocalDateTime.now().plusDays(milestone.getDurationDays() != null ? milestone.getDurationDays() : 30))
                        .milestone(milestone)
                        .completed(false)
                        .build();
                
                populateTaxonomyV2(task, null, milestone, evaluation, evaluationQuestions, iaTask.pillarName(), iaTask.milestoneCode(), iaTask.memberType(), iaTask.riskType(), iaTask.missionGenerator());
                task = planTaskRepository.save(task);

                List<PlanTaskStep> steps = new ArrayList<>();
                if (iaTask.steps() != null) {
                    for (IaStep iaStep : iaTask.steps()) {
                        PlanTaskStep step = PlanTaskStep.builder()
                                .task(task)
                                .type(StepType.valueOf(iaStep.type().toUpperCase()))
                                .detail(iaStep.detail())
                                .completed(false)
                                .build();
                        steps.add(step);
                    }
                }
                
                task.setSteps(steps);
                planTaskRepository.save(task);
                
                plan.getTasks().add(task);
            }
        }

        log.info("✅ Plan adaptativo creado con éxito con {} tareas.", plan.getTasks().size());
        return toPlanResponse(plan);
    }

    // --- Tareas ---

    @Transactional(readOnly = true)
    public List<PlanTaskResponse> findAllTasks() {
        return planTaskRepository.findAll().stream().map(this::toTaskResponse).toList();
    }

    @Transactional(readOnly = true)
    public PlanTaskResponse findTaskById(Long id) {
        PlanTask task = planTaskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada: " + id));
        return toTaskResponse(task);
    }

    @Transactional
    public PlanTaskResponse createTask(PlanTask task) {
        return toTaskResponse(planTaskRepository.save(task));
    }

    @Transactional
    public PlanTaskResponse updateTask(Long id, PlanTask request) {
        PlanTask existing = planTaskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada: " + id));
        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        existing.setCompleted(request.isCompleted());
        return toTaskResponse(planTaskRepository.save(existing));
    }

    @Transactional
    public PlanTaskResponse completeTask(Long id, boolean completed) {
        PlanTask task = planTaskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada: " + id));
        task.setCompleted(completed);
        
        PlanTask savedTask = planTaskRepository.save(task);

        // [SDD SPEC: Sentinel Auto-Evidence Engine]
        if (completed && task.getPlan() != null && task.getPlan().getFamily() != null) {
            try {
                com.integrityfamily.domain.Family family = task.getPlan().getFamily();
                
                String situation = "Ejecución exitosa de la Misión Clínica: " + task.getTitle();
                String difficultyDetected = task.getRiesgoAsociado() != null && !task.getRiesgoAsociado().isBlank() ? 
                        task.getRiesgoAsociado() : "Prevención de riesgos en la dimensión " + (task.getDimension() != null ? task.getDimension() : "INTEGRIDAD");
                String emotionIdentified = task.getDimension() != null && !task.getDimension().isBlank() ? 
                        task.getDimension() : "EMOCIONES";
                String understanding = task.getObjetivo() != null && !task.getObjetivo().isBlank() ? 
                        task.getObjetivo() : "La familia comprende la importancia de consolidar este hábito de cambio.";
                String correctionAction = task.getAccionConcreta() != null && !task.getAccionConcreta().isBlank() ? 
                        task.getAccionConcreta() : "Realización de las actividades pautadas en el plan.";
                String familyAgreement = task.getIndicadorCumplimiento() != null && !task.getIndicadorCumplimiento().isBlank() ? 
                        task.getIndicadorCumplimiento() : "Sostener el hábito en las prácticas cotidianas.";
                
                String evidenceDesc = String.format(
                        "[EVIDENCIA AUTOMÁTICA SENTINEL v4.5]: Microacción completada con éxito. Requisito de evidencia cumplido: '%s'. Impacto de +%d puntos sumados al ICF.",
                        task.getEvidenciaRequerida() != null && !task.getEvidenciaRequerida().isBlank() ? task.getEvidenciaRequerida() : "Registro clínico de plan de acción",
                        task.getImpactoIcf() != null ? task.getImpactoIcf() : 10
                );

                FamilyLogbookEntry evidenceEntry = FamilyLogbookEntry.builder()
                        .family(family)
                        .situation(situation)
                        .difficultyDetected(difficultyDetected)
                        .emotionIdentified(emotionIdentified)
                        .understanding(understanding)
                        .correctionAction(correctionAction)
                        .familyAgreement(familyAgreement)
                        .progressEvidence(evidenceDesc)
                        .status(LogbookStatus.RESOLVED)
                        .createdBy("SENTINEL_AUTO_AI")
                        .resolvedBy("SENTINEL_AUTO_AI")
                        .createdAt(LocalDateTime.now())
                        .resolvedAt(LocalDateTime.now())
                        .build();

                logbookEntryRepository.save(evidenceEntry);
                log.info("🎯 [AUTO-EVIDENCE] Entrada de bitácora registrada automáticamente para la tarea ID: {}", id);
            } catch (Exception e) {
                log.error("❌ Error en generación automática de evidencia: {}", e.getMessage());
            }

            // Publicar evento para WebSockets / Desacoplamiento
            Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("taskId", savedTask.getId());
            payload.put("completed", savedTask.isCompleted());
            payload.put("familyId", task.getPlan().getFamily().getId());
            payload.put("title", savedTask.getTitle());

            com.integrityfamily.common.event.SystemEvent eventObj = 
                com.integrityfamily.common.event.SystemEvent.of(
                    "task.completed", 
                    task.getPlan().getFamily().getId(), 
                    payload, 
                    "SYSTEM"
                );

            try {
                rabbitTemplate.convertAndSend(com.integrityfamily.common.config.RabbitConfig.EXCHANGE_NAME, "task.completed", eventObj);
                log.info("📧 [PLAN] Evento 'task.completed' enviado para familia: {}", task.getPlan().getFamily().getId());
            } catch (Exception e) {
                log.error("❌ [PLAN] Error al publicar evento 'task.completed' a RabbitMQ (resiliencia activada): {}", e.getMessage());
            }
        }

        return toTaskResponse(savedTask);
    }

    @Transactional
    public void deleteTask(Long id) {
        planTaskRepository.deleteById(id);
    }

    // --- Mappers SDD ---

    private PlanResponse toPlanResponse(ImprovementPlan plan) {
        if (plan == null) return null;
        
        List<PlanTaskResponse> tasks = plan.getTasks() == null ? List.of() :
                plan.getTasks().stream().map(this::toTaskResponse).toList();

        return PlanResponse.builder()
                .id(plan.getId())
                .familyId(plan.getFamily() != null ? plan.getFamily().getId() : null)
                .evaluationId(plan.getEvaluation() != null ? plan.getEvaluation().getId() : null)
                .title(plan.getTitle())
                .description(plan.getDescription())
                .vision3y(plan.getVision3y())
                .aiReport(plan.getAiReport())
                .aiGeneratedAt(plan.getAiGeneratedAt())
                .tasks(tasks)
                .build();
    }

    private PlanTaskResponse toTaskResponse(PlanTask task) {
        if (task == null) return null;

        List<PlanTaskStepResponse> steps = task.getSteps() == null ? List.of() :
                task.getSteps().stream().map(this::toStepResponse).toList();

        return PlanTaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .dimension(task.getDimension())
                .dueDate(task.getDueDate())
                .periodicityMonths(task.getPeriodicityMonths())
                .milestoneId(task.getMilestone() != null ? task.getMilestone().getId() : null)
                .milestoneCode(task.getMilestoneCode() != null ? task.getMilestoneCode() : (task.getMilestone() != null ? task.getMilestone().getCode() : null))
                .assignedMemberId(task.getResponsible() != null ? task.getResponsible().getId() : null)
                .assignedMemberName(task.getResponsible() != null ? task.getResponsible().getFullName() : null)
                .completed(task.isCompleted())
                .steps(steps)
                .fase(task.getFase())
                .riesgoAsociado(task.getRiesgoAsociado())
                .objetivo(task.getObjetivo())
                .accionConcreta(task.getAccionConcreta())
                .indicadorCumplimiento(task.getIndicadorCumplimiento())
                .evidenciaRequerida(task.getEvidenciaRequerida())
                .impactoIcf(task.getImpactoIcf())
                .pillarName(task.getPillarName())
                .memberType(task.getMemberType())
                .riskType(task.getRiskType())
                .missionGenerator(task.getMissionGenerator())
                .build();
    }

    private PlanTaskStepResponse toStepResponse(PlanTaskStep step) {
        if (step == null) return null;
        return PlanTaskStepResponse.builder()
                .id(step.getId())
                .type(step.getType() != null ? step.getType().name() : null)
                .detail(step.getDetail())
                .completed(step.isCompleted())
                .build();
    }

    @Transactional
    public void enrichTaskTaxonomy(PlanTask task, Evaluation evaluation, String aiPillar, String aiMilestone, String aiMember, String aiRisk, String aiGenerator) {
        List<Question> evaluationQuestions = new ArrayList<>();
        if (evaluation != null && evaluation.getAnswers() != null) {
            for (com.integrityfamily.domain.EvaluationAnswer answer : evaluation.getAnswers()) {
                questionRepository.findByQuestionKey(answer.getQuestionKey())
                        .ifPresent(evaluationQuestions::add);
            }
        }
        populateTaxonomyV2(task, null, task.getMilestone(), evaluation, evaluationQuestions, aiPillar, aiMilestone, aiMember, aiRisk, aiGenerator);
    }

    private void populateTaxonomyV2(PlanTask task, String phase, Milestone milestone, Evaluation evaluation, List<Question> evaluationQuestions, String aiPillar, String aiMilestone, String aiMember, String aiRisk, String aiGenerator) {
        // 0. Prioritize taxonomy values passed directly from AI
        boolean hasAiPillar = aiPillar != null && !aiPillar.isBlank();
        boolean hasAiMilestone = aiMilestone != null && !aiMilestone.isBlank();
        boolean hasAiMember = aiMember != null && !aiMember.isBlank();
        boolean hasAiRisk = aiRisk != null && !aiRisk.isBlank();
        boolean hasAiGenerator = aiGenerator != null && !aiGenerator.isBlank();

        if (hasAiPillar) task.setPillarName(aiPillar.trim().toLowerCase());
        if (hasAiMilestone) task.setMilestoneCode(aiMilestone.trim().toUpperCase());
        if (hasAiMember) task.setMemberType(aiMember.trim().toLowerCase());
        if (hasAiRisk) task.setRiskType(aiRisk.trim().toLowerCase());
        if (hasAiGenerator) task.setMissionGenerator(aiGenerator.trim().toUpperCase());

        // If any value was not provided by AI, look for a matching question or use the fallback
        if (!hasAiPillar || !hasAiMilestone || !hasAiMember || !hasAiRisk || !hasAiGenerator) {
            Question matchingQuestion = null;
            
            // 1. Try to find a question in the same dimension (case-insensitive)
            if (task.getDimension() != null) {
                String taskDim = task.getDimension().trim().toLowerCase();
                for (Question q : evaluationQuestions) {
                    if (q.getDimension() != null && q.getDimension().trim().toLowerCase().equals(taskDim)) {
                        matchingQuestion = q;
                        break;
                    }
                }
            }
            
            // 2. Fallback to any question from the evaluation answers
            if (matchingQuestion == null && !evaluationQuestions.isEmpty()) {
                matchingQuestion = evaluationQuestions.get(0);
            }
            
            if (matchingQuestion != null) {
                // Copy from matching question ONLY if not already populated by AI
                if (!hasAiPillar) task.setPillarName(matchingQuestion.getPillarName());
                if (!hasAiMilestone) task.setMilestoneCode(matchingQuestion.getMilestoneCode());
                if (!hasAiMember) task.setMemberType(matchingQuestion.getMemberType());
                if (!hasAiRisk) task.setRiskType(matchingQuestion.getRiskType());
                if (!hasAiGenerator) task.setMissionGenerator(matchingQuestion.getMissionGenerator());
            } else {
                // Default Fallback values derived from activity / evaluation context
                if (!hasAiPillar) {
                    String derivedPillar = "reconocimiento";
                    if (task.getFase() != null) {
                        derivedPillar = task.getFase().toLowerCase();
                    } else if (milestone != null && milestone.getCode() != null) {
                        String code = milestone.getCode().toUpperCase();
                        if (code.equals("W1") || code.equals("M1")) {
                            derivedPillar = "reconocimiento";
                        } else if (code.equals("M3") || code.equals("M6") || code.equals("M9")) {
                            derivedPillar = "amor";
                        } else {
                            derivedPillar = "entrega";
                        }
                    }
                    task.setPillarName(derivedPillar);
                }
                
                if (!hasAiMilestone) {
                    String derivedMilestone = "W1";
                    if (milestone != null && milestone.getCode() != null) {
                        derivedMilestone = milestone.getCode();
                    } else if (phase != null) {
                        derivedMilestone = switch (phase) {
                            case "1 semana" -> "W1";
                            case "1 mes" -> "M1";
                            case "3 meses" -> "M3";
                            case "6 meses" -> "M6";
                            case "9 meses" -> "M9";
                            case "12 meses" -> "M12";
                            default -> "W1";
                        };
                    }
                    task.setMilestoneCode(derivedMilestone);
                }

                if (!hasAiMember) {
                    task.setMemberType("familia");
                }
                
                if (!hasAiRisk) {
                    String derivedRisk = "desconexion_emocional";
                    if (evaluation != null && evaluation.getCriticalDimension() != null) {
                        String dim = evaluation.getCriticalDimension().toLowerCase();
                        if (dim.contains("comunicacion")) {
                            derivedRisk = "comunicacion_defensiva";
                        } else if (dim.contains("emociones")) {
                            derivedRisk = "desregulacion_emocional";
                        } else if (dim.contains("habitos")) {
                            derivedRisk = "ausencia_rutinas";
                        }
                    }
                    task.setRiskType(derivedRisk);
                }
                
                if (!hasAiGenerator) {
                    String derivedGenerator = "CONEXION_FAMILIAR";
                    if (phase != null) {
                        derivedGenerator = switch (phase) {
                            case "1 semana" -> "ESTABILIZACION_EMOCIONAL";
                            case "1 mes" -> "CONCIENCIA_EMOCIONAL";
                            case "3 meses" -> "ACUERDOS_CONVIVENCIA";
                            default -> "CONEXION_FAMILIAR";
                        };
                    } else if (milestone != null && milestone.getCode() != null) {
                        String code = milestone.getCode().toUpperCase();
                        derivedGenerator = switch (code) {
                            case "W1" -> "ESTABILIZACION_EMOCIONAL";
                            case "M1" -> "CONCIENCIA_EMOCIONAL";
                            case "M3" -> "ACUERDOS_CONVIVENCIA";
                            case "M6" -> "CONEXION_FAMILIAR";
                            default -> "LEGADO_CONSCIENTE";
                        };
                    }
                    task.setMissionGenerator(derivedGenerator);
                }
            }
        }
    }
}
