package com.integrityfamily.analytics.service;

import com.integrityfamily.analytics.dto.DashboardSummaryResponse;

/**
 * SDD: Interfaz Pura de Analítica Proyectiva.
 * Define el contrato para el motor de cálculo de integridad y estados Sentinel.
 */
public interface AnalyticsService {
    
    /**
     * Centraliza el cálculo del estado de integridad familiar.
     */
    DashboardSummaryResponse calculateLatestResults(Long familyId);

    /**
     * Disparador manual para invalidar caché y forzar actualización del motor.
     */
    void invalidateCacheAndRecalculate(Long familyId);

    /**
     * Obtiene los datos comparativos de evoluci?n en formato de radar (Baseline vs Actual).
     */
    java.util.List<java.util.Map<String, Object>> getEvolutionRadarData(Long familyId);
}


