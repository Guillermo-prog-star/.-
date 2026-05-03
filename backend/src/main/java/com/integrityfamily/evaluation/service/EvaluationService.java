package com.integrityfamily.evaluation.service;

import com.integrityfamily.ai.service.AiService;
import com.integrityfamily.domain.*;
import com.integrityfamily.dto.EvaluationDtos;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.MemberRepository;
import com.integrityfamily.risk.service.RiskService;
import com.integrityfamily.milestone.service.MilestoneService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SDD: Motor de EvaluaciÃƒÂ³n del Nodo Armenia.
 * Gestiona el ciclo de vida de los diagnÃƒÂ³sticos y dispara la inteligencia
 * reactiva.
 */
@Slf4j
@Service
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final FamilyRepository familyRepository;
    private final MemberRepository memberRepository;
    private final RiskService riskService;
    private final RabbitTemplate rabbitTemplate;
    private final MilestoneService milestoneService;
    private final AiService aiService;

    public EvaluationService(EvaluationRepository evaluationRepository,
            FamilyRepository familyRepository,
            MemberRepository memberRepository,
            RiskService riskService,
            RabbitTemplate rabbitTemplate,
            MilestoneService milestoneService,
            AiService aiService) {
        this.evaluationRepository = evaluationRepository;
        this.familyRepository = familyRepository;
        this.memberRepository = memberRepository;
        this.riskService = riskService;
        this.rabbitTemplate = rabbitTemplate;
        this.milestoneService = milestoneService;
        this.aiService = aiService;
    }

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

    /**
     * SDD: Inicia un nuevo diagnóstico vinculándolo a la familia y opcionalmente a un miembro.
     */
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
     * Finaliza un diagnÃƒÂ³stico real enviado desde el Frontend.
     */
    @Transactional
    public Evaluation finalize(Long id, EvaluationDtos.EvaluationFinalizeRequest request) {
        log.info("Ã°Å¸ÂÂ [EVALUATION] Finalizando diagnÃƒÂ³stico ID: {}", id);
        Evaluation existing = findById(id);

        existing.setStatus(EvaluationStatus.FINALIZED);
        existing.setFinalizedAt(LocalDateTime.now());
        existing.setIcf(request.icf());
        existing.setHasCrisis(request.hasCrisis());

        request.dimensionScores().forEach((name, score) -> {
            EvaluationDimensionScore ds = new EvaluationDimensionScore();
            ds.setEvaluation(existing);
            ds.setDimensionName(name);
            ds.setScore(score);
            existing.getDimensionScores().add(ds);
        });

        Evaluation saved = evaluationRepository.save(existing);
        
        // SDD-UIE: Generación de Síntesis Espiritual Narrativa (Inyección Directa)
        try {
            log.info("Ã°Å¸Â§Â  [UIE] Generando sÃƒÂ­ntesis espiritual para la evaluaciÃƒÂ³n...");
            String synthesis = aiService.generateExecutiveSynthesis(saved);
            saved.setSpiritualSynthesis(synthesis);
            evaluationRepository.save(saved);
        } catch (Exception e) {
            log.error("Ã¢Â Å’ [UIE] Error al generar sÃƒÂ­ntesis: {}", e.getMessage());
        }

        processPostFinalization(saved);
        return saved;
    }

    /**
     * SDD: Protocolo Sentinel para inyecciÃƒÂ³n de rÃƒÂ¡fagas de simulaciÃƒÂ³n.
     */
    @Transactional
    public void processSimulatedResult(Long familyId, Double icf, boolean hasCrisis) {
        log.info("Ã°Å¸Â§Âª [SIMULATION] Ejecutando rÃƒÂ¡faga para familia: {}", familyId);
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new RuntimeException("EspecificaciÃƒÂ³n de Familia no encontrada"));

        Evaluation eval = new Evaluation();
        eval.setFamily(family);
        eval.setIcf(icf);
        eval.setHasCrisis(hasCrisis);
        eval.setStatus(EvaluationStatus.FINALIZED);
        eval.setFinalizedAt(LocalDateTime.now());
        eval.setMilestoneKey(family.getCurrentMilestone());

        // SDD: InyecciÃƒÂ³n de dimensiÃƒÂ³n tÃƒÂ©cnica para consistencia del Radar Chart
        EvaluationDimensionScore ds = new EvaluationDimensionScore();
        ds.setEvaluation(eval);
        ds.setDimensionName("Integridad");
        ds.setScore(icf);
        eval.getDimensionScores().add(ds);

        Evaluation saved = evaluationRepository.save(eval);
        processPostFinalization(saved);
    }

    /**
     * Orquestador de lÃƒÂ³gica reactiva post-diagnÃƒÂ³stico.
     */
    private void processPostFinalization(Evaluation saved) {
        // 1. SDD: Actualizar Motor de Riesgo (Sentinel)
        riskService.calculateAndCreate(saved.getFamily(), saved.getIcf(), saved.getHasCrisis());

        // 2. SDD: TransiciÃƒÂ³n de Hito Territorial (Armenia-QuindÃƒÂ­o)
        milestoneService.advanceMilestone(saved.getFamily().getId());

        // 3. SDD: Disparo de MensajerÃƒÂ­a AsÃƒÂ­ncrona (RabbitMQ)
        Map<String, Object> event = new HashMap<>();
        event.put("evaluationId", saved.getId());
        event.put("familyId", saved.getFamily().getId());
        event.put("icf", saved.getIcf());
        event.put("crisis", saved.getHasCrisis());

        rabbitTemplate.convertAndSend("if.plan.queue", event);
        log.info("Ã°Å¸â€œÂ§ [EVALUATION] Evento enviado a if.plan.queue (Trigger Plan de IntervenciÃƒÂ³n)");
    }

    @Transactional
    public void delete(Long id) {
        evaluationRepository.deleteById(id);
    }
}


