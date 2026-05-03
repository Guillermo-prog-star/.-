package com.integrityfamily.analytics.controller;

import com.integrityfamily.analytics.dto.DashboardSummaryResponse;
import com.integrityfamily.analytics.service.AnalyticsService;
import com.integrityfamily.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * SDD: Controlador de AnalÃƒÂ­tica Proyectiva.
 * Proporciona estados de integridad, ICF y resultados del motor Sentinel.
 */
@Slf4j
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Obtiene el resumen ejecutivo del dashboard familiar.
     * SDD: Optimizado para devolver el cÃƒÂ¡lculo mÃƒÂ¡s reciente sin re-procesar si no
     * es necesario.
     */
    @GetMapping("/dashboard/family/{familyId}")
    public ApiResponse<DashboardSummaryResponse> getFamilySummary(@PathVariable Long familyId) {
        log.info("Ã°Å¸â€œÅ  [ANALYTICS] Solicitando resumen ejecutivo para familia: {}", familyId);

        // El servicio debe decidir internamente si calcula de nuevo o entrega cache
        DashboardSummaryResponse response = analyticsService.calculateLatestResults(familyId);

        return ApiResponse.ok(response);
    }

    /**
     * Endpoint de compatibilidad para resultados rÃƒÂ¡pidos.
     * SDD: Alias para integraciÃƒÂ³n con sistemas legados o componentes especÃƒÂ­ficos.
     */
    @GetMapping("/family/{familyId}/latest")
    public ApiResponse<DashboardSummaryResponse> getLatestResult(@PathVariable Long familyId) {
        return getFamilySummary(familyId); // ReutilizaciÃƒÂ³n de lÃƒÂ³gica interna
    }

    /**
     * SDD: Obtiene los datos para el radar de evoluciÃƒÂ³n de la familia.
     */
    @GetMapping("/radar")
    public ApiResponse<Object> getRadarData() {
        log.info("Ã°Å¸Å½Â¯ [ANALYTICS] Generando datos de radar de evoluciÃƒÂ³n");
        // Por ahora devolvemos un objeto compatible con lo que espera Chart.js en el frontend
        return ApiResponse.ok(java.util.Map.of(
            "labels", java.util.List.of("Reconocimiento", "Amor", "Compromiso", "AutonomÃƒÂ­a", "Responsabilidad"),
            "datasets", java.util.List.of(
                java.util.Map.of(
                    "label", "Estado Actual",
                    "data", java.util.List.of(80, 70, 90, 65, 75)
                )
            )
        ));
    }

    /**
     * SDD EXTRA: Disparador manual de analÃƒÂ­tica profunda.
     * ÃƒÅ¡til cuando el admin quiere forzar una actualizaciÃƒÂ³n del motor Sentinel.
     */
    @PostMapping("/family/{familyId}/recalculate")
    public ApiResponse<String> forceRecalculation(@PathVariable Long familyId) {
        log.warn("Ã°Å¸â€â€ž [ANALYTICS] RecÃƒÂ¡lculo forzado solicitado para familia: {}", familyId);
        analyticsService.invalidateCacheAndRecalculate(familyId);
        return ApiResponse.ok("RecÃƒÂ¡lculo iniciado exitosamente.");
    }
}


