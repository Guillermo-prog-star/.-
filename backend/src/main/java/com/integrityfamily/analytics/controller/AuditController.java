package com.integrityfamily.analytics.controller;

import com.integrityfamily.analytics.service.AnalyticsService;
import com.integrityfamily.analytics.dto.DashboardSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * AuditController: Endpoint de emergencia para inspección de consistencia SDD.
 * Postura Técnica: Punto de entrada para validar el estado de integridad del
 * seed (Familia 1).
 */
@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AnalyticsService analyticsService;

    /**
     * Verifica la consistencia de hitos y analítica para la familia base.
     * SDD FIX: Sincronizado con el nuevo contrato de AnalyticsService.
     */
    @GetMapping("/milestones")
    public List<Object> checkConsistency() {
        try {
            // SDD FIX: Cambiado getFamilySummaryTyped a calculateLatestResults
            DashboardSummaryResponse summary = analyticsService.calculateLatestResults(1L);
            return List.of(summary);
        } catch (Exception e) {
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("error",
                    "Consistencia fallida: No se detectó el seed territorial o la DB no ha inicializado.");
            errorInfo.put("details", e.getMessage());
            errorInfo.put("timestamp", new Date());
            return List.of(errorInfo);
        }
    }
}


