package com.integrityfamily.ai.service;

import com.integrityfamily.domain.repository.ChatMessageRepository;
import com.integrityfamily.domain.repository.ConversationSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Fase C: Detecta el arco emocional de una sesión conversacional
 * analizando las instantáneas emocionales de los últimos mensajes del usuario.
 *
 * Arc estados: STABLE | MILD_TENSION | ESCALATING | ESCALATED | DE_ESCALATING
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmotionalStateTracker {

    private final ChatMessageRepository chatMessageRepository;
    private final ConversationSessionRepository sessionRepository;

    private static final int ARC_WINDOW = 5;

    /**
     * Calcula el arco emocional de la sesión y actualiza ConversationSession.emotionalState.
     * Falla silenciosamente: devuelve "STABLE" si no hay sesión o hay error.
     */
    @Transactional
    public String computeAndUpdateArc(Long sessionId) {
        if (sessionId == null) return "STABLE";
        try {
            List<String> snapshots = chatMessageRepository
                    .findRecentUserSnapshotsForSession(sessionId, PageRequest.of(0, ARC_WINDOW));
            String arc = computeArc(snapshots);

            sessionRepository.findById(sessionId).ifPresent(session -> {
                session.setEmotionalState(arc);
                sessionRepository.save(session);
            });

            log.debug("[EMO_ARC] Sesión {} → arco: {} (n={})", sessionId, arc, snapshots.size());
            return arc;
        } catch (Exception e) {
            log.warn("[EMO_ARC] Error calculando arco para sesión {}: {}", sessionId, e.getMessage());
            return "STABLE";
        }
    }

    // La query devuelve DESC (más reciente primero), index 0 = último mensaje
    private String computeArc(List<String> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) return "STABLE";

        long negativeCount = snapshots.stream().filter(this::isNegative).count();

        if (negativeCount >= 3) return "ESCALATED";

        if (snapshots.size() >= 2) {
            boolean lastNegative = isNegative(snapshots.get(0));
            boolean prevNegative = isNegative(snapshots.get(1));
            if (lastNegative && prevNegative) return "ESCALATING";
            if (!lastNegative && prevNegative) return "DE_ESCALATING";
        }

        if (negativeCount == 1) return "MILD_TENSION";
        return "STABLE";
    }

    private boolean isNegative(String snapshot) {
        return "ANXIOUS".equals(snapshot) || "FRUSTRATED".equals(snapshot) || "CONFUSED".equals(snapshot);
    }
}
