package com.integrityfamily.evaluation.service;

import com.integrityfamily.domain.*;
import com.integrityfamily.dto.EvaluationDtos;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.repository.EvaluationSummary;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.MemberRepository;
import com.integrityfamily.domain.repository.QuestionRepository;
import com.integrityfamily.assessment.service.AssessmentAnswerService;
import com.integrityfamily.domain.repository.EvaluationAnswerRepository;
import com.integrityfamily.domain.repository.HypothesisEvidenceRepository;
import com.integrityfamily.evaluation.service.post.PostFinalizationContext;
import com.integrityfamily.evaluation.service.post.PostFinalizationPipeline;
import com.integrityfamily.risk.service.RiskAlgoV1Engine;
import com.integrityfamily.scanner.domain.InferenceRecord;
import com.integrityfamily.scanner.domain.RuleActivation;
import com.integrityfamily.scanner.repository.InferenceRecordRepository;
import com.integrityfamily.scanner.service.AlertEngine;
import com.integrityfamily.scanner.service.DeterministicExplanationPipeline;
import com.integrityfamily.scanner.service.InferenceRecordService;
import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.scanner.service.RuleExecutionEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
    private final DeterministicExplanationPipeline explanationPipeline;
    private final InferenceRecordService inferenceRecordService;
    private final InferenceRecordRepository inferenceRecordRepository;
    private final RuleExecutionEngine ruleExecutionEngine;
    private final AlertEngine alertEngine;
    private final HypothesisEvidenceRepository hypothesisEvidenceRepository;
    private final PostFinalizationPipeline postFinalizationPipeline;   // ADR-020: efectos posteriores a finalize()

    /** Identificador y versión de la hipótesis de Interrupción Deliberativa (ADR-007). */
    private static final String DELIBERATIVE_INTERRUPTION_HYPOTHESIS = "DELIBERATIVE_INTERRUPTION_HYPOTHESIS";
    private static final String DELIBERATIVE_INTERRUPTION_HYPOTHESIS_VERSION = "v1";

    /** Identificador y versión del proxy de precisión anticipatoria intra-sesión (ADR-008). */
    private static final String PROXY_PREDICTIVE_ACCURACY_HYPOTHESIS = "PROXY_PREDICTIVE_ACCURACY_HYPOTHESIS";
    private static final String PROXY_PREDICTIVE_ACCURACY_HYPOTHESIS_VERSION = "v1";

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
                .orElseThrow(() -> new BusinessException("Familia no encontrada", "FAMILY_NOT_FOUND", HttpStatus.NOT_FOUND));

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
                
                Integer val = a.getEffectiveValue();
                if (val != null && val == 0) {
                    if (!"NEURO_AWARENESS".equals(q.getType()) || !"ENTRY".equalsIgnoreCase(q.getPhase())) {
                        throw new IllegalArgumentException("El valor 0 solo está permitido para preguntas NEURO_AWARENESS en fase ENTRY. Pregunta inválida: " + q.getQuestionKey());
                    }
                }
                
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
        existing.setInc(algo.inc());
        if (algo.neuroProfile() != null) {
            existing.setSomaticAwareness(algo.neuroProfile().getSomaticAwareness());
            existing.setEmotionalAwareness(algo.neuroProfile().getEmotionalAwareness());
            existing.setCognitiveAwareness(algo.neuroProfile().getCognitiveAwareness());
            existing.setImpulsiveAwareness(algo.neuroProfile().getImpulsiveAwareness());
            existing.setPauseCapacity(algo.neuroProfile().getPauseCapacity());
            existing.setIntegrationScore(algo.neuroProfile().getIntegrationScore());
            recordPauseCapacityEvidence(
                    existing.getFamily().getId(),
                    algo.neuroProfile().getPauseCapacity(),
                    existing.getAlgorithmVersion(),
                    existing.getFinalizedAt());
        }
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

        // ADR-008: proxy de precisión anticipatoria (THINK vs. AFTERMATH) — después del save
        // porque en el flujo clásico las EvaluationAnswer del request solo se persisten
        // (cascade) al guardar `existing`; sin @Transactional en este método, una relectura
        // anterior a este punto no las vería.
        recordPredictiveAccuracyEvidence(saved.getFamily().getId(), saved.getId(), saved.getFinalizedAt());

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
                algo.hasCrisis(),
                algo.inc(),
                algo.neuroProfile(),
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
                .orElseThrow(() -> new BusinessException("Especificación de Familia no encontrada", "FAMILY_NOT_FOUND", HttpStatus.NOT_FOUND));

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

    /**
     * Escribe una observación cruda de Interrupción Deliberativa en hypothesis_evidence (ADR-007).
     * Nunca actualiza una fila existente — cada llamada inserta una nueva.
     * Solo se invoca cuando neuroProfile != null (evaluación con preguntas neuro-conductuales) —
     * pauseCapacity es un double primitivo y nunca es null, así que llamar esto fuera de ese
     * guard escribiría 0.0 como si fuera "ausencia de pausa" en vez de "no se preguntó".
     */
    private void recordPauseCapacityEvidence(Long familyId, double pauseCapacity, String algorithmVersion, LocalDateTime observedAt) {
        hypothesisEvidenceRepository.save(HypothesisEvidence.builder()
                .hypothesis(DELIBERATIVE_INTERRUPTION_HYPOTHESIS)
                .hypothesisVersion(DELIBERATIVE_INTERRUPTION_HYPOTHESIS_VERSION)
                .subjectType("FAMILY")
                .subjectId(familyId)
                .measurementType("PAUSE_CAPACITY")
                .measurementValue(pauseCapacity)
                .instrument(algorithmVersion != null ? algorithmVersion : "RISK_ALGO_V1")
                .instrumentVersion("1")
                .source(EvidenceSource.AUTOMATIC)
                .observedAt(observedAt != null ? observedAt : LocalDateTime.now())
                .build());
    }

    /**
     * ADR-008: por cada escenario (parent_key) de esta evaluación con respuesta registrada
     * tanto en fase THINK como en fase AFTERMATH, escribe el par de observaciones crudas en
     * hypothesis_evidence. Si falta cualquiera de las dos fases para un escenario, no se
     * escribe nada para ese escenario — un par incompleto no es una observación válida.
     * score_value == rubric_level siempre en el banco SCENARIO_V1_2 (V89-V95, verificado),
     * así que EvaluationAnswer.score ya es directamente el rubric_level, sin volver a
     * consultar question_options.
     */
    private void recordPredictiveAccuracyEvidence(Long familyId, Long evaluationId, LocalDateTime observedAt) {
        List<EvaluationAnswer> answers = evaluationAnswerRepository.findByEvaluationIdOrderByAnsweredAtAsc(evaluationId);

        List<Long> questionIds = answers.stream()
                .map(EvaluationAnswer::getQuestionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Question> questionById = questionRepository.findAllById(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        Map<String, Integer> thinkByParentKey = new HashMap<>();
        Map<String, Integer> aftermathByParentKey = new HashMap<>();

        for (EvaluationAnswer a : answers) {
            Question q = questionById.get(a.getQuestionId());
            if (q == null || q.getParentKey() == null || a.getScore() == null) continue;
            if ("THINK".equals(q.getPhase())) thinkByParentKey.put(q.getParentKey(), a.getScore());
            if ("AFTERMATH".equals(q.getPhase())) aftermathByParentKey.put(q.getParentKey(), a.getScore());
        }

        for (Map.Entry<String, Integer> entry : thinkByParentKey.entrySet()) {
            String parentKey = entry.getKey();
            Integer thinkScore = entry.getValue();
            Integer aftermathScore = aftermathByParentKey.get(parentKey);
            if (aftermathScore == null) continue; // par incompleto — no se escribe nada (ADR-008 Decisión 3)

            hypothesisEvidenceRepository.save(HypothesisEvidence.builder()
                    .hypothesis(PROXY_PREDICTIVE_ACCURACY_HYPOTHESIS)
                    .hypothesisVersion(PROXY_PREDICTIVE_ACCURACY_HYPOTHESIS_VERSION)
                    .subjectType("FAMILY")
                    .subjectId(familyId)
                    .measurementType("ANTICIPATED_THREAT_LEVEL")
                    .measurementValue(thinkScore.doubleValue())
                    .instrument(parentKey)
                    .instrumentVersion("1.2.0")
                    .source(EvidenceSource.AUTOMATIC)
                    .observedAt(observedAt != null ? observedAt : LocalDateTime.now())
                    .build());

            hypothesisEvidenceRepository.save(HypothesisEvidence.builder()
                    .hypothesis(PROXY_PREDICTIVE_ACCURACY_HYPOTHESIS)
                    .hypothesisVersion(PROXY_PREDICTIVE_ACCURACY_HYPOTHESIS_VERSION)
                    .subjectType("FAMILY")
                    .subjectId(familyId)
                    .measurementType("OUTCOME_SEVERITY_LEVEL")
                    .measurementValue(aftermathScore.doubleValue())
                    .instrument(parentKey)
                    .instrumentVersion("1.2.0")
                    .source(EvidenceSource.AUTOMATIC)
                    .observedAt(observedAt != null ? observedAt : LocalDateTime.now())
                    .build());
        }
    }

    /**
     * Efectos posteriores a la finalización (ADR-020, corte B). Cada efecto es un
     * {@code PostFinalizationStep} ordenado por {@code @Order}; este servicio ya no
     * conoce a los colaboradores cognitivos, de riesgo, de hito, de notificación ni
     * de mensajería.
     */
    private void processPostFinalization(Evaluation saved, RiskAlgoV1Engine.AlgoResult algo) {
        postFinalizationPipeline.run(new PostFinalizationContext(saved, algo));
    }

    @Transactional
    public void delete(Long id) {
        evaluationRepository.deleteById(id);
    }
}
