package com.integrityfamily.ai.provider;

import com.integrityfamily.ai.dto.AiContext;

/**
 * SDD-AI-04: Interface for AI Inference Engine.
 */
public interface AiProvider {

    /**
     * Chat conversacional: construye el prompt desde userMessage + context usando PromptGenerator.
     */
    String generateResponse(String userMessage, AiContext context);

    /**
     * Analytics: envía un prompt estructurado ya construido (plan, síntesis, misiones).
     * Añade SYSTEM_IDENTITY mínima como wrapper de seguridad.
     */
    String generateRawResponse(String rawPrompt);

    /**
     * Fase 2 — Chat diferenciado: envía un prompt 100% pre-construido por PromptGenerator,
     * sin ningún wrapper adicional. Usar cuando el prompt ya incluye identidad y reglas.
     */
    String generateWithFullPrompt(String fullPrompt);

    /**
     * Unique identifier for the provider implementation.
     */
    String getProviderId();
}


