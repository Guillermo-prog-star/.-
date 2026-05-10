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
    public ApiResponse<List<Evaluation>> getAll() {
        return ApiResponse.ok(evaluationService.findAll());
    }

    @GetMapping("/family/{familyId}")
    public ApiResponse<List<Evaluation>> getByFamily(@PathVariable Long familyId) {
        return ApiResponse.ok(evaluationService.findByFamilyId(familyId));
    }

    /**
     * SDD: Inicia el ciclo de diagnóstico delegando la construcción al Service y registrando la telemetría.
     */
    @PostMapping("/start")
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
    public ApiResponse<Long> finalize(
            @PathVariable Long id,
            @Valid @RequestBody EvaluationDtos.EvaluationFinalizeRequest request,
            Principal principal,
            HttpServletRequest httpServletRequest) {
        Evaluation saved = evaluationService.finalize(id, request);
        
        String email = principal != null ? principal.getName() : "ANONYMOUS";
        String metadata = String.format("{\"evaluationId\":%d,\"icf\":%s,\"hasCrisis\":%b}", 
                saved.getId(), 
                request.icf() != null ? request.icf().toString() : "null", 
                request.hasCrisis() != null ? request.hasCrisis() : false);
                
        auditService.register(email, AuditEventType.EVALUATION_SUBMITTED, httpServletRequest, metadata);
        
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
