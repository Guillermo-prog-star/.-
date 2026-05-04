package com.integrityfamily.assessment.controller;

import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.Question;
import com.integrityfamily.domain.repository.QuestionRepository;
import com.integrityfamily.evaluation.service.EvaluationService;
import com.integrityfamily.dto.EvaluationDtos;
import com.integrityfamily.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SDD: Controlador de Diagnóstico Transformacional Sincronizado.
 * Postura Técnica: Cumplimiento estricto del contrato esperado por el Frontend.
 */
@Slf4j
@RestController
@RequestMapping("/api/assessments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AssessmentController {

    private final EvaluationService evaluationService;
    private final QuestionRepository questionRepository;

    @GetMapping("/random")
    public ApiResponse<List<Question>> getRandomQuestions(@RequestParam(required = false) Long familyId) {
        log.info("[ASSESSMENT-SDD] Solicitud de preguntas aleatorias para familia: {}", familyId);
        
        List<Question> questions = questionRepository.findAll().stream()
                .limit(20)
                .collect(Collectors.toList());
        
        log.info("[ASSESSMENT-SDD] Enviando {} preguntas al frontend", questions.size());
        return ApiResponse.ok(questions);
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
