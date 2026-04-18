package com.integrityfamily.evaluation.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.evaluation.domain.Evaluation;
import com.integrityfamily.evaluation.domain.EvaluationStatus;
import com.integrityfamily.evaluation.dto.EvaluationDtos;
import com.integrityfamily.evaluation.service.EvaluationService;
import com.integrityfamily.family.domain.Family;
import com.integrityfamily.family.domain.Member;
import com.integrityfamily.family.repository.FamilyRepository;
import com.integrityfamily.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * EvaluationController: Motor de Gestión de Diagnósticos.
 * Orquestador del flujo de evaluaciones del Nodo Armenia.
 */
@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final FamilyRepository familyRepository;
    private final MemberRepository memberRepository;

    @GetMapping
    public ApiResponse<List<Evaluation>> getAll() {
        return ApiResponse.ok(evaluationService.findAll());
    }

    @GetMapping("/family/{familyId}")
    public ApiResponse<List<Evaluation>> getByFamily(@PathVariable Long familyId) {
        return ApiResponse.ok(evaluationService.findByFamilyId(familyId));
    }

    /**
     * POST /api/evaluations/start
     * Inicia una nueva danza de diagnóstico para una familia o integrante.
     */
    @PostMapping("/start")
    public ApiResponse<Evaluation> start(@RequestBody EvaluationDtos.EvaluationStartRequest req) {
        Family family = familyRepository.findById(req.familyId())
                .orElseThrow(() -> new RuntimeException("Familia no encontrada"));

        Evaluation evaluation = new Evaluation();
        evaluation.setFamily(family);
        evaluation.setStatus(EvaluationStatus.STARTED);
        evaluation.setStartedAt(LocalDateTime.now());

        if (req.memberId() != null) {
            Member member = memberRepository.findById(req.memberId()).orElse(null);
            evaluation.setMember(member);
        }

        return ApiResponse.ok(evaluationService.create(evaluation));
    }

    /**
     * POST /api/evaluations/{id}/finalize
     * Cierra el ciclo de evaluación y dispara la generación asíncrona de planes.
     */
    @PostMapping("/{id}/finalize")
    public ApiResponse<Long> finalize(
            @PathVariable Long id, 
            @RequestBody EvaluationDtos.EvaluationFinalizeRequest request) {
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
