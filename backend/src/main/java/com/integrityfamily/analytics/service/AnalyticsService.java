package com.integrityfamily.analytics.service;

import com.integrityfamily.analytics.dto.DashboardSummaryResponse;

/**
 * SDD: Interfaz Pura de AnalÃƒÂ­tica Proyectiva.
 * Define el contrato para el motor de cÃƒÂ¡lculo de integridad y estados Sentinel.
 */
public interface AnalyticsService {
    
    /**
     * Centraliza el cÃƒÂ¡lculo del estado de integridad familiar.
     */
    DashboardSummaryResponse calculateLatestResults(Long familyId);

    /**
     * Disparador manual para invalidar cachÃƒÂ© y forzar actualizaciÃƒÂ³n del motor.
     */
    void invalidateCacheAndRecalculate(Long familyId);

    /**
     * Obtiene los datos comparativos de evolución en formato de radar (Baseline vs Actual).
     */
    java.util.List<java.util.Map<String, Object>> getEvolutionRadarData(Long familyId);
}


