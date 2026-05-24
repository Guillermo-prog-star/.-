package com.integrityfamily.analytics.controller;

import com.integrityfamily.analytics.dto.DashboardSummaryResponse;
import com.integrityfamily.analytics.service.AnalyticsService;
import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.evaluation.service.EvaluationService;
import com.integrityfamily.domain.Evaluation;
import java.util.HashMap;
import java.util.Map;
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
    private final EvaluationService evaluationService;
    private final com.integrityfamily.analytics.service.FamilyProgressAnalyticsService familyProgressAnalyticsService;

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
     * SDD: Obtiene el último análisis de progreso longitudinal (ΔICF).
     */
    @GetMapping("/family/{familyId}/progress")
    @org.springframework.security.access.prepost.PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<com.integrityfamily.analytics.dto.FamilyProgressResponse> getFamilyProgress(@PathVariable Long familyId) {
        log.info("📊 [ANALYTICS] Solicitando análisis de progreso para familia: {}", familyId);
        
        com.integrityfamily.analytics.dto.FamilyProgressResponse response = familyProgressAnalyticsService.getLatestProgress(familyId)
                .orElseThrow(() -> new RuntimeException("No se encontró análisis de progreso para esta familia."));
                
        return ApiResponse.ok(response);
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
     * SDD Analytics v2: Historial de ICF para gráfico de tendencia.
     * Devuelve todos los puntos (id, icf, riskLevel, fecha) de evaluaciones finalizadas.
     */
    @GetMapping("/family/{familyId}/icf-history")
    @org.springframework.security.access.prepost.PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<java.util.List<java.util.Map<String, Object>>> getIcfHistory(@PathVariable Long familyId) {
        log.info("📈 [ANALYTICS] Solicitando historial ICF para familia: {}", familyId);

        java.util.List<java.util.Map<String, Object>> history =
            evaluationService.findByFamilyId(familyId).stream()
                .filter(e -> com.integrityfamily.domain.EvaluationStatus.FINALIZED == e.getStatus())
                .filter(e -> e.getIcf() != null)
                .map(e -> {
                    java.util.Map<String, Object> pt = new java.util.LinkedHashMap<>();
                    pt.put("evaluationId", e.getId());
                    pt.put("icf",          Math.round(e.getIcf() * 10.0) / 10.0);
                    pt.put("riskLevel",    e.getRiskLevel());
                    pt.put("hasCrisis",    Boolean.TRUE.equals(e.getHasCrisis()));
                    pt.put("finalizedAt",  e.getFinalizedAt() != null ? e.getFinalizedAt().toString() : null);
                    return pt;
                })
                .toList();

        return ApiResponse.ok(history);
    }

    /**
     * SDD Analytics v2: Historial de puntuaciones por dimensión.
     * Devuelve un punto por evaluación finalizada con las 4 dimensiones normalizadas.
     * Usado por el gráfico de evolución multidimensional del dashboard.
     */
    @GetMapping("/family/{familyId}/dimension-history")
    @org.springframework.security.access.prepost.PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<java.util.List<java.util.Map<String, Object>>> getDimensionHistory(
            @PathVariable Long familyId) {
        log.info("📊 [ANALYTICS] Solicitando historial de dimensiones para familia: {}", familyId);

        java.util.List<java.util.Map<String, Object>> history =
            evaluationService.findByFamilyId(familyId).stream()
                .filter(e -> com.integrityfamily.domain.EvaluationStatus.FINALIZED == e.getStatus())
                .filter(e -> e.getDimensionScores() != null && !e.getDimensionScores().isEmpty())
                .map(e -> {
                    java.util.Map<String, Object> pt = new java.util.LinkedHashMap<>();
                    pt.put("evaluationId", e.getId());
                    pt.put("finalizedAt",  e.getFinalizedAt() != null ? e.getFinalizedAt().toString() : null);

                    // Normalize dimension names to canonical Spanish keys
                    java.util.Map<String, Double> dims = new java.util.LinkedHashMap<>();
                    for (com.integrityfamily.domain.EvaluationDimensionScore ds : e.getDimensionScores()) {
                        String key = switch (ds.getDimensionName().toUpperCase()) {
                            case "EMOCIONES", "EMOTIONS"         -> "emociones";
                            case "COMUNICACION", "COMMUNICATION" -> "comunicacion";
                            case "HABITOS", "HABITS"             -> "habitos";
                            case "TIEMPOS", "TIMES"              -> "tiempos";
                            default -> ds.getDimensionName().toLowerCase();
                        };
                        dims.put(key, Math.round(ds.getScore() * 10.0) / 10.0);
                    }
                    pt.put("dimensions", dims);
                    return pt;
                })
                .toList();

        return ApiResponse.ok(history);
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

    /**
     * SDD: Obtiene los resultados de una evaluación específica.
     * Mapea los datos para el frontend, incluyendo el diagnóstico consciente.
     */
    @GetMapping("/results/{id}")
    public ApiResponse<Object> getResultById(@PathVariable Long id) {
        log.info("📊 [ANALYTICS] Solicitando resultados para evaluación ID: {}", id);
        Evaluation eval = evaluationService.findById(id);
        
        Map<String, Object> result = new HashMap<>();
        result.put("id", eval.getId());
        result.put("riskLevel", eval.getRiskLevel());
        result.put("globalScore", eval.getIcf());
        result.put("aiReport", eval.getSpiritualSynthesis());
        result.put("hasCrisis", eval.getHasCrisis());
        
        eval.getDimensionScores().forEach(ds -> {
            String name = ds.getDimensionName().toUpperCase();
            String key = switch (name) {
                case "EMOCIONES", "EMOTIONS" -> "scoreEmotions";
                case "COMUNICACION", "COMMUNICATION" -> "scoreCommunication";
                case "HABITOS", "HABITS" -> "scoreHabits";
                case "TIEMPOS", "TIMES" -> "scoreTimes";
                default -> "score" + name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
            };
            result.put(key, ds.getScore());
        });
        
        return ApiResponse.ok(result);
    }
}


