package com.integrityfamily.ai.service;

import com.integrityfamily.domain.ChatMessage;
import com.integrityfamily.domain.Family;
import java.util.Map;

/**
 * SDD CONTRACT: CÃƒÂ³rtex de Inteligencia Artificial Unificado.
 */
public interface AiService {

    // Alias para Controllers y SonicService (compatibilidad sin memberId)
    default ChatMessage chat(String message, Family family) {
        return processInteractiveChat(message, family, null);
    }

    // Alias enriquecido con identidad del miembro activo
    default ChatMessage chat(String message, Family family, Long memberId) {
        return processInteractiveChat(message, family, memberId);
    }

    /**
     * Chat con contexto de transformación inyectado desde el frontend.
     * El contexto enriquece el sistema de IA con el pilar, mes, sprint y misión activa.
     */
    default ChatMessage chatWithTransformation(String message, Family family, Long memberId,
                                               Object transformationContext) {
        return processInteractiveChat(message, family, memberId);
    }

    ChatMessage processInteractiveChat(String message, Family family, Long memberId);

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


