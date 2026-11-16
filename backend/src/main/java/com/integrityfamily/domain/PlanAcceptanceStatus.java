package com.integrityfamily.domain;

/**
 * ADR-010: estado operacional plano de aceptacion de un {@link ImprovementPlan}.
 * Deliberadamente sin REJECTED/REVERTED (a diferencia de AdjustmentStatus) --
 * no existe hoy ningun flujo de "familia rechaza el plan y pide otro".
 */
public enum PlanAcceptanceStatus {
    PROPOSED,
    ACCEPTED
}
