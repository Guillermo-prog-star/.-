package com.integrityfamily.assessment.controller;

import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.Question;
import com.integrityfamily.domain.EvaluationStatus;
import com.integrityfamily.domain.EvaluationDimensionScore;
import com.integrityfamily.domain.repository.QuestionRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.evaluation.service.EvaluationService;
import com.integrityfamily.dto.EvaluationDtos;
import com.integrityfamily.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SDD: Controlador de Diagnóstico Transformacional Sincronizado.
 * Postura Técnica: Cumplimiento estricto del contrato esperado por el Frontend con un motor psicométrico adaptativo híbrido.
 */
@Slf4j
@RestController
@RequestMapping("/api/assessments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AssessmentController {

    private final EvaluationService evaluationService;
    private final QuestionRepository questionRepository;
    private final FamilyRepository familyRepository;
    private final EvaluationRepository evaluationRepository;

    /**
     * Motor Psicométrico Adaptativo Longitudinal (v5.0).
     * Selecciona inteligentemente un set de 20 preguntas divididas en:
     * - 6 Núcleo (Longitudinales fijos de base para comparar evolución histórica)
     * - 6 Adaptativas por Riesgo (Profundización en la dimensión más vulnerable)
     * - 4 Fase/Pilar Actual (Maturity match basado en el Hito actual del plan)
     * - 2 Contraste/Espejo (Control de simulación / incoherencia clínica)
     * - 2 Exploratorias IA (Detección preventiva de riesgos emergentes)
     */
    @GetMapping("/random")
    public ApiResponse<List<Question>> getRandomQuestions(@RequestParam(required = false) Long familyId) {
        log.info("[ASSESSMENT-SDD] Solicitud de diagnóstico adaptativo para familia: {}", familyId);
        
        if (familyId == null) {
            log.warn("[ASSESSMENT-SDD] No familyId provided. Returning fallback default list.");
            return ApiResponse.ok(getDefaultFallbackQuestions(20));
        }

        Optional<Family> familyOpt = familyRepository.findById(familyId);
        if (familyOpt.isEmpty()) {
            log.warn("[ASSESSMENT-SDD] Family with ID {} not found. Returning fallback default list.", familyId);
            return ApiResponse.ok(getDefaultFallbackQuestions(20));
        }

        Family family = familyOpt.get();
        String currentMilestone = family.getCurrentMilestone() != null ? family.getCurrentMilestone() : "M00";

        // 1. Identificar la dimensión más vulnerable mediante la última evaluación finalizada
        String vulnerableDimension = detectVulnerableDimension(familyId);
        log.info("[ASSESSMENT-SDD] Familia ID: {} ({}) | Hito Actual: {} | Dimensión Crítica/Riesgo: {}", 
                familyId, family.getName(), currentMilestone, vulnerableDimension);

        // 2. Cargar preguntas activas del banco
        List<Question> allQuestions = questionRepository.findByActiveTrue();
        if (allQuestions.size() < 20) {
            log.warn("[ASSESSMENT-SDD] Bank has fewer than 20 active questions ({}). Returning all.", allQuestions.size());
            return ApiResponse.ok(allQuestions);
        }

        Set<Question> selectedQuestions = new LinkedHashSet<>();

        // Sub-pools de preguntas de acuerdo al tipo y taxonomía
        List<Question> corePool = new ArrayList<>();
        List<Question> adaptivePool = new ArrayList<>();
        List<Question> phasePool = new ArrayList<>();
        List<Question> mirrorPool = new ArrayList<>();
        List<Question> exploratoryPool = new ArrayList<>();

        for (Question q : allQuestions) {
            String type = q.getType() != null ? q.getType().toUpperCase() : "CORE";
            
            // Categorización por Pool
            if ("CORE".equals(type)) {
                corePool.add(q);
            } else if ("ADAPTIVE".equals(type) || (q.getDimension() != null && q.getDimension().equalsIgnoreCase(vulnerableDimension))) {
                adaptivePool.add(q);
            } else if ("FASE_PILLAR".equals(type) || (q.getPillar() != null && q.getPillar().equalsIgnoreCase(currentMilestone))) {
                phasePool.add(q);
            } else if ("MIRROR".equals(type) || q.isReverseQuestion()) {
                mirrorPool.add(q);
            } else if ("EXPLORATORY".equals(type)) {
                exploratoryPool.add(q);
            }
        }

        // Shuffling de sub-pools para variabilidad de reactivos sin perder el constructo
        Collections.shuffle(corePool);
        Collections.shuffle(adaptivePool);
        Collections.shuffle(phasePool);
        Collections.shuffle(mirrorPool);
        Collections.shuffle(exploratoryPool);
        
        // Unir todos los pools mezclados como pool general de reserva
        List<Question> generalFallback = new ArrayList<>(allQuestions);
        Collections.shuffle(generalFallback);

        // 3. Extraer cuotas exactas del modelo híbrido adaptativo
        drawQuestions(selectedQuestions, corePool, 6);        // 6 Núcleo
        drawQuestions(selectedQuestions, adaptivePool, 6);    // 6 Adaptativas por Riesgo
        drawQuestions(selectedQuestions, phasePool, 4);       // 4 Fase/Pilar Actual
        drawQuestions(selectedQuestions, mirrorPool, 2);      // 2 Espejo/Contraste
        drawQuestions(selectedQuestions, exploratoryPool, 2); // 2 Exploratorias IA

        // 4. Completar cuota de 20 preguntas si alguna subcategoría se quedó corta
        if (selectedQuestions.size() < 20) {
            log.info("[ASSESSMENT-SDD] Sub-pools insufficient (drawn: {}). Completing quota from fallback pool.", selectedQuestions.size());
            for (Question q : generalFallback) {
                if (selectedQuestions.size() >= 20) break;
                selectedQuestions.add(q);
            }
        }

        // 5. Mezclar el set de 20 preguntas final para evitar efecto aprendizaje/orden
        List<Question> finalAssessment = new ArrayList<>(selectedQuestions);
        Collections.shuffle(finalAssessment);

        log.info("[ASSESSMENT-SDD] Set psicométrico de 20 preguntas enviado con éxito al frontend. Familia: {}", familyId);
        return ApiResponse.ok(finalAssessment);
    }

    private String detectVulnerableDimension(Long familyId) {
        Optional<Evaluation> lastEvalOpt = evaluationRepository.findFirstByFamilyIdAndStatusOrderByFinalizedAtDesc(familyId, EvaluationStatus.FINALIZED);
        if (lastEvalOpt.isEmpty()) {
            return "comunicacion"; // Default prioritario baseline si no hay diagnóstico previo
        }

        Evaluation lastEval = lastEvalOpt.get();
        if (lastEval.getDimensionScores() == null || lastEval.getDimensionScores().isEmpty()) {
            return "comunicacion";
        }

        // Detectar dimensión con menor puntaje (mayor vulnerabilidad/riesgo)
        return lastEval.getDimensionScores().stream()
                .min(Comparator.comparingDouble(EvaluationDimensionScore::getScore))
                .map(EvaluationDimensionScore::getDimensionName)
                .orElse("comunicacion");
    }

    private void drawQuestions(Set<Question> target, List<Question> source, int limit) {
        int drawn = 0;
        for (Question q : source) {
            if (drawn >= limit) break;
            if (target.add(q)) {
                drawn++;
            }
        }
    }

    private List<Question> getDefaultFallbackQuestions(int limit) {
        List<Question> list = questionRepository.findAll();
        Collections.shuffle(list);
        return list.stream().limit(limit).toList();
    }

    @GetMapping("/family/{familyId}/history")
    public ResponseEntity<ApiResponse<List<EvaluationDtos.EvaluationResponse>>> getHistory(@PathVariable Long familyId) {
        List<EvaluationDtos.EvaluationResponse> history = evaluationService.findByFamilyId(familyId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<EvaluationDtos.EvaluationResponse>> startEvaluation(@RequestBody EvaluationDtos.EvaluationStartRequest req) {
        Evaluation evaluation = evaluationService.start(req);
        return ResponseEntity.ok(ApiResponse.ok(mapToResponse(evaluation)));
    }

    @PostMapping("/{id}/finalize")
    public ResponseEntity<ApiResponse<EvaluationDtos.EvaluationResponse>> finalizeEvaluation(
            @PathVariable Long id,
            @RequestBody EvaluationDtos.EvaluationFinalizeRequest req) {
        Evaluation evaluation = evaluationService.finalize(id, req);
        return ResponseEntity.ok(ApiResponse.ok(mapToResponse(evaluation)));
    }

    private EvaluationDtos.EvaluationResponse mapToResponse(Evaluation evaluation) {
        return new EvaluationDtos.EvaluationResponse(
            evaluation.getId(),
            evaluation.getFamily().getId(),
            evaluation.getMember() != null ? evaluation.getMember().getId() : null,
            evaluation.getStatus(),
            evaluation.getStartedAt(),
            evaluation.getFinalizedAt()
        );
    }
}
