package com.integrityfamily.ai.provider;

import com.integrityfamily.ai.dto.AiContext;

/**
 * SDD-AI-04: Interface for AI Inference Engine.
 */
public interface AiProvider {
    /**
     * Generates a response using the LLM.
     * @param userMessage Message from the user.
     * @param context Synthesized family context.
     * @return Markdown response.
     */
    String generateResponse(String userMessage, AiContext context);

    /**
     * Specialized inference for raw, structured prompts (e.g., reports, analytics).
     */
    String generateRawResponse(String rawPrompt);

    /**
     * Unique identifier for the provider implementation.
     */
    String getProviderId();
}


