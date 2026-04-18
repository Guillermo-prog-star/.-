package com.integrityfamily.analytics.controller;

import com.integrityfamily.analytics.dto.DashboardSummaryResponse;
import com.integrityfamily.analytics.service.AnalyticsService;
import com.integrityfamily.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /** Resumen global del dashboard */
    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> getDashboardSummary() {
        return ApiResponse.ok(analyticsService.getDashboardSummary());
    }

    /** Resumen por familia — usado en el dashboard cuando hay una familia seleccionada */
    @GetMapping("/family/{familyId}")
    public ApiResponse<DashboardSummaryResponse> getFamilySummary(@PathVariable Long familyId) {
        return ApiResponse.ok(analyticsService.getFamilySummaryTyped(familyId));
    }

    /** Resultado detallado de una evaluación específica */
    @GetMapping("/results/{evaluationId}")
    public ApiResponse<com.integrityfamily.evaluation.dto.EvaluationDtos.EvaluationResultResponse> getEvaluationResult(@PathVariable Long evaluationId) {
        return ApiResponse.ok(analyticsService.getEvaluationResult(evaluationId));
    }
}