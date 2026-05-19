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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/assessments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "2. Assessment & Diagnóstico", description = "Contrato de servicios para el diagnóstico psicométrico familiar, aplicación del algoritmo RISK_ALGO_V1 y consulta del timeline evolutivo.")
public class AssessmentController {

    private final EvaluationService evaluationService;
    private final QuestionRepository questionRepository;
    private final FamilyRepository familyRepository;
    private final EvaluationRepository evaluationRepository;

    @Operation(summary = "Obtener set psicométrico adaptativo", description = "Selecciona inteligentemente un set de 20 preguntas balanceadas entre núcleo, adaptativas por riesgo, fase actual, espejo y exploratorias.")
    @PreAuthorize("#familyId == null or @familySecurity.check(#familyId)")
    @GetMapping("/random")
    public ApiResponse<List<Question>> getRandomQuestions(
            @Parameter(description = "ID de la familia para adaptar reactivos al riesgo", required = false) @RequestParam(required = false) Long familyId) {
        log.info("[ASSESSMENT-SDD] Solicitud de diagnóstico adaptativo para familia: {}", familyId);
        
        if (familyId == null) {
            return ApiResponse.ok(getDefaultFallbackQuestions(20));
        }

        Optional<Family> familyOpt = familyRepository.findById(familyId);
        if (familyOpt.isEmpty()) {
            return ApiResponse.ok(getDefaultFallbackQuestions(20));
        }

        Family family = familyOpt.get();
        String currentMilestone = family.getCurrentMilestone() != null ? family.getCurrentMilestone() : "M00";

        String vulnerableDimension = detectVulnerableDimension(familyId);
        log.info("[ASSESSMENT-SDD] Familia ID: {} ({}) | Hito Actual: {} | Dimensión Crítica/Riesgo: {}", 
                familyId, family.getName(), currentMilestone, vulnerableDimension);

        List<Question> allQuestions = questionRepository.findByActiveTrue();
        if (allQuestions.size() < 20) {
            return ApiResponse.ok(allQuestions);
        }

        Set<Question> selectedQuestions = new LinkedHashSet<>();

        List<Question> corePool = new ArrayList<>();
        List<Question> adaptivePool = new ArrayList<>();
        List<Question> phasePool = new ArrayList<>();
        List<Question> mirrorPool = new ArrayList<>();
        List<Question> exploratoryPool = new ArrayList<>();

        for (Question q : allQuestions) {
            String type = q.getType() != null ? q.getType().toUpperCase() : "CORE";
            
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

        Collections.shuffle(corePool);
        Collections.shuffle(adaptivePool);
        Collections.shuffle(phasePool);
        Collections.shuffle(mirrorPool);
        Collections.shuffle(exploratoryPool);
        
        List<Question> generalFallback = new ArrayList<>(allQuestions);
        Collections.shuffle(generalFallback);

        drawQuestions(selectedQuestions, corePool, 6);
        drawQuestions(selectedQuestions, adaptivePool, 6);
        drawQuestions(selectedQuestions, phasePool, 4);
        drawQuestions(selectedQuestions, mirrorPool, 2);
        drawQuestions(selectedQuestions, exploratoryPool, 2);

        if (selectedQuestions.size() < 20) {
            for (Question q : generalFallback) {
                if (selectedQuestions.size() >= 20) break;
                selectedQuestions.add(q);
            }
        }

        List<Question> finalAssessment = new ArrayList<>(selectedQuestions);
        Collections.shuffle(finalAssessment);

        return ApiResponse.ok(finalAssessment);
    }

    private String detectVulnerableDimension(Long familyId) {
        Optional<Evaluation> lastEvalOpt = evaluationRepository.findFirstByFamilyIdAndStatusOrderByFinalizedAtDesc(familyId, EvaluationStatus.FINALIZED);
        if (lastEvalOpt.isEmpty()) {
            return "comunicacion";
        }

        Evaluation lastEval = lastEvalOpt.get();
        if (lastEval.getDimensionScores() == null || lastEval.getDimensionScores().isEmpty()) {
            return "comunicacion";
        }

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

    @Operation(summary = "Obtener historial de evaluaciones de la familia", description = "Devuelve todas las sesiones de evaluación asociadas a un núcleo familiar.")
    @PreAuthorize("@familySecurity.check(#familyId)")
    @GetMapping("/family/{familyId}/history")
    public ResponseEntity<ApiResponse<List<EvaluationDtos.EvaluationResponse>>> getHistory(@PathVariable Long familyId) {
        List<EvaluationDtos.EvaluationResponse> history = evaluationService.findSummaryByFamilyId(familyId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    private EvaluationDtos.EvaluationResponse mapToResponse(com.integrityfamily.domain.repository.EvaluationSummary evaluation) {
        return new EvaluationDtos.EvaluationResponse(
            evaluation.getId(),
            evaluation.getFamilyId(),
            evaluation.getMemberId(),
            evaluation.getStatus(),
            evaluation.getStartedAt(),
            evaluation.getFinalizedAt(),
            evaluation.getIcf(),
            evaluation.getRiskLevel(),
            evaluation.getCriticalDimension()
        );
    }

    @Operation(summary = "Consultar timeline de evolución diagnóstica", description = "Devuelve el historial evolutivo con el cálculo de índice saludable, nivel de riesgo y dimensión crítica por fecha.")
    @PreAuthorize("@familySecurity.check(#familyId)")
    @GetMapping("/family/{familyId}/timeline")
    public ResponseEntity<ApiResponse<List<EvaluationDtos.TimelineEntryDto>>> getTimeline(
            @Parameter(description = "ID del núcleo familiar", required = true) @PathVariable Long familyId) {
        return ResponseEntity.ok(ApiResponse.ok(evaluationService.getTimeline(familyId)));
    }

    @Operation(summary = "Iniciar nueva sesión de evaluación", description = "Crea una nueva instancia de evaluación en estado STARTED.")
    @PreAuthorize("@familySecurity.check(#req.familyId)")
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<EvaluationDtos.EvaluationResponse>> startEvaluation(@RequestBody EvaluationDtos.EvaluationStartRequest req) {
        Evaluation evaluation = evaluationService.start(req);
        return ResponseEntity.ok(ApiResponse.ok(mapToResponse(evaluation)));
    }

    @Operation(summary = "Finalizar evaluación y aplicar RISK_ALGO_V1", description = "Recibe las respuestas del usuario, normaliza puntajes, clasifica el nivel de riesgo y emite el evento para el plan de mejora.")
    @PreAuthorize("@familySecurity.checkEvaluation(#id)")
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
            evaluation.getFinalizedAt(),
            evaluation.getIcf(),
            evaluation.getRiskLevel(),
            evaluation.getCriticalDimension()
        );
    }
}
