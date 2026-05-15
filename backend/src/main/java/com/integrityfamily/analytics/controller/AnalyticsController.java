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
    private final com.integrityfamily.domain.repository.UserRepository userRepository;

    /**
     * Obtiene el resumen ejecutivo del dashboard familiar.
     * SDD: Optimizado para devolver el cálculo más reciente sin re-procesar si no
     * es necesario.
     */
    @GetMapping("/dashboard/family/{familyId}")
    @org.springframework.security.access.prepost.PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<DashboardSummaryResponse> getFamilySummary(@PathVariable Long familyId) {
        log.info("📊 [ANALYTICS] Solicitando resumen ejecutivo para familia: {}", familyId);

        // El servicio debe decidir internamente si calcula de nuevo o entrega cache
        DashboardSummaryResponse response = analyticsService.calculateLatestResults(familyId);

        return ApiResponse.ok(response);
    }

    /**
     * Endpoint de compatibilidad para resultados rápidos.
     * SDD: Alias para integración con sistemas legados o componentes específicos.
     */
    @GetMapping("/family/{familyId}/latest")
    @org.springframework.security.access.prepost.PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<DashboardSummaryResponse> getLatestResult(@PathVariable Long familyId) {
        return getFamilySummary(familyId); // Reutilización de lógica interna
    }

    /**
     * SDD: Obtiene los datos para el radar de evolución de la familia.
     */
    @GetMapping("/radar")
    public ApiResponse<Object> getRadarData(org.springframework.security.core.Authentication auth) {
        log.info("🎯 [ANALYTICS] Generando datos de radar de evolución dinámicos");
        
        if (auth == null) {
            log.warn("⚠️ [ANALYTICS] Solicitud de radar sin autenticación. Retornando vacío.");
            return ApiResponse.ok(java.util.Collections.emptyList());
        }

        com.integrityfamily.domain.User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (user.getFamily() == null) {
            log.warn("⚠️ [ANALYTICS] Usuario sin familia vinculada: {}", auth.getName());
            return ApiResponse.ok(java.util.Collections.emptyList());
        }

        Long familyId = user.getFamily().getId();
        var radarData = analyticsService.getEvolutionRadarData(familyId);

        return ApiResponse.ok(radarData);
    }

    /**
     * SDD EXTRA: Disparador manual de analítica profunda.
     * Útil cuando el admin quiere forzar una actualización del motor Sentinel.
     */
    @PostMapping("/family/{familyId}/recalculate")
    @org.springframework.security.access.prepost.PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<String> forceRecalculation(@PathVariable Long familyId) {
        log.warn("🔄 [ANALYTICS] Recálculo forzado solicitado para familia: {}", familyId);
        analyticsService.invalidateCacheAndRecalculate(familyId);
        return ApiResponse.ok("Recálculo iniciado exitosamente.");
    }
}


