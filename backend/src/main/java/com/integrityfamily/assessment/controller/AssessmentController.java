package com.integrityfamily.assessment.controller;

import com.integrityfamily.assessment.domain.Assessment;
import com.integrityfamily.assessment.domain.Question;
import com.integrityfamily.assessment.service.AssessmentService;
import com.integrityfamily.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AssessmentController: Motor de Diagnóstico Transformacional.
 * Gestiona el flujo de las 1000 preguntas y la persistencia de resultados.
 */
@RestController
@RequestMapping("/api/assessments")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;

    @GetMapping("/questions/stats")
    public ResponseEntity<ApiResponse<List<com.integrityfamily.assessment.dto.QuestionStatDTO>>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(assessmentService.getQuestionStats()));
    }

    /**
     * MOTOR ALEATORIO: Genera un diagnóstico de 20 reactivos (5 por área) filtrados por dimensión temporal.
     * GET /api/assessments/random?familyId=XXX
     */
    @GetMapping("/random")
    public ResponseEntity<ApiResponse<List<Question>>> getRandom(@RequestParam Long familyId) {
        return ResponseEntity.ok(ApiResponse.ok(assessmentService.generateRandomAssessment(familyId)));
    }

    /**
     * NUEVO MÉTODO: Obtiene preguntas del banco de 1000 preguntas.
     * GET http://localhost:8080/api/assessments/questions?dimension=reconocimiento&area=emociones
     */
    @GetMapping("/questions")
    public ResponseEntity<ApiResponse<Object>> getQuestions(
            @RequestParam(required = false) String dimension,
            @RequestParam(required = false) String area
    ) {
        // Llama al service que cargó el JSON de resources
        Object questions = assessmentService.getFilteredQuestions(dimension, area);
        return ResponseEntity.ok(ApiResponse.ok(questions));
    }

    /**
     * REGISTRO DE EVALUACIÓN: Procesa la danza del bienestar.
     */
    @PostMapping("/family/{familyId}")
    public ResponseEntity<ApiResponse<Assessment>> submitAssessment(
            @PathVariable Long familyId,
            @RequestBody Map<String, Integer> responses
    ) {
        Assessment saved = assessmentService.saveAssessment(familyId, responses);
        return ResponseEntity.ok(ApiResponse.ok(saved));
    }

    /**
     * ÚLTIMO DIAGNÓSTICO: Recupera el estado actual para el Dashboard.
     */
    @GetMapping("/family/{familyId}/latest")
    public ResponseEntity<ApiResponse<Assessment>> getLatest(@PathVariable Long familyId) {
        Assessment latest = assessmentService.getLatestByFamily(familyId);
        return ResponseEntity.ok(ApiResponse.ok(latest));
    }

    /**
     * HISTORIAL: Evolución de la familia (Amor/Continuidad).
     */
    @GetMapping("/family/{familyId}/history")
    public ResponseEntity<ApiResponse<List<Assessment>>> getHistory(@PathVariable Long familyId) {
        List<Assessment> history = assessmentService.getHistoryByFamily(familyId);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    /**
     * SÍNTESIS: Resumen espiritual y métricas.
     */
    @GetMapping("/family/{familyId}/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSummary(@PathVariable Long familyId) {
        Map<String, Object> summary = assessmentService.generateSpiritualSummary(familyId);
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }
}