package com.integrityfamily.ai.service;

import com.integrityfamily.domain.ChatMessage;
import com.integrityfamily.domain.Family;
import java.util.Map;

/**
 * SDD CONTRACT: CÃƒÂ³rtex de Inteligencia Artificial Unificado.
 */
public interface AiService {

    // SDD-FIX: Alias 'chat' para mantener compatibilidad con Controllers y SonicService
    default ChatMessage chat(String message, Family family) {
        return processInteractiveChat(message, family);
    }

    ChatMessage processInteractiveChat(String message, Family family);

    String processAnalyticInference(String prompt, Long familyId);

    String generateDashboardInsight(Family family, Map<String, Double> dimensions, String riskLevel);

    String generateExecutiveSynthesis(Long familyId);

    String generateExecutiveSynthesis(com.integrityfamily.domain.Evaluation evaluation);

    String generateDiagnosticMissions(com.integrityfamily.domain.Evaluation evaluation);

    String generateSynthesis(Map<String, Object> context);

    String generateMissions(Family family);

    String generateEvolutionaryMissions(Family family, Map<String, Double> dimensions, String riskLevel);

    /**
     * SDD SPEC 6.3: Generación de Plan Híbrido con contrato JSON estricto.
     */
    String generateHybridPlan(Family family, Map<String, Double> dimensions, String riskLevel);
    
    String generateHybridPlan(Family family, Map<String, Double> dimensions, String riskLevel, com.integrityfamily.plan.service.ContinuityEngine.ContinuityAnalysis continuityAnalysis);
}


