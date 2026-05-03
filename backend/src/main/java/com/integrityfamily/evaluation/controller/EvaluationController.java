package com.integrityfamily.evaluation.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.dto.EvaluationDtos;
import com.integrityfamily.evaluation.service.EvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SDD: Controlador de Evaluaciones armonizado.
 * Postura Técnica: Delegación total al Service para mantener la pureza de la capa web.
 */
@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    @GetMapping
    public ApiResponse<List<Evaluation>> getAll() {
        return ApiResponse.ok(evaluationService.findAll());
    }

    @GetMapping("/family/{familyId}")
    public ApiResponse<List<Evaluation>> getByFamily(@PathVariable Long familyId) {
        return ApiResponse.ok(evaluationService.findByFamilyId(familyId));
    }

    /**
     * SDD: Inicia el ciclo de diagnóstico delegando la construcción al Service.
     */
    @PostMapping("/start")
    public ApiResponse<Evaluation> start(@RequestBody EvaluationDtos.EvaluationStartRequest req) {
        return ApiResponse.ok(evaluationService.start(req));
    }

    @PostMapping("/{id}/finalize")
    public ApiResponse<Long> finalize(
            @PathVariable Long id,
            @Valid @RequestBody EvaluationDtos.EvaluationFinalizeRequest request) {
        Evaluation saved = evaluationService.finalize(id, request);
        return ApiResponse.ok(saved.getId());
    }

    @GetMapping("/{id}")
    public ApiResponse<Evaluation> getById(@PathVariable Long id) {
        return ApiResponse.ok(evaluationService.findById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        evaluationService.delete(id);
        return ApiResponse.ok(null);
    }
}
