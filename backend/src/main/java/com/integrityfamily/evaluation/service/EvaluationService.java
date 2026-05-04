package com.integrityfamily.evaluation.service;

import com.integrityfamily.ai.service.AiService;
import com.integrityfamily.domain.*;
import com.integrityfamily.dto.EvaluationDtos;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.MemberRepository;
import com.integrityfamily.domain.repository.QuestionRepository;
import com.integrityfamily.risk.service.RiskService;
import com.integrityfamily.milestone.service.MilestoneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SDD: Motor de Evaluación del Nodo Armenia.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final FamilyRepository familyRepository;
    private final MemberRepository memberRepository;
    private final QuestionRepository questionRepository;
    private final RiskService riskService;
    private final RabbitTemplate rabbitTemplate;
    private final MilestoneService milestoneService;
    private final AiService aiService;

    public List<Evaluation> findAll() {
        return evaluationRepository.findAll();
    }

    public List<Evaluation> findByFamilyId(Long familyId) {
        return evaluationRepository.findByFamilyId(familyId);
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

        if (req.memberId() != null) {
            FamilyMember member = memberRepository.findById(req.memberId()).orElse(null);
            evaluation.setMember(member);
        }

        return evaluationRepository.save(evaluation);
    }

    /**
     * Finaliza un diagnóstico real enviado desde el Frontend.
     * SDD-FIX: Ahora calcula ICF y Dimensiones si el frontend no los envía.
     */
    @Transactional
    public Evaluation finalize(Long id, EvaluationDtos.EvaluationFinalizeRequest request) {
        log.info("🏁 [EVALUATION] Finalizando diagnóstico ID: {}", id);
        Evaluation existing = findById(id);

        existing.setStatus(EvaluationStatus.FINALIZED);
        existing.setFinalizedAt(LocalDateTime.now());

        // Lógica de Autocalculo si vienen nulos
        double icf = (request.icf() != null) ? request.icf() : calculateIcf(request.answers());
        boolean hasCrisis = (request.hasCrisis() != null) ? request.hasCrisis() : icf < 30.0;
        Map<String, Double> scores = (request.dimensionScores() != null) ? request.dimensionScores() : calculateDimensionScores(request.answers());

        existing.setIcf(icf);
        existing.setHasCrisis(hasCrisis);

        scores.forEach((name, score) -> {
            EvaluationDimensionScore ds = new EvaluationDimensionScore();
            ds.setEvaluation(existing);
            ds.setDimensionName(name);
            ds.setScore(score);
            existing.getDimensionScores().add(ds);
        });

        Evaluation saved = evaluationRepository.save(existing);
        log.info("✅ [EVALUATION] Evaluación guardada con éxito (ICF: {}). ID: {}", icf, saved.getId());
        
        processPostFinalization(saved);
        return saved;
    }

    private double calculateIcf(List<EvaluationDtos.AnswerDto> answers) {
        if (answers == null || answers.isEmpty()) return 0.0;
        double sum = answers.stream().mapToInt(a -> a.getEffectiveValue()).sum();
        return (sum / (answers.size() * 5.0)) * 100.0;
    }

    private Map<String, Double> calculateDimensionScores(List<EvaluationDtos.AnswerDto> answers) {
        if (answers == null || answers.isEmpty()) return Collections.emptyMap();
        
        Map<String, List<Integer>> dimValues = new HashMap<>();
        for (EvaluationDtos.AnswerDto a : answers) {
            questionRepository.findById(a.questionId()).ifPresent(q -> {
                String dim = q.getDimension() != null ? q.getDimension() : "GENERAL";
                dimValues.computeIfAbsent(dim, k -> new ArrayList<>()).add(a.getEffectiveValue());
            });
        }

        Map<String, Double> result = new HashMap<>();
        dimValues.forEach((dim, vals) -> {
            double avg = vals.stream().mapToInt(Integer::intValue).average().orElse(0.0);
            result.put(dim, (avg / 5.0) * 100.0);
        });
        return result;
    }

    @Transactional
    public void processSimulatedResult(Long familyId, Double icf, boolean hasCrisis) {
        log.info("🧪 [SIMULATION] Ejecutando ráfaga para familia: {}", familyId);
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new RuntimeException("Especificación de Familia no encontrada"));

        Evaluation eval = new Evaluation();
        eval.setFamily(family);
        eval.setIcf(icf);
        eval.setHasCrisis(hasCrisis);
        eval.setStatus(EvaluationStatus.FINALIZED);
        eval.setFinalizedAt(LocalDateTime.now());
        eval.setMilestoneKey(family.getCurrentMilestone());

        EvaluationDimensionScore ds = new EvaluationDimensionScore();
        ds.setEvaluation(eval);
        ds.setDimensionName("Integridad");
        ds.setScore(icf);
        eval.getDimensionScores().add(ds);

        Evaluation saved = evaluationRepository.save(eval);
        processPostFinalization(saved);
    }

    private void processPostFinalization(Evaluation saved) {
        riskService.calculateAndCreate(saved.getFamily(), saved.getIcf(), saved.getHasCrisis());
        milestoneService.advanceMilestone(saved.getFamily().getId());

        Map<String, Object> payload = new HashMap<>();
        payload.put("evaluationId", saved.getId());
        payload.put("icf", saved.getIcf());
        payload.put("familyId", saved.getFamily().getId());

        com.integrityfamily.common.event.SystemEvent eventObj = 
            com.integrityfamily.common.event.SystemEvent.of(
                "evaluation.completed", 
                saved.getFamily().getId(), 
                payload, 
                "SYSTEM"
            );

        rabbitTemplate.convertAndSend(com.integrityfamily.common.config.RabbitConfig.EXCHANGE_NAME, "evaluation.completed", eventObj);
        log.info("📧 [EVALUATION] Evento 'evaluation.completed' enviado para familia: {}", saved.getFamily().getId());
    }

    @Transactional
    public void delete(Long id) {
        evaluationRepository.deleteById(id);
    }
}
