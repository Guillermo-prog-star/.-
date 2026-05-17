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
import com.integrityfamily.plan.service.PlanTaskService;
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

    private final EvaluationRepository evaluationRepository;
    private final FamilyRepository familyRepository;
    private final MemberRepository memberRepository;
    private final QuestionRepository questionRepository;
    private final RiskService riskService;
    private final RabbitTemplate rabbitTemplate;
    private final MilestoneService milestoneService;
    private final AiService aiService;
    private final PlanTaskService planTaskService;

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
     */
    @Transactional
    public Evaluation finalize(Long id, EvaluationDtos.EvaluationFinalizeRequest request) {
        log.info("🏁 [EVALUATION-ALGO] Finalizando diagnóstico ID: {} bajo RISK_ALGO_V1", id);
        Evaluation existing = findById(id);

        existing.setStatus(EvaluationStatus.FINALIZED);
        existing.setFinalizedAt(LocalDateTime.now());
        existing.setAlgorithmVersion("RISK_ALGO_V1");

        // Guardar respuestas individuales en persistencia
        if (request.answers() != null) {
            for (EvaluationDtos.AnswerDto a : request.answers()) {
                questionRepository.findById(a.questionId()).ifPresent(q -> {
                    EvaluationAnswer answer = new EvaluationAnswer();
                    answer.setEvaluation(existing);
                    answer.setQuestionKey(q.getQuestionKey() != null ? q.getQuestionKey() : "Q-" + q.getId());
                    answer.setScore(a.getEffectiveValue());
                    try {
                        if (q.getDimension() != null) {
                            answer.setDimension(DimensionType.valueOf(q.getDimension().toUpperCase().trim()));
                        } else {
                            answer.setDimension(DimensionType.COMMITMENT);
                        }
                    } catch (Exception ex) {
                        answer.setDimension(DimensionType.COMMITMENT);
                    }
                    existing.getAnswers().add(answer);
                });
            }
        }

        // Ejecutar Algoritmo Oficial de Riesgo RISK_ALGO_V1
        Map<String, Double> dimensionScores = calculateDimensionScoresAlgo(request.answers());
        double healthyIndex = calculateHealthyIndexAlgo(dimensionScores);
        String riskLevel = determineRiskLevelAlgo(healthyIndex, dimensionScores);
        String criticalDimension = detectCriticalDimensionAlgo(dimensionScores);

        existing.setIcf(healthyIndex);
        existing.setRiskLevel(riskLevel);
        existing.setCriticalDimension(criticalDimension);
        existing.setHasCrisis("CRITICO".equalsIgnoreCase(riskLevel) || "HIGH".equalsIgnoreCase(riskLevel) || "ALTO".equalsIgnoreCase(riskLevel));

        dimensionScores.forEach((name, score) -> {
            EvaluationDimensionScore ds = new EvaluationDimensionScore();
            ds.setEvaluation(existing);
            ds.setDimensionName(name);
            ds.setScore(score);
            existing.getDimensionScores().add(ds);
        });

        // Adaptación al Nuevo Modelo: Diagnóstico Consciente
        generateConsciousInterpretation(existing);

        Evaluation saved = evaluationRepository.save(existing);
        log.info("✅ [EVALUATION-ALGO] Evaluación persistida con éxito. ICF/HealthyIndex: {} | Riesgo: {} | Dim Crítica: {}", 
                healthyIndex, riskLevel, criticalDimension);
        
        processPostFinalization(saved);
        return saved;
    }

    /**
     * Genera una interpretación cualitativa basada en el rol del miembro y el resultado,
     * alineada con el modelo de "Sistema de Evolución Consciente".
     */
    private void generateConsciousInterpretation(Evaluation evaluation) {
        if (evaluation.getMember() == null) return;
        
        String role = evaluation.getMember().getRole();
        if (role == null) return;
        
        log.info("🧠 [DIAGNOSTICO-CONSCIENTE] Interpretando resultado para rol: {}", role);
        StringBuilder synthesis = new StringBuilder();
        synthesis.append("[DIAGNÓSTICO CONSCIENTE]\n");
        
        switch (role.toUpperCase()) {
            case "PADRE":
                synthesis.append("Foco: Liderazgo emocional y presencia física consciente.\n");
                if (evaluation.getIcf() < 60) {
                    synthesis.append("Recomendación: Espacios de escucha activa para reducir el estrés y la desconexión.");
                }
                break;
            case "MADRE":
                synthesis.append("Foco: Distribución de la carga mental y autocuidado.\n");
                if (evaluation.getIcf() < 60) {
                    synthesis.append("Recomendación: Delegar tareas y buscar apoyo emocional en la familia.");
                }
                break;
            case "ADOLESCENTE":
                synthesis.append("Foco: Expresión emocional segura y pertenencia.\n");
                synthesis.append("Recomendación: Evitar la imposición; fomentar la participación voluntaria.");
                break;
            case "NINO":
            case "NIÑO":
                synthesis.append("Foco: Hábitos positivos y juego consciente.\n");
                synthesis.append("Recomendación: Rutinas divertidas y seguridad emocional.");
                break;
            default:
                synthesis.append("Foco: Seguimiento adaptativo general.");
                break;
        }
        
        evaluation.setSpiritualSynthesis(synthesis.toString());
        log.info("💡 Síntesis generada: {}", synthesis.toString().replace("\n", " | "));
    }

    /**
     * Normalización 0-100 y cálculo de promedios por dimensión.
     * Positiva: ((val - 1) / 4) * 100
     * Negativa: ((5 - val) / 4) * 100
     */
    private Map<String, Double> calculateDimensionScoresAlgo(List<EvaluationDtos.AnswerDto> answers) {
        Map<String, List<Double>> dimNormalizedValues = new HashMap<>();
        // Inicializar dimensiones requeridas
        dimNormalizedValues.put("emociones", new ArrayList<>());
        dimNormalizedValues.put("comunicacion", new ArrayList<>());
        dimNormalizedValues.put("habitos", new ArrayList<>());
        dimNormalizedValues.put("tiempos", new ArrayList<>());

        if (answers != null) {
            for (EvaluationDtos.AnswerDto a : answers) {
                questionRepository.findById(a.questionId()).ifPresent(q -> {
                    String dim = q.getDimension() != null ? q.getDimension().toLowerCase().trim() : "emociones";
                    if (!dimNormalizedValues.containsKey(dim)) {
                        dim = "emociones"; // Fallback seguro
                    }
                    double val = a.getEffectiveValue();
                    double normScore;
                    if ("NEGATIVE".equalsIgnoreCase(q.getDirection())) {
                        normScore = ((5.0 - val) / 4.0) * 100.0;
                    } else {
                        normScore = ((val - 1.0) / 4.0) * 100.0;
                    }
                    dimNormalizedValues.get(dim).add(normScore);
                });
            }
        }

        Map<String, Double> result = new HashMap<>();
        dimNormalizedValues.forEach((dim, vals) -> {
            double avg = vals.isEmpty() ? 100.0 : vals.stream().mapToDouble(Double::doubleValue).average().orElse(100.0);
            result.put(dim, avg);
        });
        return result;
    }

    /**
     * Fórmula Ponderada Oficial:
     * emotions * 0.30 + communication * 0.30 + habits * 0.20 + time * 0.20
     */
    private double calculateHealthyIndexAlgo(Map<String, Double> scores) {
        double emo = scores.getOrDefault("emociones", 100.0);
        double com = scores.getOrDefault("comunicacion", 100.0);
        double hab = scores.getOrDefault("habitos", 100.0);
        double tim = scores.getOrDefault("tiempos", 100.0);

        return (emo * 0.30) + (com * 0.30) + (hab * 0.20) + (tim * 0.20);
    }

    /**
     * Clasificación y Regla de Seguridad Crítica.
     */
    private String determineRiskLevelAlgo(double healthyIndex, Map<String, Double> scores) {
        String baseRisk;
        if (healthyIndex >= 80.0) {
            baseRisk = "BAJO";
        } else if (healthyIndex >= 60.0) {
            baseRisk = "MODERADO";
        } else if (healthyIndex >= 40.0) {
            baseRisk = "ALTO";
        } else {
            baseRisk = "CRITICO";
        }

        // Regla de Seguridad Crítica
        boolean anyUnder25 = scores.values().stream().anyMatch(s -> s < 25.0);
        boolean anyUnder40 = scores.values().stream().anyMatch(s -> s < 40.0);

        if (anyUnder25) {
            return "CRITICO";
        } else if (anyUnder40 && ("BAJO".equals(baseRisk) || "MODERADO".equals(baseRisk))) {
            return "ALTO";
        }

        return baseRisk;
    }

    private String detectCriticalDimensionAlgo(Map<String, Double> scores) {
        return scores.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("emociones");
    }

    @Transactional(readOnly = true)
    public List<EvaluationDtos.TimelineEntryDto> getTimeline(Long familyId) {
        return evaluationRepository.findByFamilyId(familyId).stream()
                .filter(e -> e.getStatus() == EvaluationStatus.FINALIZED)
                .sorted(Comparator.comparing(Evaluation::getFinalizedAt).reversed())
                .map(e -> new EvaluationDtos.TimelineEntryDto(
                        e.getId(),
                        e.getFinalizedAt(),
                        e.getIcf(),
                        e.getRiskLevel() != null ? e.getRiskLevel() : "MODERADO",
                        e.getCriticalDimension() != null ? e.getCriticalDimension() : "comunicacion",
                        e.getAlgorithmVersion() != null ? e.getAlgorithmVersion() : "RISK_ALGO_V1"
                ))
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
        processPostFinalization(saved);
    }

    private void processPostFinalization(Evaluation saved) {
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

        try {
            planTaskService.generateTasksFromDiagnosis(saved);
            log.info("🎯 [EVALUATION] Misiones automáticas generadas para el diagnóstico.");
        } catch (Exception e) {
            log.error("⚠️ [EVALUATION] Error al generar misiones automáticas: {}", e.getMessage());
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

        rabbitTemplate.convertAndSend(com.integrityfamily.common.config.RabbitConfig.EXCHANGE_NAME, "evaluation.completed", eventObj);
        log.info("📧 [EVALUATION] Evento 'evaluation.completed' enviado para familia: {} con riesgo: {}", 
                saved.getFamily().getId(), riskLevel);
    }

    @Transactional
    public void delete(Long id) {
        evaluationRepository.deleteById(id);
    }
}
