package com.integrityfamily.ai.provider;

import com.integrityfamily.ai.dto.AiContext;

/**
 * SDD-AI-04: Interface for AI Inference Engine.
 */
public interface AiProvider {

    String generateResponse(String userMessage, AiContext context);

    String generateRawResponse(String rawPrompt);

    String generateWithFullPrompt(String fullPrompt);

    String getProviderId();

    /** Returns false when the provider has no API key configured or is explicitly disabled. */
    boolean available();

    /**
     * Routing priority. Lower value = preferred for cost-optimized selection.
     * Sentinel/critical mode uses inverse (highest priority = most capable).
     */
    int getPriority();
}


