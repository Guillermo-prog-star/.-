package com.integrityfamily.evaluation.service;

import com.integrityfamily.ai.service.AiService;
import com.integrityfamily.domain.*;
import com.integrityfamily.dto.EvaluationDtos;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.repository.EvaluationSummary;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.MemberRepository;
import com.integrityfamily.domain.repository.QuestionRepository;
import com.integrityfamily.assessment.service.AssessmentAnswerService;
import com.integrityfamily.domain.repository.EvaluationAnswerRepository;
import com.integrityfamily.risk.service.RiskAlgoV1Engine;
import com.integrityfamily.risk.service.RiskService;
import com.integrityfamily.milestone.service.MilestoneService;
import com.integrityfamily.plan.service.PlanTaskService;
import com.integrityfamily.plan.service.PlanGenerationService;
import com.integrityfamily.analytics.service.FamilyProgressAnalyticsService;
import com.integrityfamily.cognitive.service.FamilyMemoryService;
import com.integrityfamily.cognitive.service.FamilySkillEngine;
import com.integrityfamily.cognitive.service.FamilyReflectionService;
import com.integrityfamily.cognitive.service.NarrativeEvolutionEngine;
import com.integrityfamily.cognitive.service.FamilyIdentityGraphService;
import com.integrityfamily.scanner.domain.InferenceRecord;
import com.integrityfamily.scanner.domain.RuleActivation;
import com.integrityfamily.scanner.repository.InferenceRecordRepository;
import com.integrityfamily.scanner.service.AlertEngine;
import com.integrityfamily.scanner.service.DeterministicExplanationPipeline;
import com.integrityfamily.scanner.service.InferenceRecordService;
import com.integrityfamily.scanner.service.RuleExecutionEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SDD: Motor de Evaluación.
 * Implementación rigurosa del Algoritmo Oficial de Riesgo Familiar RISK_ALGO_V1.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationRepository       evaluationRepository;
    private final EvaluationAnswerRepository  evaluationAnswerRepository;
    private final FamilyRepository            familyRepository;
    private final MemberRepository            memberRepository;
    private final QuestionRepository          questionRepository;
    private final AssessmentAnswerService     assessmentAnswerService;
    private final RiskAlgoV1Engine            riskAlgoV1Engine;
    private final RiskService riskService;
    private final RabbitTemplate rabbitTemplate;
    private final MilestoneService milestoneService;
    private final AiService aiService;
    private final PlanTaskService planTaskService;
    private final PlanGenerationService planGenerationService;
    private final FamilyProgressAnalyticsService familyProgressAnalyticsService;
    private final FamilyMemoryService familyMemoryService;
    private final FamilySkillEngine familySkillEngine;
    private final FamilyReflectionService familyReflectionService;
    private final NarrativeEvolutionEngine narrativeEvolutionEngine;
    private final FamilyIdentityGraphService familyIdentityGraphService;
    private final DeterministicExplanationPipeline explanationPipeline;
    private final InferenceRecordService inferenceRecordService;
    private final InferenceRecordRepository inferenceRecordRepository;
    private final RuleExecutionEngine ruleExecutionEngine;
    private final AlertEngine alertEngine;

    public List<Evaluation> findAll() {
        return evaluationRepository.findAll();
    }

    public List<Evaluation> findByFamilyId(Long familyId) {
        return evaluationRepository.findByFamilyId(familyId);
    }

    public List<EvaluationSummary> findSummaryByFamilyId(Long familyId) {
        return evaluationRepository.findSummaryByFamilyId(familyId);
    }

    public Evaluation findById(Long id) {
        return evaluationRepository.findById(id).orElseThrow();
    }

    @Transactional
    public Evaluation create(Evaluation e) {
        return evaluationRepository.save(e);
    }

    @Transactional
    public Evaluation start(EvaluationDtos.EvaluationStartRequest req) {
        Family family = familyRepository.findById(req.familyId())
                .orElseThrow(() -> new RuntimeException("Familia no encontrada"));

        Evaluation evaluation = new Evaluation();
        evaluation.setFamily(family);
        evaluation.setStatus(EvaluationStatus.STARTED);
        evaluation.setStartedAt(LocalDateTime.now());
        evaluation.setAlgorithmVersion("RISK_ALGO_V1");

        if (req.memberId() != null) {
            FamilyMember member = memberRepository.findById(req.memberId()).orElse(null);
            evaluation.setMember(member);
        }

        return evaluationRepository.save(evaluation);
    }

    /**
     * Finaliza un diagnóstico ejecutando el algoritmo oficial RISK_ALGO_V1.
     *
     * Flujo mobile-first: si el request llega sin respuestas (el usuario las fue
     * guardando una a una con {@code POST /{evalId}/answers}), las carga desde BD.
     * Flujo clásico: si el request trae respuestas, las borra primero (evita
     * duplicados por unique constraint) y las re-persiste frescas.
     *
     * @return FinalizeResult con la evaluación persistida y el resultado detallado del engine
     */
    @Transactional
    public EvaluationDtos.FinalizeResult finalize(Long id, EvaluationDtos.EvaluationFinalizeRequest request) {
        log.info("🏁 [EVALUATION-ALGO] Finalizando diagnóstico ID: {} bajo RISK_ALGO_V1", id);
        Evaluation existing = findById(id);

        existing.setStatus(EvaluationStatus.FINALIZED);
        existing.setFinalizedAt(LocalDateTime.now());
        existing.setAlgorithmVersion("RISK_ALGO_V1");

        // ── Determinar fuente de respuestas ──────────────────────────────────
        boolean hasRequestAnswers = request.answers() != null && !request.answers().isEmpty();

        List<EvaluationDtos.AnswerDto> effectiveAnswers;
        if (hasRequestAnswers) {
            // Flujo clásico (desktop/API directa): las respuestas llegan en el body.
            // Borramos las incrementales previas para evitar violar el UNIQUE constraint.
            long prevCount = evaluationAnswerRepository.countByEvaluationId(id);
            if (prevCount > 0) {
                evaluationAnswerRepository.deleteByEvaluationId(id);
                existing.getAnswers().clear();
                log.info("[EVALUATION-ALGO] {} respuestas incrementales previas reemplazadas por el body del request.", prevCount);
            }
            effectiveAnswers = request.answers();

            // Persistir las respuestas del request — batch load para evitar N+1
            List<Long> questionIds = effectiveAnswers.stream()
                    .map(EvaluationDtos.AnswerDto::questionId)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .toList();
            Map<Long, Question> questionMap = questionRepository.findAllById(questionIds).stream()
                    .collect(java.util.stream.Collectors.toMap(Question::getId, q -> q));

            for (EvaluationDtos.AnswerDto a : effectiveAnswers) {
                if (a.questionId() == null) continue;
                Question q = questionMap.get(a.questionId());
                if (q == null) continue;
                EvaluationAnswer answer = new EvaluationAnswer();
                answer.setEvaluation(existing);
                answer.setQuestionKey(q.getQuestionKey() != null ? q.getQuestionKey() : "Q-" + q.getId());
                answer.setQuestionId(q.getId());
                answer.setScore(a.getEffectiveValue());
                try {
                    answer.setDimension(q.getDimension() != null
                            ? DimensionType.valueOf(q.getDimension().toUpperCase().trim())
                            : DimensionType.COMMITMENT);
                } catch (Exception ex) {
                    answer.setDimension(DimensionType.COMMITMENT);
                }
                existing.getAnswers().add(answer);
            }
        } else {
            // Flujo mobile-first: las respuestas ya están en BD, solo las cargamos para el engine.
            effectiveAnswers = assessmentAnswerService.loadAnswersAsDto(id);
            log.info("[EVALUATION-ALGO] Usando {} respuestas incrementales guardadas (body vacío).",
                    effectiveAnswers.size());
        }

        // ── Ejecutar RISK_ALGO_V1 (Taxonomía v2 — con severityWeight, MIRROR y recaída) ──
        String currentMilestone = existing.getFamily().getCurrentMilestone();
        RiskAlgoV1Engine.AlgoResult algo = riskAlgoV1Engine.compute(effectiveAnswers, currentMilestone);

        existing.setIcf(algo.healthyIndex());
        existing.setRiskLevel(algo.riskLevel());
        existing.setCriticalDimension(algo.criticalDimension());
        existing.setHasCrisis(algo.hasCrisis());
        existing.setMilestoneKey(currentMilestone);

        // Persistir scores por dimensión
        algo.dimensionScores().forEach((name, score) -> {
            EvaluationDimensionScore ds = new EvaluationDimensionScore();
            ds.setEvaluation(existing);
            ds.setDimensionName(name);
            ds.setScore(score);
            existing.getDimensionScores().add(ds);
        });

        // Loguear señales especiales
        if (algo.simulationSuspected()) {
            log.warn("[RISK_ALGO_V1] SIMULACION SOSPECHADA familia ID {} — MIRROR flags: {}",
                    existing.getFamily().getId(), algo.mirrorFlags());
        }
        if (algo.relapseDetected()) {
            log.warn("[RISK_ALGO_V1] RECAIDA DETECTADA familia ID {} — señales: {}",
                    existing.getFamily().getId(), algo.relapseFlags());
        }

        // IF-DEP: Síntesis determinística (reemplaza interpretación generativa)
        String memberRole = existing.getMember() != null ? existing.getMember().getRole() : null;
        existing.setSpiritualSynthesis(explanationPipeline.buildFamiliarNarrative(algo, memberRole));

        Evaluation saved = evaluationRepository.save(existing);
        log.info("[EVALUATION-ALGO] Evaluacion persistida. {} | {}", algo.summary(),
                explanationPipeline.buildTechnicalSummary(algo));

        // IF-CIS: Registro de inferencia epistemológicamente estable (ICF_CALC base)
        try {
            inferenceRecordService.createFromEvaluation(saved, algo);
        } catch (Exception e) {
            log.error("⚠️ [IF-CIS] Error al crear InferenceRecord (no bloqueante): {}", e.getMessage());
        }

        // IF-REE: Ejecutar reglas EEDSL y registrar activaciones como InferenceRecords adicionales
        List<RuleActivation> activations = List.of();
        try {
            activations = ruleExecutionEngine.evaluateRules(algo, saved, memberRole);
            for (RuleActivation activation : activations) {
                try {
                    inferenceRecordService.createFromRule(saved, algo, activation);
                } catch (Exception ex) {
                    log.warn("⚠️ [IF-REE] Error persistiendo activación de regla {} (no bloqueante): {}",
                            activation.ruleKey(), ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("⚠️ [IF-REE] Error en motor de reglas EEDSL (no bloqueante): {}", e.getMessage());
        }

        // IF-ALT: Detectar patrones clínicos críticos y generar alertas
        try {
            alertEngine.evaluate(saved, algo, activations);
        } catch (Exception e) {
            log.error("⚠️ [IF-ALT] Error en motor de alertas (no bloqueante): {}", e.getMessage());
        }

        processPostFinalization(saved, algo);

        // Construir respuesta enriquecida con todos los campos del engine
        EvaluationDtos.EvaluationResultResponse richResult = new EvaluationDtos.EvaluationResultResponse(
                saved.getId(),
                saved.getFamily().getId(),
                algo.riskLevel(),
                algo.dimensionScores().entrySet().stream()
                        .map(e -> new EvaluationDtos.DimensionScoreDto(e.getKey(), e.getValue(), e.getValue()))
                        .collect(Collectors.toList()),
                algo.healthyIndex(),
                null,   // riskSnapshotId — lo llena RiskService async
                saved.getSpiritualSynthesis(),
                algo.hasCrisis(),
                algo.simulationSuspected(),
                algo.relapseDetected(),
                algo.suggestedMissionGenerator(),
                algo.consciousnessLabel(),
                algo.consciousnessLevel(),
                algo.relapseFlags(),
                algo.mirrorFlags()
        );

        return new EvaluationDtos.FinalizeResult(saved, richResult);
    }

    @Transactional(readOnly = true)
    public List<EvaluationDtos.TimelineEntryDto> getTimeline(Long familyId) {
        // Cargar InferenceRecords en un Map para lookup O(1) por evaluationId
        Map<Long, InferenceRecord> inferenceByEvalId =
                inferenceRecordRepository.findByFamilyIdOrderByCreatedAtDesc(familyId)
                        .stream()
                        .collect(Collectors.toMap(
                                InferenceRecord::getEvaluationId,
                                ir -> ir,
                                (a, b) -> a   // conservar el más reciente en caso de duplicados
                        ));

        return evaluationRepository.findByFamilyId(familyId).stream()
                .filter(e -> e.getStatus() == EvaluationStatus.FINALIZED)
                .sorted(Comparator.comparing(Evaluation::getFinalizedAt).reversed())
                .map(e -> {
                    InferenceRecord ir = inferenceByEvalId.get(e.getId());
                    return new EvaluationDtos.TimelineEntryDto(
                            e.getId(),
                            e.getFinalizedAt(),
                            e.getIcf(),
                            e.getRiskLevel() != null ? e.getRiskLevel() : "MODERADO",
                            e.getCriticalDimension() != null ? e.getCriticalDimension() : "comunicacion",
                            e.getAlgorithmVersion() != null ? e.getAlgorithmVersion() : "RISK_ALGO_V1",
                            ir != null ? ir.getOperationalState() : null,
                            ir != null ? ir.getUncertaintyTotal() : null
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void processSimulatedResult(Long familyId, Double icf, boolean hasCrisis) {
        log.info("🧪 [SIMULATION] Ejecutando ráfaga para familia: {}", familyId);
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new RuntimeException("Especificación de Familia no encontrada"));

        Evaluation eval = new Evaluation();
        eval.setFamily(family);
        eval.setIcf(icf);
        eval.setRiskLevel(hasCrisis ? "CRITICO" : "MODERADO");
        eval.setCriticalDimension("comunicacion");
        eval.setHasCrisis(hasCrisis);
        eval.setStatus(EvaluationStatus.FINALIZED);
        eval.setFinalizedAt(LocalDateTime.now());
        eval.setMilestoneKey(family.getCurrentMilestone());
        eval.setAlgorithmVersion("RISK_ALGO_V1");

        EvaluationDimensionScore ds = new EvaluationDimensionScore();
        ds.setEvaluation(eval);
        ds.setDimensionName("Integridad");
        ds.setScore(icf);
        eval.getDimensionScores().add(ds);

        Evaluation saved = evaluationRepository.save(eval);
        processPostFinalization(saved, null);
    }

    private void processPostFinalization(Evaluation saved, RiskAlgoV1Engine.AlgoResult algo) {
        String riskLevel = saved.getRiskLevel() != null ? saved.getRiskLevel() : "MODERADO";
        try {
            com.integrityfamily.domain.RiskSnapshot snapshot = riskService.calculateAndCreate(saved.getFamily(), saved.getIcf(), saved.getHasCrisis());
            if (snapshot != null && snapshot.getRiskLevel() != null) {
                riskLevel = snapshot.getRiskLevel();
            }
        } catch (Exception e) {
            log.error("⚠️ [EVALUATION] Error al calcular instantánea de riesgo: {}", e.getMessage());
        }

        try {
            milestoneService.advanceMilestone(saved.getFamily().getId());
            log.info("🚀 [EVALUATION] Hito de la familia ID {} avanzado correctamente.", saved.getFamily().getId());
        } catch (Exception e) {
            log.warn("⚠️ [EVALUATION] Avance de hito omitido para la familia ID {} (No bloqueante para la evaluación): {}", 
                    saved.getFamily().getId(), e.getMessage());
        }

        // Plan generation is triggered exclusively via the "evaluation.completed" RabbitMQ event
        // published below (PLAN_QUEUE binding in RabbitConfig).  The direct synchronous call
        // that used to live here caused double generation: once inline, once via the listener.
        // The listener path is async and already has DLQ retry — no synchronous fallback needed.

        // [DIAGNOSTICO CONSCIENTE] Generar misiones automáticas según rol del miembro
        try {
            planTaskService.generateTasksFromDiagnosis(saved);
            log.info("🎯 [EVALUATION] Misiones automáticas de diagnóstico generadas.");
        } catch (Exception e) {
            log.error("⚠️ [EVALUATION] Error al generar misiones de diagnóstico: {}", e.getMessage());
        }

        // [MEMORIA COGNITIVA] Capturar episodio y consolidar patrón semántico
        try {
            familyMemoryService.captureEvaluationMemory(saved);
            log.info("🧠 [EVALUATION] Memoria episódica capturada en el sistema cognitivo.");
        } catch (Exception e) {
            log.error("⚠️ [EVALUATION] Error al capturar memoria cognitiva: {}", e.getMessage());
        }

        // [SKILL ENGINE] Detectar patrones, aplicar skills y extraer nuevas habilidades
        try {
            FamilySkillEngine.SkillEngineResult result =
                    familySkillEngine.analyze(saved.getFamily().getId(), saved);
            if (result.hasNewSkill()) {
                log.info("🌱 [EVALUATION] Nueva habilidad cognitiva extraída: '{}'",
                        result.newSkillExtracted().getSkillName());
            }
            if (result.hasAppliedSkills()) {
                log.info("⚙️ [EVALUATION] {} skills activadas para esta familia.",
                        result.appliedSkills().size());
            }
        } catch (Exception e) {
            log.error("⚠️ [EVALUATION] Error en motor de habilidades cognitivas: {}", e.getMessage());
        }

        // [GRAFO DE IDENTIDAD] Actualizar dinámicas relacionales entre miembros
        try {
            FamilyIdentityGraphService.GraphSnapshot graph =
                    familyIdentityGraphService.updateGraph(saved.getFamily().getId(), saved);
            log.info("🕸️ [EVALUATION] Grafo de identidad actualizado. Díadas: {} | Cohesión: {} | Conflictos: {}",
                    graph.totalDyads(), String.format("%.1f", graph.cohesionDensity()), graph.conflictiveEdges());
        } catch (Exception e) {
            log.error("⚠️ [EVALUATION] Error en grafo de identidad: {}", e.getMessage());
        }

        // [NARRATIVA] Evolucionar la historia familiar y detectar puntos de inflexión
        try {
            NarrativeEvolutionEngine.NarrativeSnapshot narrative =
                    narrativeEvolutionEngine.evolve(saved.getFamily().getId(), saved);
            log.info("📖 [EVALUATION] Narrativa evolucionada. Capítulo #{}: '{}' | Fase: {} | Turning point: {}",
                    narrative.currentChapter() != null ? narrative.currentChapter().getChapterNumber() : 1,
                    narrative.currentChapter() != null ? narrative.currentChapter().getTitle() : "—",
                    narrative.currentPhase(),
                    narrative.turningPointDetected());
        } catch (Exception e) {
            log.error("⚠️ [EVALUATION] Error en motor narrativo: {}", e.getMessage());
        }

        // [REFLEXIÓN AUTÓNOMA] Autoevaluación del sistema sobre efectividad de intervenciones
        try {
            FamilyReflectionService.ReflectionReport report =
                    familyReflectionService.reflect(saved.getFamily().getId());
            log.info("🪞 [EVALUATION] Reflexión autónoma completada. Efectividad: {} | Abandono: {}",
                    report.effectiveness().level(), report.abandonmentRisk().level());
        } catch (Exception e) {
            log.error("⚠️ [EVALUATION] Error en reflexión autónoma: {}", e.getMessage());
        }

        // [ANALYTICS] Análisis de Progreso Longitudinal (Rediseño 6.6)
        try {
            familyProgressAnalyticsService.analyzeProgress(saved.getId());
            log.info("📊 [EVALUATION] Análisis de progreso completado y snapshot guardado.");
        } catch (Exception e) {
            log.error("⚠️ [EVALUATION] Error al analizar el progreso: {}", e.getMessage());
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("evaluationId", saved.getId());
        payload.put("icf", saved.getIcf());
        payload.put("familyId", saved.getFamily().getId());
        payload.put("riskLevel", riskLevel);

        com.integrityfamily.common.event.SystemEvent eventObj = 
            com.integrityfamily.common.event.SystemEvent.of(
                "evaluation.completed", 
                saved.getFamily().getId(), 
                payload, 
                "SYSTEM"
            );

        try {
            rabbitTemplate.convertAndSend(com.integrityfamily.common.config.RabbitConfig.EXCHANGE_NAME, "evaluation.completed", eventObj);
            log.info("📧 [EVALUATION] Evento 'evaluation.completed' enviado para familia: {} con riesgo: {}", 
                    saved.getFamily().getId(), riskLevel);
        } catch (Exception e) {
            log.error("❌ [EVALUATION] Error al publicar evento 'evaluation.completed' a RabbitMQ (resiliencia activada): {}", e.getMessage());
        }
    }

    @Transactional
    public void delete(Long id) {
        evaluationRepository.deleteById(id);
    }
}
