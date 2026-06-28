package com.integrityfamily.capital.controller;

import com.integrityfamily.capital.dto.IcafDashboardResponse;
import com.integrityfamily.capital.service.CriticalEventLifecycleService;
import com.integrityfamily.capital.service.IcafDashboardService;
import com.integrityfamily.capital.service.IcafQuestionnaireService;
import com.integrityfamily.capital.service.IcafScoringEngine;
import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.domain.FamilyCriticalEvent;
import com.integrityfamily.domain.Question;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Endpoints del Índice de Capital Familiar (ICaF).
 * Base: /api/capital
 */
@Slf4j
@RestController
@RequestMapping("/api/capital")
@RequiredArgsConstructor
public class IcafController {

    private final IcafDashboardService dashboardService;
    private final IcafScoringEngine scoringEngine;
    private final IcafQuestionnaireService questionnaireService;
    private final CriticalEventLifecycleService lifecycleService;

    // ── Dashboard ─────────────────────────────────────────────────────────────

    /**
     * Dashboard ICaF completo: índice, 11 dominios, trayectoria, eventos críticos.
     */
    @GetMapping("/family/{familyId}/dashboard")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<IcafDashboardResponse> getDashboard(@PathVariable Long familyId) {
        log.info("[ICaF] Dashboard solicitado para familia {}", familyId);
        return ApiResponse.ok(dashboardService.getDashboard(familyId));
    }

    /**
     * Fuerza un recálculo inmediato del ICaF.
     * Útil después de completar un cuestionario o resolver un evento.
     */
    @PostMapping("/family/{familyId}/recalculate")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<IcafDashboardResponse> recalculate(
            @PathVariable Long familyId,
            @RequestParam(defaultValue = "MANUAL") String trigger) {
        log.info("[ICaF] Recálculo manual solicitado | familia={} | trigger={}", familyId, trigger);
        scoringEngine.compute(familyId, trigger);
        return ApiResponse.ok(dashboardService.getDashboard(familyId), "ICaF recalculado");
    }

    // ── Cuestionarios ─────────────────────────────────────────────────────────

    /** Devuelve las preguntas de un dominio ICaF (confianza | bienestar_emocional) */
    @GetMapping("/questionnaire/{domain}/questions")
    public ApiResponse<List<Question>> getQuestions(@PathVariable String domain) {
        return ApiResponse.ok(questionnaireService.getQuestions(domain));
    }

    /**
     * Guarda las respuestas del cuestionario de un dominio y retorna
     * el score actualizado del dominio.
     * Body: { "ICAF_CONF_001": 4, "ICAF_CONF_002": 5, ... }
     */
    @PostMapping("/family/{familyId}/questionnaire/{domain}/answers")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<IcafQuestionnaireService.DomainScoreResult> saveAnswers(
            @PathVariable Long familyId,
            @PathVariable String domain,
            @RequestBody Map<String, Integer> answers,
            @RequestParam(required = false) String answeredBy) {
        log.info("[ICaF] Guardando {} respuestas de dominio={} para familia={}", answers.size(), domain, familyId);
        IcafQuestionnaireService.DomainScoreResult result =
                questionnaireService.saveAnswers(familyId, answers, domain, answeredBy);
        // Recalcular ICaF con los nuevos datos
        scoringEngine.compute(familyId, "ASSESSMENT");
        return ApiResponse.ok(result, "Respuestas guardadas y ICaF recalculado");
    }

    // ── Eventos críticos ──────────────────────────────────────────────────────

    /** Eventos críticos activos de una familia */
    @GetMapping("/family/{familyId}/critical-events/active")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<List<FamilyCriticalEvent>> getActiveEvents(@PathVariable Long familyId) {
        return ApiResponse.ok(lifecycleService.getActiveEvents(familyId));
    }

    /** Métricas de resiliencia (tasa de resolución, tiempo promedio, recaídas) */
    @GetMapping("/family/{familyId}/resilience-metrics")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<CriticalEventLifecycleService.ResilienceMetrics> getResilienceMetrics(
            @PathVariable Long familyId) {
        return ApiResponse.ok(lifecycleService.getResilienceMetrics(familyId));
    }

    /** Inicia la intervención de un evento crítico */
    @PatchMapping("/critical-events/{eventId}/start-intervention")
    public ApiResponse<FamilyCriticalEvent> startIntervention(@PathVariable Long eventId) {
        return ApiResponse.ok(lifecycleService.startIntervention(eventId));
    }

    /** Marca un evento como resuelto */
    @PatchMapping("/critical-events/{eventId}/resolve")
    public ApiResponse<FamilyCriticalEvent> resolve(
            @PathVariable Long eventId,
            @RequestParam(required = false) String summary) {
        return ApiResponse.ok(lifecycleService.resolve(eventId, summary));
    }

    /** Registra una recaída */
    @PatchMapping("/critical-events/{eventId}/relapse")
    public ApiResponse<FamilyCriticalEvent> registerRelapse(
            @PathVariable Long eventId,
            @RequestParam(required = false) String notes) {
        return ApiResponse.ok(lifecycleService.registerRelapse(eventId, notes));
    }
}
