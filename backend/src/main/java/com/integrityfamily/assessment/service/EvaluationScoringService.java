package com.integrityfamily.assessment.service;

import com.integrityfamily.domain.RiskLevel;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/**
 * Motor de Scoring: Calcula niveles de riesgo basados en los diagnÃƒÂ³sticos.
 * Migrado al paquete ÃƒÂºnico 'assessment' para evitar conflictos de Beans.
 */
@Service
public class EvaluationScoringService {

    public record ScoringResult(
        RiskLevel riskLevel, 
        BigDecimal emotions, 
        BigDecimal communication, 
        BigDecimal habits, 
        BigDecimal times, 
        BigDecimal global
    ) {}

    public ScoringResult calculateRisk(BigDecimal globalScore) {
        RiskLevel level;
        
        // LÃƒÂ³gica de rangos de William:
        // 70+ = Bajo Riesgo, 40-69 = Medio, <40 = Alto
        if (globalScore.compareTo(new BigDecimal("70")) >= 0) {
            level = RiskLevel.LOW;
        } else if (globalScore.compareTo(new BigDecimal("40")) >= 0) {
            level = RiskLevel.MEDIUM;
        } else {
            level = RiskLevel.HIGH;
        }

        return new ScoringResult(
            level,
            globalScore, // emociones
            globalScore, // comunicacion
            globalScore, // habitos
            globalScore, // tiempos
            globalScore  // global
        );
    }
}


