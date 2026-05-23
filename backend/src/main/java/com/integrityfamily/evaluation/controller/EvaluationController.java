package com.integrityfamily.evaluation.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.AuditEventType;
import com.integrityfamily.auth.service.AuditService;
import com.integrityfamily.dto.EvaluationDtos;
import com.integrityfamily.evaluation.service.EvaluationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * SDD: Controlador de Evaluaciones armonizado.
 * Postura Técnica: Delegación total al Service con telemetría integrada para auditoría inteligente.
 */
@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final AuditService auditService;

    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ApiResponse<List<Evaluation>> getAll() {
        return ApiResponse.ok(evaluationService.findAll());
    }

    @GetMapping("/family/{familyId}")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<List<Evaluation>> getByFamily(@PathVariable Long familyId) {
        return ApiResponse.ok(evaluationService.findByFamilyId(familyId));
    }

    /**
     * SDD: Inicia el ciclo de diagnóstico delegando la construcción al Service y registrando la telemetría.
     */
    @PostMapping("/start")
    @PreAuthorize("@familySecurity.check(#req.familyId)")
    public ApiResponse<Evaluation> start(
            @RequestBody EvaluationDtos.EvaluationStartRequest req,
            Principal principal,
            HttpServletRequest httpServletRequest) {
        Evaluation eval = evaluationService.start(req);
        
        String email = principal != null ? principal.getName() : "ANONYMOUS";
        String metadata = String.format("{\"familyId\":%d,\"memberId\":%s,\"evaluationId\":%d}", 
                req.familyId(), 
                req.memberId() != null ? req.memberId().toString() : "null", 
                eval.getId());
                
        auditService.register(email, AuditEventType.EVALUATION_STARTED, httpServletRequest, metadata);
        
        return ApiResponse.ok(eval);
    }

    @PostMapping("/{id}/finalize")
    @PreAuthorize("@familySecurity.checkEvaluation(#id)")
    public ApiResponse<Long> finalize(
            @PathVariable Long id,
            @Valid @RequestBody EvaluationDtos.EvaluationFinalizeRequest request,
            Principal principal,
            HttpServletRequest httpServletRequest) {
        EvaluationDtos.FinalizeResult result = evaluationService.finalize(id, request);
        Evaluation saved = result.evaluation();

        String email = principal != null ? principal.getName() : "ANONYMOUS";
        String metadata = String.format("{\"evaluationId\":%d,\"icf\":%s,\"hasCrisis\":%b}",
                saved.getId(),
                saved.getIcf() != null ? saved.getIcf().toString() : "null",
                saved.getHasCrisis() != null ? saved.getHasCrisis() : false);

        auditService.register(email, AuditEventType.EVALUATION_SUBMITTED, httpServletRequest, metadata);

        return ApiResponse.ok(saved.getId());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@familySecurity.checkEvaluation(#id)")
    public ApiResponse<Evaluation> getById(@PathVariable Long id) {
        return ApiResponse.ok(evaluationService.findById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@familySecurity.checkEvaluation(#id)")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        evaluationService.delete(id);
        return ApiResponse.ok(null);
    }
}
