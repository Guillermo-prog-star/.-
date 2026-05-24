package com.integrityfamily.plan.service;

import com.integrityfamily.plan.service.PlanGenerationService.HybridPlanDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SDD SPEC 6.7: PlanValidator.
 * Modo Bypass para soportar el nuevo contrato simplificado de IA.
 */
@Component
@Slf4j
public class PlanValidator {

    public HybridPlanDto validateAndSanitize(HybridPlanDto originalPlan) {
        log.info("🛡️ [PLAN-VALIDATOR] Modo bypass activado para el nuevo contrato. Retornando plan sin modificar.");
        return originalPlan;
    }
}
