package com.integrityfamily.ai.service;

import com.integrityfamily.ai.dto.AiContext;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.ai.provider.TaskType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * SDD-AI-04-SELECTOR: AI Gateway — routes requests across providers with automatic fallback.
 *
 * Routing strategy:
 *  - Sentinel active (family in crisis): prefer highest-capability provider (lowest priority number).
 *  - Normal tasks: prefer cheapest available provider (highest priority number first).
 *  - If preferred provider unavailable, cascade to next in line automatically.
 *  - Throws IllegalStateException only when NO provider is available (all keys missing/disabled).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AiProviderSelector {

    private final List<AiProvider> providers;

    public AiProvider selectProvider(AiContext context) {
        if (context != null && context.sentinelActive()) {
            return selectHighCapacity();
        }
        return selectCostOptimized();
    }

    /**
     * Routes by task type, ignoring Sentinel mode for LIGHTWEIGHT tasks.
     * Use this overload when the task category is known at call site.
     */
    public AiProvider selectProvider(TaskType taskType) {
        return switch (taskType) {
            case LIGHTWEIGHT -> {
                log.debug("[AI-GATEWAY] LIGHTWEIGHT task → cost-optimized provider");
                yield selectCostOptimized();
            }
            case HIGH_CAPACITY -> {
                log.debug("[AI-GATEWAY] HIGH_CAPACITY task → high-capability provider");
                yield selectHighCapacity();
            }
            case STANDARD -> {
                log.debug("[AI-GATEWAY] STANDARD task → cost-optimized provider (no Sentinel context)");
                yield selectCostOptimized();
            }
        };
    }

    /** Reports always use the highest-capability available provider. */
    public AiProvider getReportingProvider() {
        return selectHighCapacity();
    }

    /** Cheapest available: highest priority number first (Gemini → DeepSeek → Claude). */
    private AiProvider selectCostOptimized() {
        return providers.stream()
                .filter(AiProvider::available)
                .max(Comparator.comparingInt(AiProvider::getPriority))
                .orElseGet(() -> {
                    log.warn("[AI-GATEWAY] No paid provider available — falling back to simulation mode.");
                    return firstProvider();
                });
    }

    /** Most capable available: lowest priority number first (Claude → Gemini → DeepSeek). */
    private AiProvider selectHighCapacity() {
        return providers.stream()
                .filter(AiProvider::available)
                .min(Comparator.comparingInt(AiProvider::getPriority))
                .orElseGet(() -> {
                    log.warn("[AI-GATEWAY] No paid provider available for high-capacity request — falling back to simulation mode.");
                    return firstProvider();
                });
    }

    private AiProvider firstProvider() {
        return providers.stream()
                .min(Comparator.comparingInt(AiProvider::getPriority))
                .orElseThrow(() -> new IllegalStateException("No AI providers registered"));
    }
}
