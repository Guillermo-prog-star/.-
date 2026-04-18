package com.integrityfamily.evaluation.service;

import com.integrityfamily.common.service.AiService;
import com.integrityfamily.evaluation.domain.*;
import com.integrityfamily.evaluation.dto.EvaluationDtos;
import com.integrityfamily.evaluation.repository.EvaluationRepository;
import com.integrityfamily.family.domain.Family;
import com.integrityfamily.family.repository.FamilyRepository;
import com.integrityfamily.risk.service.RiskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EvaluationService {

    private static final Logger log = LoggerFactory.getLogger(EvaluationService.class);

    private final EvaluationRepository evaluationRepository;
    private final FamilyRepository familyRepository;
    private final RiskService riskService;
    private final RabbitTemplate rabbitTemplate;
    private final AiService aiService;

    public EvaluationService(EvaluationRepository evaluationRepository, 
                            FamilyRepository familyRepository,
                            RiskService riskService,
                            RabbitTemplate rabbitTemplate,
                            AiService aiService) {
        this.evaluationRepository = evaluationRepository;
        this.familyRepository = familyRepository;
        this.riskService = riskService;
        this.rabbitTemplate = rabbitTemplate;
        this.aiService = aiService;
    }

    public List<Evaluation> findAll() { return evaluationRepository.findAll(); }
    public List<Evaluation> findByFamilyId(Long familyId) { return evaluationRepository.findByFamilyId(familyId); }
    public Evaluation findById(Long id) { return evaluationRepository.findById(id).orElseThrow(); }
    public Evaluation create(Evaluation e) { return evaluationRepository.save(e); }

    @Transactional
    public Evaluation finalize(Long id, EvaluationDtos.EvaluationFinalizeRequest request) {
        log.info("🏁 [EVALUATION] Finalizando diagnóstico ID: {}", id);
        
        Evaluation existing = findById(id);
        existing.setStatus(EvaluationStatus.FINALIZED);
        existing.setFinalizedAt(LocalDateTime.now());
        existing.setIcf(request.icf());
        existing.setHasCrisis(request.hasCrisis());
        
        // Guardar respuestas
        for (EvaluationDtos.AnswerDto a : request.answers()) {
            EvaluationAnswer ans = new EvaluationAnswer();
            ans.setEvaluation(existing);
            ans.setQuestionId(a.questionId());
            ans.setAnswerValue(a.value());
            existing.getAnswers().add(ans);
        }

        // Guardar dimensiones
        for (Map.Entry<String, Double> entry : request.dimensionScores().entrySet()) {
            EvaluationDimensionScore score = new EvaluationDimensionScore();
            score.setEvaluation(existing);
            score.setDimensionName(entry.getKey());
            score.setScore(entry.getValue());
            existing.getDimensionScores().add(score);
        }

        Evaluation saved = evaluationRepository.save(existing);
        
        // Actualizar Sentinel de Riesgo
        riskService.calculateAndCreate(saved.getFamily(), saved.getIcf(), saved.getHasCrisis());

        // Disparar evento de Plan (Asíncrono via RabbitMQ)
        Map<String, Object> event = new HashMap<>();
        event.put("evaluationId", saved.getId());
        event.put("familyId", saved.getFamily().getId());
        event.put("riskLevel", saved.getIcf() < 50 ? "ALTO" : "MEDIO");
        event.put("requiresImmediatePlan", saved.getHasCrisis());
        
        rabbitTemplate.convertAndSend("if.plan.queue", event);
        log.info("📧 [EVALUATION] Evento de plan enviado a RabbitMQ.");

        return saved;
    }

    public void delete(Long id) { evaluationRepository.deleteById(id); }
}