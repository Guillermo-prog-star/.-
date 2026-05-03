package com.integrityfamily.ai.service;

import com.integrityfamily.ai.dto.AiContext;
import com.integrityfamily.ai.provider.AiProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * SDD-AI-04-SELECTOR: Deterministic Model Selection Strategy.
 * Implements logic to route requests based on family integrity status.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AiProviderSelector {

    private final List<AiProvider> providers;

    /**
     * Selects the best provider based on the family context.
     * Logic:
     * - If Sentinel is Active: Use Claude (High reasoning, better safety).
     * - Else: Use Gemini (Efficiency, Speed).
     */
    public AiProvider selectProvider(AiContext context) {
        if (context.sentinelActive()) {
            log.info("Sentinel Active: Selecting CLAUDE_3_5_SONNET for high-integrity intervention.");
            return findProvider("CLAUDE_3_5_SONNET");
        }

        log.debug("Standard Status: Selecting GEMINI_1_5_FLASH for routine mentoring.");
        return findProvider("GEMINI_1_5_FLASH");
    }

    public AiProvider getReportingProvider() {
        log.info("Selecting high-reasoning provider (CLAUDE) for executive reporting.");
        return findProvider("CLAUDE_3_5_SONNET");
    }

    private AiProvider findProvider(String providerId) {
        return providers.stream()
                .filter(p -> p.getProviderId().equals(providerId))
                .findFirst()
                .orElse(providers.get(0)); // Fallback to first available
    }
}


